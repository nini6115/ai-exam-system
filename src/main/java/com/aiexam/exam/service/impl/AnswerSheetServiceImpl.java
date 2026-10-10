package com.aiexam.exam.service.impl;

import com.aiexam.common.context.UserContext;
import com.aiexam.exam.dto.AnswerSaveDTO;
import com.aiexam.exam.dto.CheatReportDTO;
import com.aiexam.exam.entity.AnswerDetail;
import com.aiexam.exam.entity.AnswerSheet;
import com.aiexam.exam.entity.CheatRecord;
import com.aiexam.exam.entity.Exam;
import com.aiexam.exam.entity.ExamUser;
import com.aiexam.exam.mapper.AnswerSheetMapper;
import com.aiexam.exam.mapper.CheatRecordMapper;
import com.aiexam.exam.mapper.ExamMapper;
import com.aiexam.exam.mapper.ExamUserMapper;
import com.aiexam.exam.service.AnswerSheetService;
import com.aiexam.exam.vo.CheatReportVO;
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
 * 学生答题服务实现（进入考试 / 草稿保存 / 交卷 / 防作弊上报）
 * <p>
 * 手动 / 切屏强制 / 超时自动三种收卷入口收敛到私有方法 doFinish，
 * 以条件 UPDATE（status=1 守卫）+ 行锁保证判分落库恰好一次。
 */
@Slf4j
@Service
public class AnswerSheetServiceImpl implements AnswerSheetService {

    /** 答卷状态：1答题中 2已交卷 3强制交卷 4超时自动交卷 */
    private static final int SHEET_STATUS_ANSWERING = 1;
    private static final int SHEET_STATUS_SUBMITTED = 2;
    private static final int SHEET_STATUS_FORCE = 3;
    private static final int SHEET_STATUS_TIMEOUT = 4;
    /** 交卷方式：1手动 2超时 3切屏超限 */
    private static final int SUBMIT_TYPE_MANUAL = 1;
    private static final int SUBMIT_TYPE_TIMEOUT = 2;
    private static final int SUBMIT_TYPE_FORCE = 3;
    /** 防作弊上报类型：1切屏 2离开超时 */
    private static final int CHEAT_TYPE_SCREEN_SWITCH = 1;
    private static final int CHEAT_TYPE_AWAY = 2;
    /** 上报后服务端动作：0仅记录 1前端警告（超限但配置为仅警告） 2已强制交卷 */
    private static final int ACTION_NONE = 0;
    private static final int ACTION_WARN = 1;
    private static final int ACTION_FORCE = 2;
    /** 判分方式：1系统 */
    private static final int GRADE_BY_SYSTEM = 1;
    /** 题型：2多选 5简答（其余 1单选/3判断/4填空均为客观题，走同一精确比对） */
    private static final int TYPE_MULTI = 2;
    private static final int TYPE_SUBJECTIVE = 5;
    /** 次数上限兜底值（与 exam.max_attempts 列默认一致） */
    private static final int DEFAULT_MAX_ATTEMPTS = 1;
    /** 每轮自动收卷最大扫描条数 */
    private static final int AUTO_SUBMIT_BATCH = 100;
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
    private CheatRecordMapper cheatRecordMapper;

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
    @Transactional(rollbackFor = Exception.class)
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

        // 已有答题中答卷则复用（刷新页面重进，同 seed 保证题目顺序一致，不消耗次数）
        AnswerSheet sheet = latestSheet(examId, userId);
        if (sheet == null || sheet.getStatus() != SHEET_STATUS_ANSWERING) {
            // 新建答卷前条件自增已考次数（attempts < max 守卫）：
            // 失败即次数用尽；双击/多端并发也只有一个入口能自增成功，封死重复建卷窗口
            int maxAttempts = exam.getMaxAttempts() == null ? DEFAULT_MAX_ATTEMPTS : exam.getMaxAttempts();
            if (examUserMapper.increaseAttempts(examId, userId, maxAttempts) == 0) {
                throw new RuntimeException("考试次数已用完，无法再次进入考试");
            }
            ExamUser examUser = examUserMapper.selectOne(new LambdaQueryWrapper<ExamUser>()
                    .eq(ExamUser::getExamId, examId).eq(ExamUser::getUserId, userId));
            sheet = createSheet(exam, userId, ip, userAgent, now, examUser.getAttempts());
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
     * <p>
     * 调用前已完成条件自增次数的并发守卫，双击/多端不会产生两条答题中记录。
     */
    private AnswerSheet createSheet(Exam exam, Long userId, String ip, String userAgent,
                                    LocalDateTime now, int attemptNo) {
        LocalDateTime deadline = now.plusMinutes(exam.getDuration());
        if (deadline.isAfter(exam.getEndTime())) {
            deadline = exam.getEndTime();
        }
        AnswerSheet sheet = new AnswerSheet();
        sheet.setExamId(exam.getId());
        sheet.setPaperId(exam.getPaperId());
        sheet.setUserId(userId);
        sheet.setAttemptNo(attemptNo);
        sheet.setStatus(SHEET_STATUS_ANSWERING);
        sheet.setStartTime(now);
        sheet.setEndTime(deadline);
        sheet.setScreenSwitchCount(0);
        // 列宽 VARCHAR(50)/VARCHAR(500)，超长截断防插入失败
        sheet.setIpAddress(truncate(ip, 50));
        sheet.setUserAgent(truncate(userAgent, 500));
        sheet.setQuestionOrderSeed(ThreadLocalRandom.current().nextInt());
        answerSheetMapper.insert(sheet);
        log.info("用户[{}]进入考试[{}]，答卷ID：{}，第{}次，seed：{}",
                userId, exam.getId(), sheet.getId(), attemptNo, sheet.getQuestionOrderSeed());
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

    // ==================== 交卷（三个入口） ====================

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

        // 超过应交卷时间仍允许交（保住学生答案），但按超时语义标记，不必等定时任务下一轮
        boolean overtime = LocalDateTime.now().isAfter(sheet.getEndTime());
        doFinish(sheet, exam,
                overtime ? SHEET_STATUS_TIMEOUT : SHEET_STATUS_SUBMITTED,
                overtime ? SUBMIT_TYPE_TIMEOUT : SUBMIT_TYPE_MANUAL);
    }

    @Override
    public int autoSubmitTimeoutSheets() {
        // 单表条件查询，MP 足够（规范：复杂联表才进 XML）
        List<Long> ids = answerSheetMapper.selectList(new LambdaQueryWrapper<AnswerSheet>()
                        .select(AnswerSheet::getId)
                        .eq(AnswerSheet::getStatus, SHEET_STATUS_ANSWERING)
                        .lt(AnswerSheet::getEndTime, LocalDateTime.now())
                        .last("LIMIT " + AUTO_SUBMIT_BATCH))
                .stream().map(AnswerSheet::getId).toList();
        int done = 0;
        for (Long id : ids) {
            try {
                // 走接口方法 autoSubmit 经代理开启独立事务，一张失败不阻断批次
                if (autoSubmit(id)) {
                    done++;
                }
            } catch (Exception e) {
                log.error("答卷[{}]超时自动交卷失败，跳过继续", id, e);
            }
        }
        return done;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean autoSubmit(Long sheetId) {
        AnswerSheet sheet = answerSheetMapper.selectById(sheetId);
        if (sheet == null || sheet.getStatus() != SHEET_STATUS_ANSWERING) {
            // 已被其他入口收卷，幂等跳过
            return false;
        }
        Exam exam = examMapper.selectById(sheet.getExamId());
        if (exam == null) {
            log.error("答卷[{}]对应考试[{}]不存在，无法自动交卷", sheetId, sheet.getExamId());
            return false;
        }
        return doFinish(sheet, exam, SHEET_STATUS_TIMEOUT, SUBMIT_TYPE_TIMEOUT);
    }

    /**
     * 共享交卷核心：手动 / 切屏超限强制 / 超时自动 三入口复用。
     * 必须在调用方的事务内执行（抢占 UPDATE 的行锁持有到提交，是三方并发互斥的唯一依据）。
     *
     * @param sheet       答卷（内存中 status 仍为答题中）
     * @param exam        所属考试
     * @param finalStatus 终态：2已交卷 3强制交卷 4超时自动交卷
     * @param submitType  交卷方式：1手动 2超时 3切屏超限
     * @return false=已被其他入口收卷（幂等跳过），true=本次完成收卷
     */
    private boolean doFinish(AnswerSheet sheet, Exam exam, int finalStatus, int submitType) {
        LocalDateTime now = LocalDateTime.now();
        // 1. 条件抢占：status=1 → 终态。抢不到说明手动/强制/自动已有一方完成，直接返回
        if (answerSheetMapper.markSubmitted(sheet.getId(), finalStatus, submitType, now) == 0) {
            log.info("答卷[{}]已被其他入口收卷，本次(状态{}/方式{})跳过", sheet.getId(), finalStatus, submitType);
            return false;
        }

        // 2. 读草稿并逐题落库判分
        String draftKey = String.format(ANSWER_KEY_TEMPLATE, sheet.getExamId(), sheet.getUserId());
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
        // uk_sheet_question 唯一索引兜底防重复明细
        Db.saveBatch(details);

        // 3. 只补得分字段（status/submit_time/submit_type 已在抢占时写入；updateById 忽略 null）
        AnswerSheet scoreUpdate = new AnswerSheet();
        scoreUpdate.setId(sheet.getId());
        scoreUpdate.setObjectiveScore(objectiveScore);
        scoreUpdate.setTotalScore(objectiveScore);   // 主观分待 AI 判卷后补
        answerSheetMapper.updateById(scoreUpdate);

        // 4. 刷新最高分（当前 total=客观分；AI 判卷模块更新终分时需再刷一次）
        examUserMapper.updateBestScore(exam.getId(), sheet.getUserId(), objectiveScore);

        // 5. 清草稿（放最后：事务若在前面回滚，草稿还在 Redis 不丢）
        redisTemplate.delete(draftKey);
        // 日志不取 UserContext（Quartz 线程无登录态），用答卷上的考生ID
        log.info("答卷[{}]收卷完成，考试[{}]，考生[{}]，状态[{}]，方式[{}]，客观题得分：{}",
                sheet.getId(), exam.getId(), sheet.getUserId(), finalStatus, submitType, objectiveScore);
        return true;
    }

    // ==================== 防作弊上报 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheatReportVO reportCheat(Long examId, CheatReportDTO dto) {
        if (dto.getType() != CHEAT_TYPE_SCREEN_SWITCH && dto.getType() != CHEAT_TYPE_AWAY) {
            throw new RuntimeException("暂不支持的上报类型");
        }
        Long userId = UserContext.getUserId();
        Exam exam = requireExam(examId);

        AnswerSheet sheet = latestSheet(examId, userId);
        if (sheet == null) {
            throw new RuntimeException("请先进入考试");
        }
        if (sheet.getStatus() != SHEET_STATUS_ANSWERING) {
            throw new RuntimeException("考试已交卷，无需上报");
        }

        int action = ACTION_NONE;
        boolean exceeded = false;
        int switchCount = sheet.getScreenSwitchCount() == null ? 0 : sheet.getScreenSwitchCount();
        // 仅切屏计入次数上限；离开超时只记录（已有应交卷时间硬截止兜底）
        if (dto.getType() == CHEAT_TYPE_SCREEN_SWITCH) {
            // 原子自增（守卫答题中状态），affected=0 说明已被并发收卷
            if (answerSheetMapper.increaseScreenSwitch(sheet.getId()) == 0) {
                throw new RuntimeException("考试已交卷，无需上报");
            }
            // 同事务读回自增后的最新值（行锁串行化，计数不重不漏）
            switchCount = answerSheetMapper.selectById(sheet.getId()).getScreenSwitchCount();
            int max = exam.getMaxScreenSwitch() == null ? 0 : exam.getMaxScreenSwitch();
            // 0=不限；第 max+1 次切屏（count > max）视为超限
            exceeded = max > 0 && switchCount > max;
            if (exceeded && Integer.valueOf(2).equals(exam.getScreenSwitchAction())) {
                // 配置为强制交卷：同事务内走共享核心收卷
                doFinish(sheet, exam, SHEET_STATUS_FORCE, SUBMIT_TYPE_FORCE);
                action = ACTION_FORCE;
            } else if (exceeded) {
                // 配置为仅警告：交由前端提示
                action = ACTION_WARN;
            }
        }

        // 切屏/离开超时都写记录，供教师端复核（handled=0 待处理）
        cheatRecordMapper.insert(buildCheatRecord(sheet, dto));

        CheatReportVO vo = new CheatReportVO();
        vo.setSwitchCount(switchCount);
        vo.setMaxScreenSwitch(exam.getMaxScreenSwitch());
        vo.setExceeded(exceeded);
        vo.setAction(action);
        log.info("用户[{}]上报防作弊事件，考试[{}]，类型[{}]，切屏次数：{}，动作：{}",
                userId, examId, dto.getType(), switchCount, action);
        return vo;
    }

    /**
     * 组装作弊记录：detail 是 JSON 列，非法 JSON 直接置 null 防插入失败（上报内容仅作参考，不值得为它回滚事务）
     */
    private CheatRecord buildCheatRecord(AnswerSheet sheet, CheatReportDTO dto) {
        CheatRecord record = new CheatRecord();
        record.setSheetId(sheet.getId());
        record.setExamId(sheet.getExamId());
        record.setUserId(sheet.getUserId());
        record.setType(dto.getType());
        record.setDescription(truncate(dto.getDescription(), 500));
        if (dto.getDetail() != null && !dto.getDetail().isBlank()) {
            try {
                objectMapper.readTree(dto.getDetail());
                record.setDetail(truncate(dto.getDetail(), 2000));
            } catch (Exception e) {
                record.setDetail(null);
            }
        }
        record.setOccurTime(LocalDateTime.now());
        record.setHandled(0);
        return record;
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

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
