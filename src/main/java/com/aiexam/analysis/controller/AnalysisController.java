package com.aiexam.analysis.controller;

import com.aiexam.analysis.dto.ScoreQueryDTO;
import com.aiexam.analysis.service.AnalysisService;
import com.aiexam.analysis.vo.ExamStatsVO;
import com.aiexam.analysis.vo.ScoreExportVO;
import com.aiexam.analysis.vo.ScoreItemVO;
import com.aiexam.common.AjaxResult;
import com.aiexam.common.annotation.OperationLog;
import com.aiexam.common.vo.PageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 成绩分析控制器（教师端：统计概览 / 成绩明细 / CSV 导出）
 */
@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

    /**
     * 考试统计概览：参考/缺考人数、均分极值、及格率、分数段分布
     */
    @GetMapping("/{examId}/stats")
    public AjaxResult<ExamStatsVO> stats(@PathVariable Long examId) {
        return AjaxResult.success(analysisService.getExamStats(examId));
    }

    /**
     * 成绩明细分页：本场考试全名单（未考也返回），支持姓名学号搜索
     */
    @GetMapping("/{examId}/scores")
    public AjaxResult<PageVO<ScoreItemVO>> scores(@PathVariable Long examId, ScoreQueryDTO dto) {
        return AjaxResult.success(analysisService.getScorePage(examId, dto));
    }

    /**
     * 成绩明细 CSV 导出（与明细同一条查询，不分页全量导出）
     * <p>
     * 文件下载例外：不走 AjaxResult 统一包装，直接返回字节流；
     * CSV 带 UTF-8 BOM，Excel 双击打开中文不乱码；文件名按 RFC 5987 编码支持中文。
     */
    @GetMapping("/{examId}/export")
    @OperationLog(module = "成绩分析", action = "导出成绩明细")
    public ResponseEntity<byte[]> export(@PathVariable Long examId,
                                         @RequestParam(required = false) String keyword) {
        ScoreExportVO vo = analysisService.exportScores(examId, keyword);
        // URLEncoder 把空格编码成 +，HTTP 头约定应为 %20，需修正
        String encoded = URLEncoder.encode(vo.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(vo.getContent());
    }
}
