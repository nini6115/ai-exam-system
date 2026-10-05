package com.aiexam.question.controller;

import com.aiexam.common.AjaxResult;
import com.aiexam.common.vo.PageVO;
import com.aiexam.question.dto.QuestionAddDTO;
import com.aiexam.question.dto.QuestionQueryDTO;
import com.aiexam.question.dto.QuestionUpdateDTO;
import com.aiexam.question.service.QuestionService;
import com.aiexam.question.vo.QuestionVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题目控制器
 */
@RestController
@RequestMapping("/question")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    /**
     * 分页查询题目列表
     */
    @GetMapping("/list")
    public AjaxResult<PageVO<QuestionVO>> list(QuestionQueryDTO dto) {
        return AjaxResult.success(questionService.listQuestions(dto));
    }

    /**
     * 查询题目分类（去重）
     */
    @GetMapping("/category")
    public AjaxResult<List<String>> category() {
        return AjaxResult.success(questionService.listCategories());
    }

    /**
     * 查询题目详情
     */
    @GetMapping("/{id}")
    public AjaxResult<QuestionVO> detail(@PathVariable Long id) {
        return AjaxResult.success(questionService.getQuestionDetail(id));
    }

    /**
     * 新增题目
     */
    @PostMapping
    public AjaxResult<Long> add(@Valid @RequestBody QuestionAddDTO dto) {
        return AjaxResult.success(questionService.addQuestion(dto));
    }

    /**
     * 修改题目
     */
    @PutMapping
    public AjaxResult<Void> update(@Valid @RequestBody QuestionUpdateDTO dto) {
        questionService.updateQuestion(dto);
        return AjaxResult.success();
    }

    /**
     * 删除题目（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public AjaxResult<Void> delete(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return AjaxResult.success();
    }
}
