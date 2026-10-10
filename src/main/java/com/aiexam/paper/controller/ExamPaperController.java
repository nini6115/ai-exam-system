package com.aiexam.paper.controller;

import com.aiexam.common.AjaxResult;
import com.aiexam.common.annotation.OperationLog;
import com.aiexam.common.vo.PageVO;
import com.aiexam.paper.dto.PaperCreateDTO;
import com.aiexam.paper.dto.PaperGenerateDTO;
import com.aiexam.paper.dto.PaperQueryDTO;
import com.aiexam.paper.service.ExamPaperService;
import com.aiexam.paper.vo.PaperDetailVO;
import com.aiexam.paper.vo.PaperVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 试卷控制器
 */
@RestController
@RequestMapping("/paper")
public class ExamPaperController {

    @Autowired
    private ExamPaperService examPaperService;

    /**
     * 智能组卷：按题型/难度/分类随机抽题生成试卷
     */
    @PostMapping("/generate")
    @OperationLog(module = "试卷管理", action = "智能组卷")
    public AjaxResult<Long> generate(@Valid @RequestBody PaperGenerateDTO dto) {
        return AjaxResult.success(examPaperService.generatePaper(dto));
    }

    /**
     * 手动组卷：按题目ID列表直接组卷
     */
    @PostMapping("/create")
    @OperationLog(module = "试卷管理", action = "手动组卷")
    public AjaxResult<Long> create(@Valid @RequestBody PaperCreateDTO dto) {
        return AjaxResult.success(examPaperService.createPaper(dto));
    }

    /**
     * 分页查询试卷列表
     */
    @GetMapping("/list")
    public AjaxResult<PageVO<PaperVO>> list(PaperQueryDTO dto) {
        return AjaxResult.success(examPaperService.listPapers(dto));
    }

    /**
     * 试卷详情（基本信息 + 题目列表，不含答案）
     */
    @GetMapping("/{id}")
    public AjaxResult<PaperDetailVO> detail(@PathVariable Long id) {
        return AjaxResult.success(examPaperService.getPaperDetail(id));
    }
}
