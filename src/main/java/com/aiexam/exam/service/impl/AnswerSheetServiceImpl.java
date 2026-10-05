package com.aiexam.exam.service.impl;

import com.aiexam.common.context.UserContext;
import com.aiexam.exam.dto.AnswerSaveDTO;
import com.aiexam.exam.entity.AnswerDetail;
import com.aiexam.exam.entity.AnswerSheet;
import com.aiexam.exam.entity.Exam;
import com.aiexam.exam.entity.ExamUser;
import com.aiexam.exam.mapper.AnswerDetailMapper;
import com.aiexam.exam.mapper.AnswerSheetMapper;
import com.aiexam.exam.mapper.ExamMapper;
import com.aiexam.exam.mapper.ExamUserMapper;
import com.aiexam.exam.service.AnswerSheetService;
import com.aiexam.exam.vo.ExamStartVO;
import com.aiexam.paper.mapper.ExamPaperQuestionMapper;
import com.aiexam.paper.vo.PaperQuestionVO;
import com.aiexam.question.entity.Question;
import com.aiexam.question.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学生答题服务实现（进入考试 / 草稿保存）
 */
@Slf4j
@Service
public class AnswerSheetServiceImpl implements AnswerSheetService {

    /** 答卷状态：答题中 / 已交卷 */
    private static final int SHEET_STATUS_ANSWERING = 1;
    private static final int SHEET_STATUS_SUBMITTED = 2;
    /** 判分方式：1系统 */
    private static final int GRADE_BY_SYSTEM = 1;
    /** 交卷方式：1手动 */
    private static final int SUBMIT_TYPE_MANUAL = 1;
    /** 题型：2多选 5简答（其余 1单选/3判断/4填空均为客观题，走同一精确比对） */
    private static final int TYPE_MULTI = 2;
    private static final int TYPE_SUBJECTIVE = 5;
    /** 试卷题目缓存 key 前缀（试卷发布后题目不可改，缓存 24h） */
    private static final String PAPER_QUESTIONS_KEY = "exam:paper:questions:";
    /** 草稿答案 key 模板：exam:answer:{examId}:{userId}，Hash 结构 */
    private static final String ANSWER_KEY_TEMPLATE = "exam:answer:%d:%d";

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private ExamUserMapper examUserMapper;

    @Autowired
    private AnswerSheetMapper answerSheetMapper;

    @Autowired
    private ExamPaperQuestionMapper examPaperQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /** PaperQuestionVO 无日期字段，无需注册 jsr310 模块 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 进入考试 ====================

    @Override
    public ExamStartVO start(Long examId, String ip, String userAgent) {
        Long userId = UserContext.getUserId();
        Exam exam = requireExam(examId);

        // 时间窗口校验
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            throw new RuntimeException("考试尚未开始");
        }
        if (now.isAfter(exam.getEndTime())) {
            throw new RuntimeException("考试已结束");
        }
        // 迟到限制：>0 时才生效（0 = 不限迟到，只受截止时间约束）
        if (exam.getAllowLateMinutes() != null && exam.getAllowLateMinutes() > 0
                && now.isAfter(exam.getStartTime().plusMinutes(exam.getAllowLateMinutes()))) {
            throw new RuntimeException("迟到超过允许时间，无法进入考试");
        }

        // 名单校验
        Long count = examUserMapper.selectCount(new LambdaQueryWrapper<ExamUser>()
                .eq(ExamUser::getExamId, examId).eq(ExamUser::getUserId, userId));
        if (count == 0) {
            throw new RuntimeException("您不在本场考试名单中");
        }

        // 已有答卷：交过卷则拒绝；答题中则复用（刷新页面重进，同 seed 保证题目顺序一致）
        AnswerSheet sheet = latestSheet(examId, userId);
        if (sheet != null && sheet.getStatus() != SHEET_STATUS_ANSWERING) {
            throw new RuntimeException("您已交卷，无法再次进入考试");
        }
        if (sheet == null) {
            sheet = createSheet(exam, userId, ip, userAgent, now);
        }

        // 取题目并按种子乱序（确定性：同 seed 同序）
        List<PaperQuestionVO> questions = getPaperQuestions(exam.getPaperId());
        if (Integer.valueOf(1).equals(exam.getRandomOrder()) && questions.size() > 1) {
            Collections.shuffle(questions, new Random(sheet.getQuestionOrderSeed()));
        }

        ExamStartVO vo = new ExamStartVO();
        vo.setExamId(exam.getId());
        vo.setExamName(exam.getName());
        vo.setSheetId(sheet.getId());
        vo.setStartTime(sheet.getStartTime());
        vo.setEndTime(sheet.getEndTime());
        vo.setDuration(exam.getDuration());
        vo.setRandomOrder(exam.getRandomOrder());
        vo.setRandomOptions(exam.getRandomOptions());
        vo.setQuestions(questions);
        return vo;
    }

    /**
     * 创建答卷：开始时间=now，应交卷时间=min(now+时长, 考试截止时间)
     */
    private AnswerSheet createSheet(Exam exam, Long userId, String ip, String userAgent, LocalDateTime now) {
        LocalDateTime deadline = now.plusMinutes(exam.getDuration());
        if (deadline.isAfter(exam.getEndTime())) {
            deadline = exam.getEndTime();
        }
        AnswerSheet sheet = new AnswerSheet();
        sheet.setExamId(exam.getId());
        sheet.setPaperId(exam.getPaperId());
        sheet.setUserId(userId);
        sheet.setAttemptNo(1);
        sheet.setStatus(SHEET_STATUS_ANSWERING);
        sheet.setStartTime(now);
        sheet.setEndTime(deadline);
        sheet.setScreenSwitchCount(0);
        // 列宽 VARCHAR(50)/VARCHAR(500)，超长截断防插入失败
        sheet.setIpAddress(truncate(ip, 50));
        sheet.setUserAgent(truncate(userAgent, 500));
        sheet.setQuestionOrderSeed(ThreadLocalRandom.current().nextInt());
        answerSheetMapper.insert(sheet);
        // ponytail: check-then-insert 有并发窗口，双击极端下会产生两条答题中记录；需严格幂等时再加锁或唯一索引
        log.info("用户[{}]进入考试[{}]，答卷ID：{}，seed：{}",
                userId, exam.getId(), sheet.getId(), sheet.getQuestionOrderSeed());
        return sheet;
    }

    // ==================== 保存草稿 ====================

    @Override
    public void saveAnswer(Long examId, AnswerSaveDTO dto) {
        Long userId = UserContext.getUserId();
        Exam exam = requireExam(examId);

        AnswerSheet sheet = latestSheet(examId, userId);
        if (sheet == null) {
            throw new RuntimeException("请先进入考试");
        }
        if (sheet.getStatus() != SHEET_STATUS_ANSWERING) {
            throw new RuntimeException("考试已交卷，无法保存答案");
        }
        if (LocalDateTime.now().isAfter(sheet.getEndTime())) {
            throw new RuntimeException("考试时间已到，无法保存答案");
        }

        // 题目必须属于本场考试的试卷
        boolean inPaper = getPaperQuestions(exam.getPaperId()).stream()
                .anyMatch(q -> q.getQuestionId().equals(dto.getQuestionId()));
        if (!inPaper) {
            throw new RuntimeException("题目不属于本场考试试卷");
        }

        // Hash 结构：hashKey=题目ID，hashValue=答案（空串表示清空该题）
        String key = String.format(ANSWER_KEY_TEMPLATE, examId, userId);
        redisTemplate.opsForHash().put(key, String.valueOf(dto.getQuestionId()),
                dto.getAnswer() == null ? "" : dto.getAnswer());
        // TTL 刷到应交卷时间（兜底防止交卷失败后草稿残留）
        redisTemplate.expire(key, Duration.between(LocalDateTime.now(), sheet.getEndTime()));
    }

    // ==================== 交卷 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long examId) {
        Long userId = UserContext.getUserId();
        Exam exam = requireExam(examId);

        // 按本人查答卷，天然保证只能交自己的卷
        AnswerSheet sheet = latestSheet(examId, userId);
        if (sheet == null) {
            throw new RuntimeException("请先进入考试");
        }
        if (sheet.getStatus() != SHEET_STATUS_ANSWERING) {
            throw new RuntimeException("您已交卷，无需重复交卷");
        }

        // 读草稿：hashKey=题目ID字符串
        String draftKey = String.format(ANSWER_KEY_TEMPLATE, examId, userId);
        Map<Object, Object> drafts = redisTemplate.opsForHash().entries(draftKey);

        // 题目（含分值/题型）+ 标准答案
        List<PaperQuestionVO> questions = getPaperQuestions(exam.getPaperId());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(
                        questions.stream().map(PaperQuestionVO::getQuestionId).toList()).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        // 还原学生看到的题目顺序（与 start 同 seed 同序），否则按试卷原序
        if (Integer.valueOf(1).equals(exam.getRandomOrder()) && questions.size() > 1) {
            Collections.shuffle(questions, new Random(sheet.getQuestionOrderSeed()));
        }

        // 逐题落库 + 客观题判分
        LocalDateTime now = LocalDateTime.now();
        List<AnswerDetail> details = new ArrayList<>();
        BigDecimal objectiveScore = BigDecimal.ZERO;
        for (int i = 0; i < questions.size(); i++) {
            PaperQuestionVO pq = questions.get(i);
            Question question = questionMap.get(pq.getQuestionId());
            String userAnswer = (String) drafts.get(String.valueOf(pq.getQuestionId()));

            AnswerDetail detail = new AnswerDetail();
            detail.setSheetId(sheet.getId());
            detail.setQuestionId(pq.getQuestionId());
            detail.setUserAnswer(userAnswer);
            detail.setCorrectAnswer(question != null ? question.getAnswer() : null);
            detail.setFullScore(pq.getQuestionScore());
            detail.setSortOrder(i + 1);
            detail.setAnswerTime(now);

            if (question != null && isObjective(question.getType())) {
                boolean correct = gradeObjective(question.getType(), userAnswer, question.getAnswer());
                detail.setScore(correct ? pq.getQuestionScore() : BigDecimal.ZERO);
                detail.setIsCorrect(correct ? 1 : 0);
                detail.setGradedBy(GRADE_BY_SYSTEM);
                detail.setGradeTime(now);
                if (correct) {
                    objectiveScore = objectiveScore.add(pq.getQuestionScore());
                }
            }
            // 主观题（简答）：score/is_correct 留空，等 AI 判卷模块
            details.add(detail);
        }
        Db.saveBatch(details);

        // 更新答卷：已交卷 + 得分（total 暂等于客观分，主观判完再补）
        sheet.setStatus(SHEET_STATUS_SUBMITTED);
        sheet.setSubmitTime(now);
        sheet.setSubmitType(SUBMIT_TYPE_MANUAL);
        sheet.setObjectiveScore(objectiveScore);
        sheet.setTotalScore(objectiveScore);
        answerSheetMapper.updateById(sheet);

        // 清草稿（放最后：事务若在前面回滚，草稿还在 Redis 不丢）
        redisTemplate.delete(draftKey);
        log.info("用户[{}]交卷，考试[{}]，答卷ID：{}，客观题得分：{}",
                userId, examId, sheet.getId(), objectiveScore);
    }

    /**
     * 是否客观题（单选/多选/判断/填空），简答为主观题
     */
    private boolean isObjective(Integer type) {
        return type != null && type != TYPE_SUBJECTIVE;
    }

    /**
     * 客观题判分：单选/判断/填空精确比对；多选排序后比对
     */
    private boolean gradeObjective(Integer type, String userAnswer, String correctAnswer) {
        if (userAnswer == null || correctAnswer == null) {
            return false;
        }
        String user = userAnswer.trim();
        String correct = correctAnswer.trim();
        if (type == TYPE_MULTI) {
            user = user.chars().sorted()
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
            correct = correct.chars().sorted()
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        }
        return user.equals(correct);
    }

    // ==================== 公共方法 ====================

    private Exam requireExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }
        return exam;
    }

    /**
     * 查某考生在本场考试的最新一张答卷
     */
    private AnswerSheet latestSheet(Long examId, Long userId) {
        return answerSheetMapper.selectOne(new LambdaQueryWrapper<AnswerSheet>()
                .eq(AnswerSheet::getExamId, examId).eq(AnswerSheet::getUserId, userId)
                .orderByDesc(AnswerSheet::getId).last("LIMIT 1"));
    }

    /**
     * 取试卷题目：Redis 缓存 → 数据库回源并缓存（24h）
     */
    private List<PaperQuestionVO> getPaperQuestions(Long paperId) {
        String key = PAPER_QUESTIONS_KEY + paperId;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<List<PaperQuestionVO>>() {});
            }
            List<PaperQuestionVO> questions = examPaperQuestionMapper.selectPaperQuestions(paperId);
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(questions), Duration.ofHours(24));
            return questions;
        } catch (Exception e) {
            // Redis/JSON 异常不阻断考试，降级直查数据库
            log.warn("读取试卷题目缓存失败，降级查库，paperId：{}", paperId, e);
            return examPaperQuestionMapper.selectPaperQuestions(paperId);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
