package com.aiexam.analysis.service.impl;

import com.aiexam.analysis.dto.ScoreQueryDTO;
import com.aiexam.analysis.mapper.AnalysisMapper;
import com.aiexam.analysis.service.AnalysisService;
import com.aiexam.analysis.vo.ExamStatsVO;
import com.aiexam.analysis.vo.ScoreExportVO;
import com.aiexam.analysis.vo.ScoreItemVO;
import com.aiexam.analysis.vo.ScoreSegmentVO;
import com.aiexam.common.context.UserContext;
import com.aiexam.common.vo.PageVO;
import com.aiexam.exam.entity.Exam;
import com.aiexam.exam.entity.ExamUser;
import com.aiexam.exam.mapper.ExamMapper;
import com.aiexam.exam.mapper.ExamUserMapper;
import com.aiexam.paper.entity.ExamPaper;
import com.aiexam.paper.mapper.ExamPaperMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成绩分析服务实现（考试统计 / 成绩明细 / CSV 导出）
 * <p>
 * 无对应实体，不继承 ServiceImpl；统计口径见 AnalysisMapper.xml 的公共子查询注释。
 */
@Slf4j
@Service
public class AnalysisServiceImpl implements AnalysisService {

    /** 每页最大条数 */
    private static final int MAX_PAGE_SIZE = 100;
    /** 答卷状态（仅已交卷态参与统计）：2已交卷 3强制交卷 4超时自动交卷 */
    private static final int STATUS_SUBMITTED = 2;
    private static final int STATUS_FORCED = 3;
    private static final int STATUS_TIMEOUT = 4;
    /** 分数段名称，下标+1 即段位 level */
    private static final String[] SEGMENT_LABELS = {"90%及以上", "80%~89%", "70%~79%", "60%~69%", "60%以下"};
    /** 及格率百分数因子与保留位数 */
    private static final BigDecimal RATE_MULTIPLIER = new BigDecimal(100);
    private static final int RATE_SCALE = 2;
    /** CSV 单元格：UTF-8 BOM（Excel 识别中文编码用）、表头、CRLF 行尾（Excel 友好） */
    private static final char CSV_BOM = '﻿';
    private static final String CSV_HEADER = "学号,姓名,状态,交卷时间,客观分,总分,是否及格,切屏次数";
    private static final String CSV_ROW_END = "\r\n";
    /** CSV 交卷时间格式（与 API 的 @JsonFormat 一致） */
    private static final DateTimeFormatter CSV_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 导出文件名非法字符（Windows 文件名保留字符 + 换行） */
    private static final String FILE_NAME_INVALID_CHARS = "[\\\\/:*?\"<>|\r\n]";

    @Autowired
    private AnalysisMapper analysisMapper;

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private ExamUserMapper examUserMapper;

    @Autowired
    private ExamPaperMapper examPaperMapper;

    // ==================== 统计概览 ====================

    @Override
    public ExamStatsVO getExamStats(Long examId) {
        Exam exam = requireExam(examId);
        // 惰性回填 is_passed（AI 判卷更新总分后再次查询自动重算，见 XML 注释）
        analysisMapper.fillIsPassed(examId);
        ExamPaper paper = examPaperMapper.selectById(exam.getPaperId());

        // 聚合行：无考生交卷时 COUNT=0、其余列为 NULL，统一兜底
        ExamStatsVO vo = analysisMapper.selectScoreSummary(examId);
        if (vo == null) {
            vo = new ExamStatsVO();
        }
        long attended = vo.getAttendedCount() == null ? 0L : vo.getAttendedCount();
        long passed = vo.getPassedCount() == null ? 0L : vo.getPassedCount();

        Long assigned = examUserMapper.selectCount(new LambdaQueryWrapper<ExamUser>()
                .eq(ExamUser::getExamId, examId));

        vo.setExamId(exam.getId());
        vo.setExamName(exam.getName());
        vo.setTotalScore(paper != null ? paper.getTotalScore() : null);
        vo.setPassScore(paper != null ? paper.getPassScore() : null);
        vo.setAssignedCount(assigned == null ? 0L : assigned);
        vo.setAttendedCount(attended);
        vo.setAbsentCount(vo.getAssignedCount() - attended);
        vo.setPassedCount(passed);
        // 均分统一 2 位小数；极值保留数据库原值
        if (vo.getAvgScore() != null) {
            vo.setAvgScore(vo.getAvgScore().setScale(RATE_SCALE, RoundingMode.HALF_UP));
        }
        // 及格率 = 及格人数 * 100 / 参考人数，2 位小数（85.50 表示 85.5%）；无人交卷为 0 防除零
        vo.setPassRate(attended == 0 ? BigDecimal.ZERO
                : new BigDecimal(passed).multiply(RATE_MULTIPLIER)
                .divide(new BigDecimal(attended), RATE_SCALE, RoundingMode.HALF_UP));
        vo.setScoreSegments(buildSegments(examId));
        return vo;
    }

    /**
     * 组装恒定 5 段的分数段分布：SQL 只返回非空段，缺失段补 0，前端无需判空
     */
    private List<ScoreSegmentVO> buildSegments(Long examId) {
        Map<Integer, Long> countByLevel = new HashMap<>();
        for (ScoreSegmentVO row : analysisMapper.selectScoreSegments(examId)) {
            countByLevel.put(row.getLevel(), row.getStudentCount() == null ? 0L : row.getStudentCount());
        }
        List<ScoreSegmentVO> segments = new ArrayList<>(SEGMENT_LABELS.length);
        for (int level = 1; level <= SEGMENT_LABELS.length; level++) {
            segments.add(new ScoreSegmentVO(level, SEGMENT_LABELS[level - 1],
                    countByLevel.getOrDefault(level, 0L)));
        }
        return segments;
    }

    // ==================== 成绩明细 ====================

    @Override
    public PageVO<ScoreItemVO> getScorePage(Long examId, ScoreQueryDTO dto) {
        requireExam(examId);
        analysisMapper.fillIsPassed(examId);
        Page<ScoreItemVO> page = new Page<>(dto.getPageNum(),
                Math.min(dto.getPageSize(), MAX_PAGE_SIZE));
        IPage<ScoreItemVO> result = analysisMapper.selectScorePage(page, examId, normalizeKeyword(dto.getKeyword()));
        return PageVO.of(page, result.getRecords());
    }

    // ==================== CSV 导出 ====================

    @Override
    public ScoreExportVO exportScores(Long examId, String keyword) {
        Exam exam = requireExam(examId);
        analysisMapper.fillIsPassed(examId);
        // Page size=-1：MP 分页插件约定 size<0 不拼 LIMIT 且不执行 count，同一条 SQL 全量导出
        List<ScoreItemVO> list = analysisMapper
                .selectScorePage(new Page<>(1, -1), examId, normalizeKeyword(keyword)).getRecords();

        log.info("用户[{}]导出考试[{}]成绩明细，共{}条", UserContext.getUsername(), examId, list.size());
        return new ScoreExportVO("成绩明细_" + sanitizeFileName(exam.getName()) + ".csv", buildCsv(list));
    }

    /**
     * 生成 CSV 内容：UTF-8 BOM 开头（Excel 识别编码），CRLF 行尾，文本列过 RFC 4180 转义
     */
    private byte[] buildCsv(List<ScoreItemVO> list) {
        StringBuilder sb = new StringBuilder();
        sb.append(CSV_BOM).append(CSV_HEADER).append(CSV_ROW_END);
        for (ScoreItemVO item : list) {
            sb.append(escapeCsv(item.getUserNo())).append(',')
                    .append(escapeCsv(item.getRealName())).append(',')
                    .append(escapeCsv(statusText(item.getSheetStatus()))).append(',')
                    .append(item.getSubmitTime() == null ? "" : CSV_TIME_FORMATTER.format(item.getSubmitTime()))
                    .append(',')
                    .append(item.getObjectiveScore() == null ? "" : item.getObjectiveScore().toPlainString())
                    .append(',')
                    .append(item.getTotalScore() == null ? "" : item.getTotalScore().toPlainString())
                    .append(',')
                    .append(passText(item.getIsPassed())).append(',')
                    .append(item.getScreenSwitchCount() == null ? "" : item.getScreenSwitchCount())
                    .append(CSV_ROW_END);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 答卷状态文本映射
     */
    private String statusText(Integer sheetStatus) {
        if (sheetStatus == null) {
            return "未考";
        }
        return switch (sheetStatus) {
            case STATUS_SUBMITTED -> "已交卷";
            case STATUS_FORCED -> "强制交卷";
            case STATUS_TIMEOUT -> "超时自动交卷";
            default -> String.valueOf(sheetStatus);
        };
    }

    /**
     * 是否及格文本映射
     */
    private String passText(Integer isPassed) {
        if (isPassed == null) {
            return "";
        }
        return isPassed == 1 ? "是" : "否";
    }

    /**
     * CSV 单元格转义（RFC 4180）：含逗号/引号/换行时整体加双引号，内部引号加倍
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * 导出文件名清理：考试名去掉 Windows 文件名非法字符，空值兜底
     */
    private String sanitizeFileName(String examName) {
        if (examName == null || examName.isBlank()) {
            return "导出";
        }
        return examName.replaceAll(FILE_NAME_INVALID_CHARS, "_");
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
     * 关键词规范化：空白转 null，避免 LIKE '%%' 全模糊
     */
    private String normalizeKeyword(String keyword) {
        return (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    }
}
