package com.aiexam.exam.service.impl;

import com.aiexam.common.context.LoginUser;
import com.aiexam.common.context.UserContext;
import com.aiexam.exam.dto.CheatReportDTO;
import com.aiexam.exam.entity.AnswerSheet;
import com.aiexam.exam.entity.CheatRecord;
import com.aiexam.exam.entity.Exam;
import com.aiexam.exam.entity.ExamUser;
import com.aiexam.exam.mapper.AnswerSheetMapper;
import com.aiexam.exam.mapper.CheatRecordMapper;
import com.aiexam.exam.mapper.ExamMapper;
import com.aiexam.exam.mapper.ExamUserMapper;
import com.aiexam.exam.vo.CheatReportVO;
import com.aiexam.exam.vo.ExamStartVO;
import com.aiexam.paper.mapper.ExamPaperQuestionMapper;
import com.aiexam.paper.vo.PaperQuestionVO;
import com.aiexam.question.entity.Question;
import com.aiexam.question.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 学生答题服务单元测试（Mockito，不依赖 MySQL/Redis）
 * <p>
 * 覆盖：交卷三入口的状态/方式标记、共享核心的抢占幂等、防作弊上报、
 * 超时自动收卷、考试次数守卫。
 */
@ExtendWith(MockitoExtension.class)
class AnswerSheetServiceImplTest {

    private static final Long EXAM_ID = 10L;
    private static final Long PAPER_ID = 5L;
    private static final Long USER_ID = 100L;
    private static final Long SHEET_ID = 55L;
    private static final Long QUESTION_ID = 1L;
    private static final String DRAFT_KEY = "exam:answer:10:100";

    @BeforeAll
    static void initTableInfo() {
        // LambdaQueryWrapper.select(SFunction) 解析列需要 MP 的 TableInfo 缓存，
        // 纯单测没有 Spring 容器注册 Mapper，手动初始化一次
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AnswerSheet.class);
    }

    @Mock
    private ExamMapper examMapper;

    @Mock
    private ExamUserMapper examUserMapper;

    @Mock
    private AnswerSheetMapper answerSheetMapper;

    @Mock
    private CheatRecordMapper cheatRecordMapper;

    @Mock
    private ExamPaperQuestionMapper examPaperQuestionMapper;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AnswerSheetServiceImpl service;

    /** Db.saveBatch 是静态工具方法，需静态mock（Mockito 5 默认内联引擎支持） */
    private MockedStatic<Db> dbMock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        dbMock = mockStatic(Db.class);
        UserContext.set(new LoginUser(USER_ID, "student01", "张三"));
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        // 未打桩时 Mockito 对集合返回空集合、对象返回 null，正好覆盖缓存未命中/无草稿场景
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOps);
    }

    @AfterEach
    void tearDown() {
        dbMock.close();
        UserContext.clear();
    }

    // ==================== 手动交卷 ====================

    @Test
    @DisplayName("交卷：未进入过考试应报『请先进入考试』")
    void submit_withoutSheet_throws() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());

        assertThatThrownBy(() -> service.submit(EXAM_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("请先进入考试");
    }

    @Test
    @DisplayName("交卷：已交卷状态应报『无需重复交卷』")
    void submit_alreadySubmitted_throws() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());
        when(answerSheetMapper.selectOne(any(Wrapper.class)))
                .thenReturn(buildSheet(2, LocalDateTime.now().plusMinutes(10), 0));

        assertThatThrownBy(() -> service.submit(EXAM_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("您已交卷，无需重复交卷");
    }

    @Test
    @DisplayName("交卷：未超时按『手动交卷(2/1)』收卷并完成判分落库")
    void submit_inTime_marksManualAndGrades() {
        Exam exam = buildExam();
        when(examMapper.selectById(EXAM_ID)).thenReturn(exam);
        when(answerSheetMapper.selectOne(any(Wrapper.class)))
                .thenReturn(buildSheet(1, LocalDateTime.now().plusMinutes(10), 0));
        when(answerSheetMapper.markSubmitted(eq(SHEET_ID), anyInt(), anyInt(), any(LocalDateTime.class)))
                .thenReturn(1);
        // 草稿里第1题答 B；试卷1道单选（2分），标准答案 B → 客观题应得 2 分
        when(redisTemplate.<String, Object>opsForHash().entries(DRAFT_KEY))
                .thenReturn((Map) Map.of(String.valueOf(QUESTION_ID), "B"));
        when(examPaperQuestionMapper.selectPaperQuestions(PAPER_ID))
                .thenReturn(List.of(buildPaperQuestion()));
        when(questionMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(buildQuestion()));

        service.submit(EXAM_ID);

        verify(answerSheetMapper).markSubmitted(eq(SHEET_ID), eq(2), eq(1), any(LocalDateTime.class));
        // 明细批量落库 + 得分回写 + 最高分刷新 + 清草稿
        dbMock.verify(() -> Db.saveBatch(anyList()));
        ArgumentCaptor<AnswerSheet> captor = ArgumentCaptor.forClass(AnswerSheet.class);
        verify(answerSheetMapper).updateById(captor.capture());
        assertThat(captor.getValue().getObjectiveScore()).isEqualByComparingTo("2");
        assertThat(captor.getValue().getTotalScore()).isEqualByComparingTo("2");
        // BigDecimal 的 equals 对 scale 敏感（2.00 != 2），用 compareTo 比较
        verify(examUserMapper).updateBestScore(eq(EXAM_ID), eq(USER_ID),
                argThat(score -> score.compareTo(new BigDecimal("2")) == 0));
        verify(redisTemplate).delete(DRAFT_KEY);
    }

    @Test
    @DisplayName("交卷：超过应交卷时间仍收卷，但标记为『超时自动交卷(4/2)』")
    void submit_overtime_marksTimeout() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());
        when(answerSheetMapper.selectOne(any(Wrapper.class)))
                .thenReturn(buildSheet(1, LocalDateTime.now().minusMinutes(1), 0));
        when(answerSheetMapper.markSubmitted(eq(SHEET_ID), anyInt(), anyInt(), any(LocalDateTime.class)))
                .thenReturn(1);

        service.submit(EXAM_ID);

        verify(answerSheetMapper).markSubmitted(eq(SHEET_ID), eq(4), eq(2), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("交卷：答卷已被其他入口收卷时幂等跳过，不落明细不清草稿")
    void submit_preempted_isIdempotent() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());
        when(answerSheetMapper.selectOne(any(Wrapper.class)))
                .thenReturn(buildSheet(1, LocalDateTime.now().plusMinutes(10), 0));
        when(answerSheetMapper.markSubmitted(eq(SHEET_ID), anyInt(), anyInt(), any(LocalDateTime.class)))
                .thenReturn(0);

        service.submit(EXAM_ID);

        dbMock.verify(() -> Db.saveBatch(anyList()), never());
        verify(answerSheetMapper, never()).updateById(any(AnswerSheet.class));
        verify(examUserMapper, never()).updateBestScore(any(), any(), any());
        verify(redisTemplate, never()).delete(any(String.class));
    }

    // ==================== 进入考试（次数守卫） ====================

    @Test
    @DisplayName("进入考试：考试次数用尽应报错且不建答卷")
    void start_attemptsExhausted_throws() {
        Exam exam = buildExam();
        when(examMapper.selectById(EXAM_ID)).thenReturn(exam);
        when(examUserMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(answerSheetMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(examUserMapper.increaseAttempts(EXAM_ID, USER_ID, 1)).thenReturn(0);

        assertThatThrownBy(() -> service.start(EXAM_ID, "127.0.0.1", "ua"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("考试次数已用完，无法再次进入考试");
        verify(answerSheetMapper, never()).insert(any(AnswerSheet.class));
    }

    @Test
    @DisplayName("进入考试：首次进入自增次数并以正确 attemptNo 建卷")
    void start_firstTime_increasesAttemptsAndCreatesSheet() {
        Exam exam = buildExam();
        when(examMapper.selectById(EXAM_ID)).thenReturn(exam);
        when(examUserMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(answerSheetMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(examUserMapper.increaseAttempts(EXAM_ID, USER_ID, 1)).thenReturn(1);
        ExamUser examUser = new ExamUser();
        examUser.setExamId(EXAM_ID);
        examUser.setUserId(USER_ID);
        examUser.setAttempts(1);
        when(examUserMapper.selectOne(any(Wrapper.class))).thenReturn(examUser);

        ExamStartVO vo = service.start(EXAM_ID, "127.0.0.1", "ua");

        assertThat(vo).isNotNull();
        assertThat(vo.getExamId()).isEqualTo(EXAM_ID);
        verify(examUserMapper).increaseAttempts(EXAM_ID, USER_ID, 1);
        ArgumentCaptor<AnswerSheet> captor = ArgumentCaptor.forClass(AnswerSheet.class);
        verify(answerSheetMapper).insert(captor.capture());
        assertThat(captor.getValue().getAttemptNo()).isEqualTo(1);
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
        // 应交卷时间 = min(now+时长, 考试截止时间)
        assertThat(captor.getValue().getEndTime()).isBeforeOrEqualTo(exam.getEndTime());
    }

    @Test
    @DisplayName("进入考试：已有答题中答卷则复用，不消耗次数不建卷")
    void start_existingAnsweringSheet_reusedWithoutAttempts() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());
        when(examUserMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(answerSheetMapper.selectOne(any(Wrapper.class)))
                .thenReturn(buildSheet(1, LocalDateTime.now().plusMinutes(10), 0));

        ExamStartVO vo = service.start(EXAM_ID, "127.0.0.1", "ua");

        assertThat(vo.getSheetId()).isEqualTo(SHEET_ID);
        verify(examUserMapper, never()).increaseAttempts(any(), any(), anyInt());
        verify(answerSheetMapper, never()).insert(any(AnswerSheet.class));
    }

    // ==================== 防作弊上报 ====================

    @Test
    @DisplayName("上报：切屏超限且配置强制交卷 → 强制收卷(3/3)并返回动作2")
    void reportCheat_screenExceededForceSubmits() {
        Exam exam = buildExam(); // maxScreenSwitch=2, action=2
        when(examMapper.selectById(EXAM_ID)).thenReturn(exam);
        when(answerSheetMapper.selectOne(any(Wrapper.class)))
                .thenReturn(buildSheet(1, LocalDateTime.now().plusMinutes(10), 2));
        when(answerSheetMapper.increaseScreenSwitch(SHEET_ID)).thenReturn(1);
        // 自增后读回最新计数=3（>2 超限）
        when(answerSheetMapper.selectById(SHEET_ID))
                .thenReturn(buildSheet(1, LocalDateTime.now().plusMinutes(10), 3));
        when(answerSheetMapper.markSubmitted(eq(SHEET_ID), anyInt(), anyInt(), any(LocalDateTime.class)))
                .thenReturn(1);

        CheatReportVO vo = service.reportCheat(EXAM_ID, buildCheatDTO(1, null, null));

        assertThat(vo.getSwitchCount()).isEqualTo(3);
        assertThat(vo.getExceeded()).isTrue();
        assertThat(vo.getAction()).isEqualTo(2);
        verify(answerSheetMapper).markSubmitted(eq(SHEET_ID), eq(3), eq(3), any(LocalDateTime.class));
        ArgumentCaptor<CheatRecord> captor = ArgumentCaptor.forClass(CheatRecord.class);
        verify(cheatRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(1);
        assertThat(captor.getValue().getSheetId()).isEqualTo(SHEET_ID);
        assertThat(captor.getValue().getHandled()).isEqualTo(0);
    }

    @Test
    @DisplayName("上报：切屏超限但配置仅警告 → 返回动作1，不强制交卷")
    void reportCheat_screenExceededWarnOnly() {
        Exam exam = buildExam();
        exam.setScreenSwitchAction(1);
        when(examMapper.selectById(EXAM_ID)).thenReturn(exam);
        when(answerSheetMapper.selectOne(any(Wrapper.class)))
                .thenReturn(buildSheet(1, LocalDateTime.now().plusMinutes(10), 2));
        when(answerSheetMapper.increaseScreenSwitch(SHEET_ID)).thenReturn(1);
        when(answerSheetMapper.selectById(SHEET_ID))
                .thenReturn(buildSheet(1, LocalDateTime.now().plusMinutes(10), 3));

        CheatReportVO vo = service.reportCheat(EXAM_ID, buildCheatDTO(1, null, null));

        assertThat(vo.getExceeded()).isTrue();
        assertThat(vo.getAction()).isEqualTo(1);
        verify(answerSheetMapper, never()).markSubmitted(any(), anyInt(), anyInt(), any());
        verify(cheatRecordMapper).insert(any(CheatRecord.class));
    }

    @Test
    @DisplayName("上报：切屏未超限 → 仅记录，返回动作0")
    void reportCheat_screenNotExceeded() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());
        when(answerSheetMapper.selectOne(any(Wrapper.class)))
                .thenReturn(buildSheet(1, LocalDateTime.now().plusMinutes(10), 0));
        when(answerSheetMapper.increaseScreenSwitch(SHEET_ID)).thenReturn(1);
        when(answerSheetMapper.selectById(SHEET_ID))
                .thenReturn(buildSheet(1, LocalDateTime.now().plusMinutes(10), 1));

        CheatReportVO vo = service.reportCheat(EXAM_ID, buildCheatDTO(1, "切屏", null));

        assertThat(vo.getSwitchCount()).isEqualTo(1);
        assertThat(vo.getExceeded()).isFalse();
        assertThat(vo.getAction()).isEqualTo(0);
        verify(answerSheetMapper, never()).markSubmitted(any(), anyInt(), anyInt(), any());
        verify(cheatRecordMapper).insert(any(CheatRecord.class));
    }

    @Test
    @DisplayName("上报：离开超时只记 cheat_record 不累加切屏计数")
    void reportCheat_awayType_onlyRecords() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());
        when(answerSheetMapper.selectOne(any(Wrapper.class)))
                .thenReturn(buildSheet(1, LocalDateTime.now().plusMinutes(10), 2));

        CheatReportVO vo = service.reportCheat(EXAM_ID,
                buildCheatDTO(2, "离开超过60秒", "{\"reason\":\"blur\"}"));

        assertThat(vo.getSwitchCount()).isEqualTo(2);
        assertThat(vo.getExceeded()).isFalse();
        assertThat(vo.getAction()).isEqualTo(0);
        verify(answerSheetMapper, never()).increaseScreenSwitch(any());
        ArgumentCaptor<CheatRecord> captor = ArgumentCaptor.forClass(CheatRecord.class);
        verify(cheatRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(2);
        assertThat(captor.getValue().getDetail()).isEqualTo("{\"reason\":\"blur\"}");
    }

    @Test
    @DisplayName("上报：detail 非法 JSON 时置 null，不影响记录落库")
    void reportCheat_invalidJsonDetail_setNull() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());
        when(answerSheetMapper.selectOne(any(Wrapper.class)))
                .thenReturn(buildSheet(1, LocalDateTime.now().plusMinutes(10), 0));

        service.reportCheat(EXAM_ID, buildCheatDTO(2, null, "{bad json"));

        ArgumentCaptor<CheatRecord> captor = ArgumentCaptor.forClass(CheatRecord.class);
        verify(cheatRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getDetail()).isNull();
    }

    @Test
    @DisplayName("上报：不支持的上报类型直接报错")
    void reportCheat_unknownType_throws() {
        assertThatThrownBy(() -> service.reportCheat(EXAM_ID, buildCheatDTO(9, null, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("暂不支持的上报类型");
    }

    @Test
    @DisplayName("上报：答卷已交卷再上报应报错")
    void reportCheat_afterSubmitted_throws() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());
        when(answerSheetMapper.selectOne(any(Wrapper.class)))
                .thenReturn(buildSheet(2, LocalDateTime.now().plusMinutes(10), 0));

        assertThatThrownBy(() -> service.reportCheat(EXAM_ID, buildCheatDTO(1, null, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("考试已交卷，无需上报");
    }

    @Test
    @DisplayName("上报：切屏自增失效（已被并发收卷）应报错")
    void reportCheat_switchIncreaseFailed_throws() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());
        when(answerSheetMapper.selectOne(any(Wrapper.class)))
                .thenReturn(buildSheet(1, LocalDateTime.now().plusMinutes(10), 0));
        when(answerSheetMapper.increaseScreenSwitch(SHEET_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.reportCheat(EXAM_ID, buildCheatDTO(1, null, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("考试已交卷，无需上报");
    }

    // ==================== 超时自动交卷 ====================

    @Test
    @DisplayName("批量超时收卷：逐张独立收卷，单张失败不阻断批次")
    void autoSubmitTimeoutSheets_succeedsPerSheetAndContinuesOnError() {
        AnswerSheet s1 = buildSheet(1, LocalDateTime.now().minusMinutes(1), 0);
        s1.setId(55L);
        AnswerSheet s2 = buildSheet(1, LocalDateTime.now().minusMinutes(1), 0);
        s2.setId(56L);
        when(answerSheetMapper.selectList(any(Wrapper.class))).thenReturn(List.of(s1, s2));
        when(answerSheetMapper.selectById(55L)).thenReturn(s1);
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());
        when(answerSheetMapper.markSubmitted(eq(55L), anyInt(), anyInt(), any(LocalDateTime.class)))
                .thenReturn(1);
        // 第二张查库时抛异常，应被跳过而不是中断
        when(answerSheetMapper.selectById(56L)).thenThrow(new RuntimeException("模拟数据库异常"));

        int done = service.autoSubmitTimeoutSheets();

        assertThat(done).isEqualTo(1);
        verify(answerSheetMapper).markSubmitted(eq(55L), eq(4), eq(2), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("单张自动收卷：答卷已被其他入口收卷时幂等返回false")
    void autoSubmit_alreadyCollected_returnsFalse() {
        when(answerSheetMapper.selectById(SHEET_ID))
                .thenReturn(buildSheet(2, LocalDateTime.now().minusMinutes(1), 0));

        boolean done = service.autoSubmit(SHEET_ID);

        assertThat(done).isFalse();
        verify(answerSheetMapper, never()).markSubmitted(any(), anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("单张自动收卷：考试不存在时返回false不抛异常")
    void autoSubmit_examMissing_returnsFalse() {
        when(answerSheetMapper.selectById(SHEET_ID))
                .thenReturn(buildSheet(1, LocalDateTime.now().minusMinutes(1), 0));
        when(examMapper.selectById(EXAM_ID)).thenReturn(null);

        assertThat(service.autoSubmit(SHEET_ID)).isFalse();
    }

    // ==================== 测试数据 ====================

    private Exam buildExam() {
        Exam exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setPaperId(PAPER_ID);
        exam.setName("Java 期末考试");
        exam.setStartTime(LocalDateTime.now().minusMinutes(10));
        exam.setEndTime(LocalDateTime.now().plusMinutes(30));
        exam.setDuration(30);
        exam.setAllowLateMinutes(0);
        exam.setMaxAttempts(1);
        exam.setRandomOrder(0);
        exam.setMaxScreenSwitch(2);
        exam.setScreenSwitchAction(2);
        return exam;
    }

    private AnswerSheet buildSheet(int status, LocalDateTime endTime, int switchCount) {
        AnswerSheet sheet = new AnswerSheet();
        sheet.setId(SHEET_ID);
        sheet.setExamId(EXAM_ID);
        sheet.setPaperId(PAPER_ID);
        sheet.setUserId(USER_ID);
        sheet.setAttemptNo(1);
        sheet.setStatus(status);
        sheet.setStartTime(LocalDateTime.now().minusMinutes(10));
        sheet.setEndTime(endTime);
        sheet.setScreenSwitchCount(switchCount);
        sheet.setQuestionOrderSeed(123);
        return sheet;
    }

    private PaperQuestionVO buildPaperQuestion() {
        PaperQuestionVO pq = new PaperQuestionVO();
        pq.setQuestionId(QUESTION_ID);
        pq.setSortOrder(1);
        pq.setQuestionScore(new BigDecimal("2.00"));
        pq.setType(1);
        pq.setTitle("Java 中用于定义常量的关键字是？");
        return pq;
    }

    private Question buildQuestion() {
        Question question = new Question();
        question.setId(QUESTION_ID);
        question.setType(1);
        question.setAnswer("B");
        return question;
    }

    private CheatReportDTO buildCheatDTO(int type, String description, String detail) {
        CheatReportDTO dto = new CheatReportDTO();
        dto.setType(type);
        dto.setDescription(description);
        dto.setDetail(detail);
        return dto;
    }
}
