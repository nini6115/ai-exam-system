package com.aiexam.ai.controller;

import com.aiexam.ai.dto.ManualGradeDTO;
import com.aiexam.ai.service.AiGradingService;
import com.aiexam.ai.vo.GradeBatchResultVO;
import com.aiexam.ai.vo.GradeDetailVO;
import com.aiexam.ai.vo.SheetScoreSummaryVO;
import com.aiexam.common.AjaxResult;
import com.aiexam.common.annotation.OperationLog;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 判卷控制器（教师端：批量 AI 判卷 / 主观题复核 / 人工改分）
 */
@RestController
@RequestMapping("/ai")
public class AiGradingController {

    @Autowired
    private AiGradingService aiGradingService;

    /**
     * 触发整场考试批量 AI 判卷（主观题串行逐题判分，失败逐题返回，可重复触发重试）
     */
    @PostMapping("/exams/{examId}/grade")
    @OperationLog(module = "AI判卷", action = "批量AI判卷")
    public AjaxResult<GradeBatchResultVO> grade(@PathVariable Long examId) {
        return AjaxResult.success(aiGradingService.gradeExam(examId));
    }

    /**
     * 答卷复核视图：逐题判分详情（含 AI/人工/系统判分与评语）
     */
    @GetMapping("/exams/{examId}/sheets/{sheetId}/details")
    public AjaxResult<List<GradeDetailVO>> sheetDetails(@PathVariable Long examId,
                                                        @PathVariable Long sheetId) {
        return AjaxResult.success(aiGradingService.getSheetGradeDetails(examId, sheetId));
    }

    /**
     * 人工改分一道主观题（graded_by=2），返回该卷最新成绩汇总
     */
    @PutMapping("/details/{detailId}")
    @OperationLog(module = "AI判卷", action = "人工改分")
    public AjaxResult<SheetScoreSummaryVO> manualGrade(@PathVariable Long detailId,
                                                       @Valid @RequestBody ManualGradeDTO dto) {
        return AjaxResult.success(aiGradingService.manualGrade(detailId, dto));
    }
}
