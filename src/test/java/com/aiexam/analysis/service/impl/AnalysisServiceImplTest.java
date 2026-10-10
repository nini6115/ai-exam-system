package com.aiexam.analysis.service.impl;

import com.aiexam.analysis.dto.ScoreQueryDTO;
import com.aiexam.analysis.mapper.AnalysisMapper;
import com.aiexam.analysis.vo.ExamStatsVO;
import com.aiexam.analysis.vo.ScoreExportVO;
import com.aiexam.analysis.vo.ScoreItemVO;
import com.aiexam.analysis.vo.ScoreSegmentVO;
import com.aiexam.common.context.LoginUser;
import com.aiexam.common.context.UserContext;
import com.aiexam.common.vo.PageVO;
import com.aiexam.exam.entity.Exam;
import com.aiexam.exam.entity.ExamUser;
import com.aiexam.exam.mapper.ExamMapper;
import com.aiexam.exam.mapper.ExamUserMapper;
import com.aiexam.paper.entity.ExamPaper;
import com.aiexam.paper.mapper.ExamPaperMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成绩分析服务单元测试（Mockito，不依赖 MySQL/Redis）
 * <p>
 * 覆盖：权限与参数钳制、统计口径与零值安全、及格率计算、is_passed 回填时机、
 * CSV 的 BOM/表头/转义/状态映射、全量导出与文件名清理。
 */
@ExtendWith(MockitoExtension.class)
class AnalysisServiceImplTest {

    private static final Long EXAM_ID = 10L;
    private static final Long PAPER_ID = 5L;
    private static final Long TEACHER_ID = 1L;

    @Mock
    private AnalysisMapper analysisMapper;

    @Mock
    private ExamMapper examMapper;

    @Mock
    private ExamUserMapper examUserMapper;

    @Mock
    private ExamPaperMapper examPaperMapper;

    @InjectMocks
    private AnalysisServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        // getExamStats 构造 LambdaQueryWrapper 解析列需要 MP 的 TableInfo 缓存，
        // 纯单测没有 Spring 容器注册 Mapper，手动初始化一次
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExamUser.class);
    }

    @BeforeEach
    void setUp() {
        UserContext.set(new LoginUser(TEACHER_ID, "teacher01", "王老师"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // ==================== 参数与统计 ====================

    @Test
    @DisplayName("统计：考试不存在应报错")
    void stats_examNotFound_throws() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.getExamStats(EXAM_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("考试不存在");
    }

    @Test
    @DisplayName("明细：pageSize 钳制到 100，空关键词转 null")
    void scores_pageSizeClamped() {
        stubTeacherAndExam();
        stubEmptyScorePage();

        ScoreQueryDTO dto = new ScoreQueryDTO();
        dto.setPageSize(500);
        dto.setKeyword("  ");
        service.getScorePage(EXAM_ID, dto);

        ArgumentCaptor<Page<ScoreItemVO>> captor = ArgumentCaptor.forClass(Page.class);
        verify(analysisMapper).selectScorePage(captor.capture(), eq(EXAM_ID), isNull());
        assertThat(captor.getValue().getSize()).isEqualTo(100);
    }

    // ==================== 统计口径与计算 ====================

    @Test
    @DisplayName("统计：聚合人数/及格率/分数段组装，缺失段补 0")
    void stats_aggregatesAndSegments() {
        stubTeacherAndExam();
        when(examPaperMapper.selectById(PAPER_ID)).thenReturn(buildPaper());
        ExamStatsVO summary = new ExamStatsVO();
        summary.setAttendedCount(3L);
        summary.setPassedCount(2L);
        summary.setAvgScore(new BigDecimal("82.50"));
        summary.setMaxScore(new BigDecimal("95.00"));
        summary.setMinScore(new BigDecimal("60.00"));
        when(analysisMapper.selectScoreSummary(EXAM_ID)).thenReturn(summary);
        when(analysisMapper.selectScoreSegments(EXAM_ID)).thenReturn(List.of(
                buildSegment(1, 1L), buildSegment(2, 1L), buildSegment(4, 1L)));
        when(examUserMapper.selectCount(any(Wrapper.class))).thenReturn(4L);

        ExamStatsVO vo = service.getExamStats(EXAM_ID);

        assertThat(vo.getAssignedCount()).isEqualTo(4L);
        assertThat(vo.getAttendedCount()).isEqualTo(3L);
        assertThat(vo.getAbsentCount()).isEqualTo(1L);
        assertThat(vo.getPassedCount()).isEqualTo(2L);
        assertThat(vo.getPassRate()).isEqualByComparingTo("66.67");
        assertThat(vo.getAvgScore()).isEqualByComparingTo("82.50");
        assertThat(vo.getScoreSegments()).hasSize(5);
        assertThat(vo.getScoreSegments()).extracting(ScoreSegmentVO::getLevel)
                .containsExactly(1, 2, 3, 4, 5);
        assertThat(vo.getScoreSegments()).extracting(ScoreSegmentVO::getStudentCount)
                .containsExactly(1L, 1L, 0L, 1L, 0L);
        assertThat(vo.getScoreSegments().get(0).getLabel()).isEqualTo("90%及以上");
        assertThat(vo.getScoreSegments().get(4).getLabel()).isEqualTo("60%以下");
    }

    @Test
    @DisplayName("统计：无人交卷时零值安全，不抛除零异常")
    void stats_noAttendee_zeroSafe() {
        stubTeacherAndExam();
        when(examPaperMapper.selectById(PAPER_ID)).thenReturn(buildPaper());
        // 模拟聚合空行：COUNT=0，其余聚合列为 NULL
        ExamStatsVO summary = new ExamStatsVO();
        summary.setAttendedCount(0L);
        when(analysisMapper.selectScoreSummary(EXAM_ID)).thenReturn(summary);
        when(analysisMapper.selectScoreSegments(EXAM_ID)).thenReturn(List.of());
        when(examUserMapper.selectCount(any(Wrapper.class))).thenReturn(5L);

        ExamStatsVO vo = service.getExamStats(EXAM_ID);

        assertThat(vo.getAttendedCount()).isEqualTo(0L);
        assertThat(vo.getAbsentCount()).isEqualTo(5L);
        assertThat(vo.getPassRate()).isEqualByComparingTo("0");
        assertThat(vo.getAvgScore()).isNull();
        assertThat(vo.getMaxScore()).isNull();
        assertThat(vo.getMinScore()).isNull();
    }

    @Test
    @DisplayName("统计：及格率 HALF_UP 保留 2 位（1/3 → 33.33）")
    void stats_passRateRounding() {
        stubTeacherAndExam();
        when(examPaperMapper.selectById(PAPER_ID)).thenReturn(buildPaper());
        ExamStatsVO summary = new ExamStatsVO();
        summary.setAttendedCount(3L);
        summary.setPassedCount(1L);
        when(analysisMapper.selectScoreSummary(EXAM_ID)).thenReturn(summary);
        when(analysisMapper.selectScoreSegments(EXAM_ID)).thenReturn(List.of());
        when(examUserMapper.selectCount(any(Wrapper.class))).thenReturn(3L);

        assertThat(service.getExamStats(EXAM_ID).getPassRate()).isEqualByComparingTo("33.33");
    }

    @Test
    @DisplayName("统计：is_passed 回填先于统计查询执行")
    void stats_fillIsPassedCalledFirst() {
        stubTeacherAndExam();
        when(examPaperMapper.selectById(PAPER_ID)).thenReturn(buildPaper());
        ExamStatsVO summary = new ExamStatsVO();
        summary.setAttendedCount(1L);
        summary.setPassedCount(1L);
        when(analysisMapper.selectScoreSummary(EXAM_ID)).thenReturn(summary);
        when(analysisMapper.selectScoreSegments(EXAM_ID)).thenReturn(List.of());
        when(examUserMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        service.getExamStats(EXAM_ID);

        InOrder inOrder = inOrder(analysisMapper);
        inOrder.verify(analysisMapper).fillIsPassed(EXAM_ID);
        inOrder.verify(analysisMapper).selectScoreSummary(EXAM_ID);
        inOrder.verify(analysisMapper).selectScoreSegments(EXAM_ID);
    }

    // ==================== 成绩明细 ====================

    @Test
    @DisplayName("明细：未考考生（答卷字段全 null）原样透传不报错")
    void scores_unsubmittedRowKept() {
        stubTeacherAndExam();
        ScoreItemVO row = buildItem("S01", "张三", null, null, null, null, null, null);
        when(analysisMapper.selectScorePage(any(), eq(EXAM_ID), isNull()))
                .thenAnswer(invocation -> pageOf(List.of(row)));

        PageVO<ScoreItemVO> vo = service.getScorePage(EXAM_ID, new ScoreQueryDTO());

        assertThat(vo.getList()).hasSize(1);
        assertThat(vo.getList().get(0).getSheetStatus()).isNull();
        assertThat(vo.getList().get(0).getTotalScore()).isNull();
    }

    @Test
    @DisplayName("明细：is_passed 回填先于分页查询执行")
    void scores_fillIsPassedCalled() {
        stubTeacherAndExam();
        stubEmptyScorePage();

        service.getScorePage(EXAM_ID, new ScoreQueryDTO());

        InOrder inOrder = inOrder(analysisMapper);
        inOrder.verify(analysisMapper).fillIsPassed(EXAM_ID);
        inOrder.verify(analysisMapper).selectScorePage(any(), eq(EXAM_ID), isNull());
    }

    // ==================== CSV 导出 ====================

    @Test
    @DisplayName("导出：UTF-8 BOM 三字节开头 + 表头行 + CRLF 行尾")
    void export_bomAndHeader() {
        stubTeacherAndExam();
        stubExportPage(List.of(buildItem("S01", "张三", 2,
                LocalDateTime.of(2026, 1, 1, 10, 0, 0),
                new BigDecimal("80.00"), new BigDecimal("80.00"), 1, 1)));

        byte[] content = service.exportScores(EXAM_ID, null).getContent();

        assertThat(content[0]).isEqualTo((byte) 0xEF);
        assertThat(content[1]).isEqualTo((byte) 0xBB);
        assertThat(content[2]).isEqualTo((byte) 0xBF);
        String text = new String(content, StandardCharsets.UTF_8);
        assertThat(text).startsWith("﻿学号,姓名,状态,交卷时间,客观分,总分,是否及格,切屏次数\r\n");
        assertThat(text).endsWith("\r\n");
    }

    @Test
    @DisplayName("导出：状态/及格文本映射，未考行数值列全为空单元格")
    void export_statusAndPassTextMapping() {
        stubTeacherAndExam();
        stubExportPage(List.of(
                buildItem("S01", "张三", 2, LocalDateTime.of(2026, 1, 1, 10, 0, 0),
                        new BigDecimal("80.00"), new BigDecimal("80.00"), 1, 1),
                buildItem("S02", "李四", 3, null, new BigDecimal("50.00"), new BigDecimal("50.00"), 0, 0),
                buildItem("S03", "王五", 4, null, new BigDecimal("90.00"), new BigDecimal("90.00"), 1, 2),
                buildItem("S04", "赵六", null, null, null, null, null, null)));

        String text = csvText();

        String[] lines = text.split("\r\n");
        assertThat(lines).hasSize(5);
        assertThat(lines[1]).isEqualTo("S01,张三,已交卷,2026-01-01 10:00:00,80.00,80.00,是,1");
        assertThat(lines[2]).isEqualTo("S02,李四,强制交卷,,50.00,50.00,否,0");
        assertThat(lines[3]).isEqualTo("S03,王五,超时自动交卷,,90.00,90.00,是,2");
        assertThat(lines[4]).isEqualTo("S04,赵六,未考,,,,,");
    }

    @Test
    @DisplayName("导出：含逗号/引号/换行的姓名按 RFC 4180 转义")
    void export_csvEscaping() {
        stubTeacherAndExam();
        stubExportPage(List.of(
                buildItem("S01", "张,三", 2, null, null, null, 1, 0),
                buildItem("S02", "张\"三", 2, null, null, null, 1, 0),
                buildItem("S03", "张\n三", 2, null, null, null, 1, 0),
                buildItem("S04", "张三", 2, null, null, null, 1, 0)));

        String[] lines = csvText().split("\r\n");

        assertThat(lines[1]).isEqualTo("S01,\"张,三\",已交卷,,,,是,0");
        assertThat(lines[2]).isEqualTo("S02,\"张\"\"三\",已交卷,,,,是,0");
        assertThat(lines[3]).isEqualTo("S03,\"张\n三\",已交卷,,,,是,0");
        assertThat(lines[4]).isEqualTo("S04,张三,已交卷,,,,是,0");
    }

    @Test
    @DisplayName("导出：使用 Page(1,-1) 全量查询，不拼 LIMIT")
    void export_unpagedQuery() {
        stubTeacherAndExam();
        stubExportPage(List.of());

        service.exportScores(EXAM_ID, null);

        ArgumentCaptor<Page<ScoreItemVO>> captor = ArgumentCaptor.forClass(Page.class);
        verify(analysisMapper).selectScorePage(captor.capture(), eq(EXAM_ID), isNull());
        assertThat(captor.getValue().getCurrent()).isEqualTo(1);
        assertThat(captor.getValue().getSize()).isEqualTo(-1);
    }

    @Test
    @DisplayName("导出：文件名清理非法字符，考试名为空时兜底")
    void export_fileNameSanitized() {
        Exam exam = buildExam();
        exam.setName("期中/考试:2024*");
        when(examMapper.selectById(EXAM_ID)).thenReturn(exam);
        Exam examNoName = buildExam();
        examNoName.setId(11L);
        examNoName.setName(null);
        when(examMapper.selectById(11L)).thenReturn(examNoName);
        // 两个考试ID都要能走通导出查询
        when(analysisMapper.selectScorePage(any(), any(), isNull()))
                .thenAnswer(invocation -> pageOf(List.of()));

        String fileName = service.exportScores(EXAM_ID, null).getFileName();
        String nullNameFile = service.exportScores(11L, null).getFileName();

        assertThat(fileName).isEqualTo("成绩明细_期中_考试_2024_.csv");
        assertThat(nullNameFile).isEqualTo("成绩明细_导出.csv");
    }

    // ==================== 测试数据 ====================

    private void stubTeacherAndExam() {
        when(examMapper.selectById(EXAM_ID)).thenReturn(buildExam());
    }

    private void stubEmptyScorePage() {
        when(analysisMapper.selectScorePage(any(), eq(EXAM_ID), isNull()))
                .thenAnswer(invocation -> pageOf(List.of()));
    }

    private void stubExportPage(List<ScoreItemVO> rows) {
        when(analysisMapper.selectScorePage(any(), eq(EXAM_ID), isNull()))
                .thenAnswer(invocation -> pageOf(rows));
    }

    private IPage<ScoreItemVO> pageOf(List<ScoreItemVO> rows) {
        Page<ScoreItemVO> page = new Page<>(1, 10);
        page.setRecords(rows);
        page.setTotal(rows.size());
        return page;
    }

    private Exam buildExam() {
        Exam exam = new Exam();
        exam.setId(EXAM_ID);
        exam.setPaperId(PAPER_ID);
        exam.setName("Java 期中考试");
        return exam;
    }

    private ExamPaper buildPaper() {
        ExamPaper paper = new ExamPaper();
        paper.setId(PAPER_ID);
        paper.setTotalScore(new BigDecimal("100.00"));
        paper.setPassScore(new BigDecimal("60.00"));
        return paper;
    }

    private ScoreSegmentVO buildSegment(int level, long count) {
        return new ScoreSegmentVO(level, null, count);
    }

    private ScoreItemVO buildItem(String userNo, String realName, Integer sheetStatus,
                                  LocalDateTime submitTime, BigDecimal objectiveScore,
                                  BigDecimal totalScore, Integer isPassed, Integer switchCount) {
        ScoreItemVO item = new ScoreItemVO();
        item.setUserId(100L);
        item.setUserNo(userNo);
        item.setRealName(realName);
        item.setSheetStatus(sheetStatus);
        item.setSubmitTime(submitTime);
        item.setObjectiveScore(objectiveScore);
        item.setTotalScore(totalScore);
        item.setIsPassed(isPassed);
        item.setScreenSwitchCount(switchCount);
        return item;
    }

    private String csvText() {
        byte[] content = service.exportScores(EXAM_ID, null).getContent();
        return new String(content, StandardCharsets.UTF_8);
    }
}
