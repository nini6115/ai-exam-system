package com.aiexam.ai.service.impl;

import com.aiexam.ai.client.AiChatClient;
import com.aiexam.ai.client.AiGradePrompt;
import com.aiexam.ai.client.AiGradeResult;
import com.aiexam.ai.dto.ManualGradeDTO;
import com.aiexam.ai.mapper.AiGradingMapper;
import com.aiexam.ai.service.AiGradingService;
import com.aiexam.ai.vo.GradeBatchResultVO;
import com.aiexam.ai.vo.GradeDetailVO;
import com.aiexam.ai.vo.GradeFailureVO;
import com.aiexam.ai.vo.PendingGradeItemVO;
import com.aiexam.ai.vo.SheetScoreSummaryVO;
import com.aiexam.ai.vo.SubjectiveSummaryVO;
import com.aiexam.common.context.UserContext;
import com.aiexam.exam.entity.AnswerDetail;
import com.aiexam.exam.entity.AnswerSheet;
import com.aiexam.exam.entity.Exam;
import com.aiexam.exam.mapper.AnswerDetailMapper;
import com.aiexam.exam.mapper.AnswerSheetMapper;
import com.aiexam.exam.mapper.ExamMapper;
import com.aiexam.exam.mapper.ExamUserMapper;
import com.aiexam.question.entity.Question;
import com.aiexam.question.mapper.QuestionMapper;
import com.aiexam.system.entity.SysRole;
import com.aiexam.system.mapper.SysRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * AI 判卷服务实现（批量判卷 / 复核视图 / 人工改分 / 单卷成绩汇总）
 * <p>
 * 无对应实体，不继承 ServiceImpl。全链路不加事务：每题判分是独立单条 UPDATE 自动提交
 * （一题失败不回滚其他题是需求）；汇总是幂等重算（SUM 重算 + GREATEST 刷新 best_score），
 * 中途失败下次触发自愈。
 */
@Slf4j
@Service
public class AiGradingServiceImpl implements AiGradingService {

    /** 管理员角色编码 */
    private static final String ADMIN_ROLE_CODE = "admin";
    /** 教师角色编码 */
    private static final String TEACHER_ROLE_CODE = "teacher";
    /** 答卷状态：1答题中（未交卷不能判分） */
    private static final int STATUS_ANSWERING = 1;
    /** 题型：5简答（主观题） */
    private static final int TYPE_SUBJECTIVE = 5;
    /** 判卷防重锁 key 模板：ai:grading:{examId} */
    private static final String GRADING_LOCK_KEY = "ai:grading:%d";
    /** 锁 TTL：兜底防进程崩溃死锁；锁过期后二次触发的正确性由 markAiGraded 的
     *  graded_by IS NULL 守卫保证（重复触发不会重复计分） */
    private static final Duration GRADING_LOCK_TTL = Duration.ofMinutes(10);
    /** 评语列宽 VARCHAR(500)，超长截断防插入失败 */
    private static final int COMMENT_MAX_LENGTH = 500;

    @Autowired
    private AiGradingMapper aiGradingMapper;

    @Autowired
    private AiChatClient aiChatClient;

    @Autowired
    private AnswerDetailMapper answerDetailMapper;

    @Autowired
    private AnswerSheetMapper answerSheetMapper;

    @Autowired
    private ExamUserMapper examUserMapper;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // ==================== 批量 AI 判卷 ====================

    @Override
    public GradeBatchResultVO gradeExam(Long examId) {
        checkTeacherOrAdmin();
        requireExam(examId);

        List<PendingGradeItemVO> pending = aiGradingMapper.selectPendingGrades(examId);
        GradeBatchResultVO vo = new GradeBatchResultVO(pending.size(), 0, 0, new ArrayList<>());
        if (pending.isEmpty()) {
            log.info("考试[{}]无待判主观题", examId);
            return vo;
        }

        // 防重锁：同一考试同时只允许一个判卷任务；拿锁失败直接报错（不进入 try，不释放他人锁）
        String lockKey = String.format(GRADING_LOCK_KEY, examId);
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, String.valueOf(UserContext.getUserId()), GRADING_LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            throw new RuntimeException("该考试正在判卷中，请稍后再试");
        }

        try {
            // 涉及答卷去重（保序），判完后逐张尝试汇总
            Set<Long> sheetIds = new LinkedHashSet<>();
            for (PendingGradeItemVO item : pending) {
                try {
                    gradeOne(item, vo);
                    sheetIds.add(item.getSheetId());
                } catch (Exception e) {
                    vo.setFailedCount(vo.getFailedCount() + 1);
                    vo.getFailures().add(new GradeFailureVO(item.getSheetId(),
                            item.getQuestionId(), e.getMessage()));
                    // 单题失败不阻断批次，明细保持待判可重试
                    log.warn("AI 判分失败，sheetId[{}] questionId[{}]：{}",
                            item.getSheetId(), item.getQuestionId(), e.getMessage());
                }
            }
            for (Long sheetId : sheetIds) {
                summarizeIfComplete(sheetId);
            }
        } finally {
            redisTemplate.delete(lockKey);
        }

        log.info("用户[{}]触发考试[{}]AI判卷：待判{}，成功{}，失败{}",
                UserContext.getUsername(), examId, vo.getPendingCount(), vo.getSuccessCount(), vo.getFailedCount());
        return vo;
    }

    /**
     * 判分单题：调大模型 → 校验分数 → 条件落库（graded_by IS NULL 守卫）
     */
    private void gradeOne(PendingGradeItemVO item, GradeBatchResultVO vo) {
        AiGradeResult result = aiChatClient.grade(AiGradePrompt.builder()
                .title(item.getTitle())
                .correctAnswer(item.getCorrectAnswer())
                .analysis(item.getAnalysis())
                .fullScore(item.getFullScore())
                .userAnswer(item.getUserAnswer())
                .build());
        BigDecimal score = requireValidScore(result.getScore(), item.getFullScore());
        int isCorrect = score.compareTo(item.getFullScore()) >= 0 ? 1 : 0;
        int affected = aiGradingMapper.markAiGraded(item.getDetailId(), score, isCorrect,
                truncate(result.getComment(), COMMENT_MAX_LENGTH), LocalDateTime.now());
        if (affected == 0) {
            // 已被并发判分/人工改分处理，不重复计分
            log.info("明细[{}]已被其他判分处理，跳过", item.getDetailId());
        }
        vo.setSuccessCount(vo.getSuccessCount() + 1);
    }

    /**
     * 校验大模型返回的分数：非空、不超满分、不为负；统一 2 位小数
     */
    private BigDecimal requireValidScore(BigDecimal score, BigDecimal fullScore) {
        if (score == null) {
            throw new RuntimeException("大模型未返回分数");
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("大模型返回负分数");
        }
        if (score.compareTo(fullScore) > 0) {
            throw new RuntimeException("大模型返回分数超过本题满分");
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    // ==================== 成绩汇总 ====================

    /**
     * 答卷主观题全部判完时汇总成绩：subjective=SUM、total=客观+主观、刷新 best_score。
     *
     * @return 汇总结果；返回 null 表示该卷仍有待判主观题（不汇总）或答卷不存在
     */
    private SheetScoreSummaryVO summarizeIfComplete(Long sheetId) {
        SubjectiveSummaryVO progress = aiGradingMapper.selectSubjectiveSummary(sheetId);
        if (progress == null || progress.getUngradedCount() != null && progress.getUngradedCount() > 0) {
            return null;
        }
        AnswerSheet sheet = answerSheetMapper.selectById(sheetId);
        if (sheet == null) {
            return null;
        }
        BigDecimal objective = sheet.getObjectiveScore() == null ? BigDecimal.ZERO : sheet.getObjectiveScore();
        BigDecimal subjective = progress.getSubjectiveScore() == null
                ? BigDecimal.ZERO : progress.getSubjectiveScore();
        BigDecimal total = objective.add(subjective).setScale(2, RoundingMode.HALF_UP);

        // updateById 忽略 null，只更新主观分与总分两列
        AnswerSheet update = new AnswerSheet();
        update.setId(sheetId);
        update.setSubjectiveScore(subjective);
        update.setTotalScore(total);
        answerSheetMapper.updateById(update);
        // 刷新最高分（复用 GREATEST 原子比较）；is_passed 由分析模块 fillIsPassed 惰性重算
        examUserMapper.updateBestScore(sheet.getExamId(), sheet.getUserId(), total);
        log.info("答卷[{}]主观题判分完成，主观分[{}]，总分[{}]（客观分[{}]），已刷新 best_score",
                sheetId, subjective, total, objective);
        return new SheetScoreSummaryVO(sheetId, objective, subjective, total);
    }

    // ==================== 复核视图 ====================

    @Override
    public List<GradeDetailVO> getSheetGradeDetails(Long examId, Long sheetId) {
        checkTeacherOrAdmin();
        requireExam(examId);
        // 越权 sheetId（不属于本场考试）由 SQL 限定条件过滤为空列表
        return aiGradingMapper.selectSheetGradeDetails(examId, sheetId);
    }

    // ==================== 人工改分 ====================

    @Override
    public SheetScoreSummaryVO manualGrade(Long detailId, ManualGradeDTO dto) {
        checkTeacherOrAdmin();

        AnswerDetail detail = answerDetailMapper.selectById(detailId);
        if (detail == null) {
            throw new RuntimeException("答题明细不存在");
        }
        AnswerSheet sheet = answerSheetMapper.selectById(detail.getSheetId());
        if (sheet == null) {
            throw new RuntimeException("答卷不存在");
        }
        if (sheet.getStatus() == STATUS_ANSWERING) {
            throw new RuntimeException("答卷未交卷，不能判分");
        }
        Question question = questionMapper.selectById(detail.getQuestionId());
        if (question == null || question.getType() == null || question.getType() != TYPE_SUBJECTIVE) {
            throw new RuntimeException("仅主观题支持人工改分");
        }

        BigDecimal score = dto.getScore();
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("分数不能为负");
        }
        if (score.compareTo(detail.getFullScore()) > 0) {
            throw new RuntimeException("分数不能超过本题满分" + detail.getFullScore().toPlainString());
        }
        score = score.setScale(2, RoundingMode.HALF_UP);
        int isCorrect = score.compareTo(detail.getFullScore()) >= 0 ? 1 : 0;

        aiGradingMapper.markManualGraded(detailId, score, isCorrect,
                UserContext.getUserId(), truncate(dto.getComment(), COMMENT_MAX_LENGTH), LocalDateTime.now());
        log.info("教师[{}]人工改分明细[{}]，分数[{}]", UserContext.getUsername(), detailId, score);

        // 改分后尝试汇总；整卷仍有待判主观题时返回兜底 VO（subjectiveScore=null 表示成绩未定稿）
        SheetScoreSummaryVO summary = summarizeIfComplete(sheet.getId());
        if (summary != null) {
            return summary;
        }
        return new SheetScoreSummaryVO(sheet.getId(), sheet.getObjectiveScore(), null, sheet.getTotalScore());
    }

    // ==================== 公共方法 ====================

    private Exam requireExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }
        return exam;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 管理权限校验（粗粒度：当前登录用户须持有 admin 或 teacher 角色）
     */
    private void checkTeacherOrAdmin() {
        Long userId = UserContext.getUserId();
        boolean allowed = userId != null && sysRoleMapper.selectByUserId(userId).stream()
                .anyMatch(r -> ADMIN_ROLE_CODE.equals(r.getRoleCode())
                        || TEACHER_ROLE_CODE.equals(r.getRoleCode()));
        if (!allowed) {
            throw new RuntimeException("无权限操作");
        }
    }
}
