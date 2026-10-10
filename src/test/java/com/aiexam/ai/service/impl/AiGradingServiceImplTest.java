package com.aiexam.ai.service.impl;

import com.aiexam.ai.client.AiChatClient;
import com.aiexam.ai.client.AiGradePrompt;
import com.aiexam.ai.client.AiGradeResult;
import com.aiexam.ai.dto.ManualGradeDTO;
import com.aiexam.ai.mapper.AiGradingMapper;
import com.aiexam.ai.vo.GradeBatchResultVO;
import com.aiexam.ai.vo.GradeDetailVO;
import com.aiexam.ai.vo.PendingGradeItemVO;
import com.aiexam.ai.vo.SheetScoreSummaryVO;
import com.aiexam.ai.vo.SubjectiveSummaryVO;
import com.aiexam.common.context.LoginUser;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 判卷服务单元测试（Mockito，不依赖 MySQL/Redis/网络）
 * <p>
 * 覆盖：权限、防重锁、批量判卷成败收集、越界分数、幂等守卫、成绩汇总触发、人工改分校验与落库、复核视图。
 */
@ExtendWith(MockitoExtension.class)
class AiGradingServiceImplTest {

    private static final Long EXAM_ID = 10L;
    private static final Long SHEET_ID = 55L;
    private static final Long DETAIL_ID = 1001L;
    private static final Long TEACHER_ID = 1L;
    private static final String LOCK_KEY = "ai:grading:10";

    @Mock
    private AiGradingMapper aiGradingMapper;

    @Mock
    private AiChatClient aiChatClient;

    @Mock
    private AnswerDetailMapper answerDetailMapper;

    @Mock
    private AnswerSheetMapper answerSheetMapper;

    @Mock
    private ExamUserMapper examUserMapper;

    @Mock
    private ExamMapper examMapper;

    @Mock
    private QuestionMapper questionMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AiGradingServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        UserContext.set(new LoginUser(TEACHER_ID, "teacher01", "王老师"));
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // ==================== 前置校验 ====================

    @Test
    @DisplayName("批量判卷：考试不存在应报错")
    void gradeExam_examNotFound_throws() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.gradeExam(EXAM_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("考试不存在");
    }

    @Test
    @DisplayName("批量判卷：防重锁被占用应报错，不调大模型不释放他人锁")
    void gradeExam_lockTaken_throws() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());
        when(aiGradingMapper.selectPendingGrades(EXAM_ID)).thenReturn(List.of(buildPending()));
        when(lock()).thenReturn(false);

        assertThatThrownBy(() -> service.gradeExam(EXAM_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("该考试正在判卷中，请稍后再试");
        verify(aiChatClient, never()).grade(any(AiGradePrompt.class));
        verify(aiGradingMapper, never()).markAiGraded(any(), any(), anyInt(), any(), any());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("批量判卷：无待判明细直接返回 0/0/0，不调大模型不汇总")
    void gradeExam_noPending_noop() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());
        when(aiGradingMapper.selectPendingGrades(EXAM_ID)).thenReturn(List.of());

        GradeBatchResultVO vo = service.gradeExam(EXAM_ID);

        assertThat(vo.getPendingCount()).isZero();
        assertThat(vo.getSuccessCount()).isZero();
        assertThat(vo.getFailedCount()).isZero();
        verify(aiChatClient, never()).grade(any(AiGradePrompt.class));
        verify(answerSheetMapper, never()).updateById(any(AnswerSheet.class));
    }

    // ==================== 批量判卷 ====================

    @Test
    @DisplayName("批量判卷：2 题成功——分数规整/满分判定/评语截断/锁释放")
    void gradeExam_twoItems_success() {
        stubTeacherAndExamWithLock();
        PendingGradeItemVO item1 = buildPending();
        PendingGradeItemVO item2 = buildPending();
        item2.setDetailId(1002L);
        item2.setQuestionId(202L);
        item2.setFullScore(new BigDecimal("8.00"));
        when(aiGradingMapper.selectPendingGrades(EXAM_ID)).thenReturn(List.of(item1, item2));
        // 第1题满分、评语超长；第2题部分分
        when(aiChatClient.grade(any(AiGradePrompt.class))).thenReturn(
                new AiGradeResult(new BigDecimal("10.00"), "很".repeat(600)),
                new AiGradeResult(new BigDecimal("7.5"), "基本正确"));
        stubCompleteSummary(new BigDecimal("8.00"));

        GradeBatchResultVO vo = service.gradeExam(EXAM_ID);

        assertThat(vo.getSuccessCount()).isEqualTo(2);
        assertThat(vo.getFailedCount()).isZero();
        ArgumentCaptor<BigDecimal> scoreCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<Integer> correctCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> commentCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiGradingMapper, times(2)).markAiGraded(any(), scoreCaptor.capture(),
                correctCaptor.capture(), commentCaptor.capture(), any(LocalDateTime.class));
        // 统一 2 位小数；满分→1、部分分→0；评语截断到 500
        assertThat(scoreCaptor.getAllValues()).satisfies(scores -> {
            assertThat(scores.get(0)).isEqualByComparingTo("10.00");
            assertThat(scores.get(1)).isEqualByComparingTo("7.50");
        });
        assertThat(correctCaptor.getAllValues()).containsExactly(1, 0);
        assertThat(commentCaptor.getAllValues().get(0)).hasSize(500);
        verify(redisTemplate).delete(LOCK_KEY);
    }

    @Test
    @DisplayName("批量判卷：单题失败不阻断批次，失败明细正确")
    void gradeExam_firstFails_secondSucceeds() {
        stubTeacherAndExamWithLock();
        PendingGradeItemVO item1 = buildPending();
        PendingGradeItemVO item2 = buildPending();
        item2.setDetailId(1002L);
        item2.setQuestionId(202L);
        when(aiGradingMapper.selectPendingGrades(EXAM_ID)).thenReturn(List.of(item1, item2));
        when(aiChatClient.grade(any(AiGradePrompt.class)))
                .thenThrow(new RuntimeException("大模型调用失败：连接超时"))
                .thenReturn(new AiGradeResult(new BigDecimal("7.5"), "基本正确"));
        // 第1题仍待判 → 卷未判完，不汇总
        stubIncompleteSummary();

        GradeBatchResultVO vo = service.gradeExam(EXAM_ID);

        assertThat(vo.getSuccessCount()).isEqualTo(1);
        assertThat(vo.getFailedCount()).isEqualTo(1);
        assertThat(vo.getFailures()).hasSize(1);
        assertThat(vo.getFailures().get(0).getSheetId()).isEqualTo(SHEET_ID);
        assertThat(vo.getFailures().get(0).getQuestionId()).isEqualTo(201L);
        assertThat(vo.getFailures().get(0).getReason()).contains("连接超时");
        verify(aiGradingMapper, times(1)).markAiGraded(any(), any(), anyInt(), any(), any());
        verify(redisTemplate).delete(LOCK_KEY);
    }

    @Test
    @DisplayName("批量判卷：大模型返回越界分数进失败清单，不落库")
    void gradeExam_scoreOutOfRange_fails() {
        stubTeacherAndExamWithLock();
        when(aiGradingMapper.selectPendingGrades(EXAM_ID)).thenReturn(List.of(buildPending()));
        when(aiChatClient.grade(any(AiGradePrompt.class)))
                .thenReturn(new AiGradeResult(new BigDecimal("99"), "离谱"));

        GradeBatchResultVO vo = service.gradeExam(EXAM_ID);

        assertThat(vo.getFailedCount()).isEqualTo(1);
        assertThat(vo.getFailures().get(0).getReason()).contains("超过本题满分");
        verify(aiGradingMapper, never()).markAiGraded(any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("批量判卷：落库 0 行（已被并发判分）计入成功不报错")
    void gradeExam_preempted_countedSuccess() {
        stubTeacherAndExamWithLock();
        when(aiGradingMapper.selectPendingGrades(EXAM_ID)).thenReturn(List.of(buildPending()));
        when(aiChatClient.grade(any(AiGradePrompt.class)))
                .thenReturn(new AiGradeResult(new BigDecimal("8"), "ok"));
        when(aiGradingMapper.markAiGraded(any(), any(), anyInt(), any(), any())).thenReturn(0);
        stubIncompleteSummary();

        GradeBatchResultVO vo = service.gradeExam(EXAM_ID);

        assertThat(vo.getSuccessCount()).isEqualTo(1);
        assertThat(vo.getFailedCount()).isZero();
        assertThat(vo.getFailures()).isEmpty();
    }

    // ==================== 成绩汇总 ====================

    @Test
    @DisplayName("汇总：主观题判完更新主观分/总分并刷新 best_score")
    void gradeExam_summarizesWhenComplete() {
        stubTeacherAndExamWithLock();
        PendingGradeItemVO item1 = buildPending();
        PendingGradeItemVO item2 = buildPending();
        item2.setDetailId(1002L);
        item2.setQuestionId(202L);
        when(aiGradingMapper.selectPendingGrades(EXAM_ID)).thenReturn(List.of(item1, item2));
        when(aiChatClient.grade(any(AiGradePrompt.class))).thenReturn(
                new AiGradeResult(new BigDecimal("10.00"), "满分"),
                new AiGradeResult(new BigDecimal("6.00"), "部分"));
        // 客观 80 + 主观 8 = 88
        stubCompleteSummary(new BigDecimal("8.00"));

        service.gradeExam(EXAM_ID);

        ArgumentCaptor<AnswerSheet> captor = ArgumentCaptor.forClass(AnswerSheet.class);
        verify(answerSheetMapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(SHEET_ID);
        assertThat(captor.getValue().getSubjectiveScore()).isEqualByComparingTo("8.00");
        assertThat(captor.getValue().getTotalScore()).isEqualByComparingTo("88.00");
        verify(examUserMapper).updateBestScore(eq(EXAM_ID), eq(100L),
                argThat(score -> score.compareTo(new BigDecimal("88.00")) == 0));
    }

    @Test
    @DisplayName("汇总：卷上仍有待判主观题时不更新任何成绩")
    void gradeExam_incompleteSheet_notSummarized() {
        stubTeacherAndExamWithLock();
        when(aiGradingMapper.selectPendingGrades(EXAM_ID)).thenReturn(List.of(buildPending()));
        when(aiChatClient.grade(any(AiGradePrompt.class)))
                .thenReturn(new AiGradeResult(new BigDecimal("8"), "ok"));
        stubIncompleteSummary();

        service.gradeExam(EXAM_ID);

        verify(answerSheetMapper, never()).updateById(any(AnswerSheet.class));
        verify(examUserMapper, never()).updateBestScore(any(), any(), any());
    }

    @Test
    @DisplayName("汇总：客观分为 null 时按 0 参与合计，不抛异常")
    void summarize_objectiveNull_safe() {
        stubTeacherAndExamWithLock();
        when(aiGradingMapper.selectPendingGrades(EXAM_ID)).thenReturn(List.of(buildPending()));
        when(aiChatClient.grade(any(AiGradePrompt.class)))
                .thenReturn(new AiGradeResult(new BigDecimal("8.00"), "ok"));
        AnswerSheet sheet = buildSheet();
        sheet.setObjectiveScore(null);
        sheet.setTotalScore(null);
        when(answerSheetMapper.selectById(SHEET_ID)).thenReturn(sheet);
        SubjectiveSummaryVO progress = new SubjectiveSummaryVO();
        progress.setUngradedCount(0L);
        progress.setSubjectiveScore(new BigDecimal("8.00"));
        when(aiGradingMapper.selectSubjectiveSummary(SHEET_ID)).thenReturn(progress);

        service.gradeExam(EXAM_ID);

        verify(examUserMapper).updateBestScore(eq(EXAM_ID), eq(100L),
                argThat(score -> score.compareTo(new BigDecimal("8.00")) == 0));
    }

    // ==================== 人工改分 ====================

    @Test
    @DisplayName("人工改分：明细不存在应报错")
    void manualGrade_detailMissing_throws() {
        when(answerDetailMapper.selectById(DETAIL_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.manualGrade(DETAIL_ID, buildManualDTO(new BigDecimal("8"))))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("答题明细不存在");
    }

    @Test
    @DisplayName("人工改分：答卷未交卷不能判分")
    void manualGrade_sheetAnswering_throws() {
        when(answerDetailMapper.selectById(DETAIL_ID)).thenReturn(buildDetail());
        AnswerSheet sheet = buildSheet();
        sheet.setStatus(1);
        when(answerSheetMapper.selectById(SHEET_ID)).thenReturn(sheet);

        assertThatThrownBy(() -> service.manualGrade(DETAIL_ID, buildManualDTO(new BigDecimal("8"))))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("答卷未交卷，不能判分");
    }

    @Test
    @DisplayName("人工改分：客观题不支持")
    void manualGrade_objectiveQuestion_throws() {
        when(answerDetailMapper.selectById(DETAIL_ID)).thenReturn(buildDetail());
        when(answerSheetMapper.selectById(SHEET_ID)).thenReturn(buildSheet());
        Question question = buildQuestion();
        question.setType(1);
        when(questionMapper.selectById(201L)).thenReturn(question);

        assertThatThrownBy(() -> service.manualGrade(DETAIL_ID, buildManualDTO(new BigDecimal("8"))))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("仅主观题支持人工改分");
    }

    @Test
    @DisplayName("人工改分：负分与超满分应报错且不落库")
    void manualGrade_invalidScore_throws() {
        when(answerDetailMapper.selectById(DETAIL_ID)).thenReturn(buildDetail());
        when(answerSheetMapper.selectById(SHEET_ID)).thenReturn(buildSheet());
        when(questionMapper.selectById(201L)).thenReturn(buildQuestion());

        assertThatThrownBy(() -> service.manualGrade(DETAIL_ID, buildManualDTO(new BigDecimal("-1"))))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("分数不能为负");
        assertThatThrownBy(() -> service.manualGrade(DETAIL_ID, buildManualDTO(new BigDecimal("11"))))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("超过本题满分");
        verify(aiGradingMapper, never()).markManualGraded(any(), any(), anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("人工改分：成功带评语，判分人为当前教师，返回整卷汇总")
    void manualGrade_success_withComment() {
        stubManualChain();
        when(aiGradingMapper.markManualGraded(any(), any(), anyInt(), any(), any(), any())).thenReturn(1);

        SheetScoreSummaryVO vo = service.manualGrade(DETAIL_ID, buildManualDTO(new BigDecimal("10.00")));

        ArgumentCaptor<BigDecimal> scoreCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<Integer> correctCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(aiGradingMapper).markManualGraded(eq(DETAIL_ID), scoreCaptor.capture(),
                correctCaptor.capture(), eq(TEACHER_ID), eq("很好"), any(LocalDateTime.class));
        assertThat(scoreCaptor.getValue()).isEqualByComparingTo("10.00");
        assertThat(correctCaptor.getValue()).isEqualTo(1);
        assertThat(vo.getTotalScore()).isEqualByComparingTo("88.00");
        assertThat(vo.getSubjectiveScore()).isEqualByComparingTo("8.00");
    }

    @Test
    @DisplayName("人工改分：不传评语时保留原评语（comment=null）")
    void manualGrade_success_withoutComment() {
        stubManualChain();
        when(aiGradingMapper.markManualGraded(any(), any(), anyInt(), any(), any(), any())).thenReturn(1);

        ManualGradeDTO dto = buildManualDTO(new BigDecimal("7"));
        dto.setComment(null);
        service.manualGrade(DETAIL_ID, dto);

        verify(aiGradingMapper).markManualGraded(eq(DETAIL_ID), any(), eq(0), eq(TEACHER_ID),
                isNullComment(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("人工改分：整卷仍有待判主观题时返回未定稿汇总（subjectiveScore=null）")
    void manualGrade_incompleteSheet_fallbackVO() {
        stubManualChain();
        when(aiGradingMapper.markManualGraded(any(), any(), anyInt(), any(), any(), any())).thenReturn(1);
        stubIncompleteSummary();

        SheetScoreSummaryVO vo = service.manualGrade(DETAIL_ID, buildManualDTO(new BigDecimal("7")));

        assertThat(vo.getSheetId()).isEqualTo(SHEET_ID);
        assertThat(vo.getSubjectiveScore()).isNull();
        assertThat(vo.getTotalScore()).isEqualByComparingTo("80.00");
        verify(answerSheetMapper, never()).updateById(any(AnswerSheet.class));
    }

    // ==================== 复核视图 ====================

    @Test
    @DisplayName("复核视图：逐题详情原样透传（null 字段不报错）")
    void sheetDetails_passthrough() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());
        GradeDetailVO row = new GradeDetailVO();
        row.setDetailId(DETAIL_ID);
        row.setType(5);
        when(aiGradingMapper.selectSheetGradeDetails(EXAM_ID, SHEET_ID)).thenReturn(List.of(row));

        List<GradeDetailVO> list = service.getSheetGradeDetails(EXAM_ID, SHEET_ID);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getDetailId()).isEqualTo(DETAIL_ID);
        assertThat(list.get(0).getScore()).isNull();
    }

    @Test
    @DisplayName("复核视图：考试不存在应报错")
    void sheetDetails_examNotFound_throws() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(null);
        assertThatThrownBy(() -> service.getSheetGradeDetails(EXAM_ID, SHEET_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("考试不存在");
        verify(aiGradingMapper, never()).selectSheetGradeDetails(any(), any());
    }

    // ==================== 测试数据 ====================

    private void stubTeacherAndExamWithLock() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());
        // lenient：无待判明细场景不会走到拿锁
        lenient().when(lock()).thenReturn(true);
    }

    private void stubCompleteSummary(BigDecimal subjective) {
        AnswerSheet sheet = buildSheet();
        when(answerSheetMapper.selectById(SHEET_ID)).thenReturn(sheet);
        SubjectiveSummaryVO progress = new SubjectiveSummaryVO();
        progress.setUngradedCount(0L);
        progress.setSubjectiveScore(subjective);
        when(aiGradingMapper.selectSubjectiveSummary(SHEET_ID)).thenReturn(progress);
    }

    private void stubIncompleteSummary() {
        SubjectiveSummaryVO progress = new SubjectiveSummaryVO();
        progress.setUngradedCount(1L);
        progress.setSubjectiveScore(new BigDecimal("7.50"));
        when(aiGradingMapper.selectSubjectiveSummary(SHEET_ID)).thenReturn(progress);
    }

    private void stubManualChain() {
        when(answerDetailMapper.selectById(DETAIL_ID)).thenReturn(buildDetail());
        when(answerSheetMapper.selectById(SHEET_ID)).thenReturn(buildSheet());
        when(questionMapper.selectById(201L)).thenReturn(buildQuestion());
        // 判完卷上唯一主观题 → 汇总：客观 80 + 主观 8 = 88
        stubCompleteSummary(new BigDecimal("8.00"));
    }

    private Boolean lock() {
        return redisTemplate.opsForValue().setIfAbsent(eq(LOCK_KEY), anyString(), any());
    }

    private Exam buildExam() {
        Exam exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setPaperId(5L);
        exam.setName("Java 期末考试");
        return exam;
    }

    private PendingGradeItemVO buildPending() {
        PendingGradeItemVO item = new PendingGradeItemVO();
        item.setDetailId(DETAIL_ID);
        item.setSheetId(SHEET_ID);
        item.setQuestionId(201L);
        item.setSortOrder(5);
        item.setTitle("简述 JVM 内存分区");
        item.setUserAnswer("堆、栈、方法区");
        item.setCorrectAnswer("堆、虚拟机栈、本地方法栈、方法区、程序计数器");
        item.setAnalysis("按内存五大分区作答");
        item.setFullScore(new BigDecimal("10.00"));
        return item;
    }

    private AnswerSheet buildSheet() {
        AnswerSheet sheet = new AnswerSheet();
        sheet.setId(SHEET_ID);
        sheet.setExamId(EXAM_ID);
        sheet.setUserId(100L);
        sheet.setStatus(2);
        sheet.setObjectiveScore(new BigDecimal("80.00"));
        sheet.setTotalScore(new BigDecimal("80.00"));
        return sheet;
    }

    private AnswerDetail buildDetail() {
        AnswerDetail detail = new AnswerDetail();
        detail.setId(DETAIL_ID);
        detail.setSheetId(SHEET_ID);
        detail.setQuestionId(201L);
        detail.setFullScore(new BigDecimal("10.00"));
        return detail;
    }

    private Question buildQuestion() {
        Question question = new Question();
        question.setId(201L);
        question.setType(5);
        return question;
    }

    private ManualGradeDTO buildManualDTO(BigDecimal score) {
        ManualGradeDTO dto = new ManualGradeDTO();
        dto.setScore(score);
        dto.setComment("很好");
        return dto;
    }

    private static String isNullComment() {
        return org.mockito.ArgumentMatchers.isNull();
    }
}
