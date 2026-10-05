package com.aiexam.exam.controller;

import com.aiexam.common.AjaxResult;
import com.aiexam.common.utils.IpUtil;
import com.aiexam.common.vo.PageVO;
import com.aiexam.exam.dto.AnswerSaveDTO;
import com.aiexam.exam.dto.ExamPublishDTO;
import com.aiexam.exam.dto.ExamQueryDTO;
import com.aiexam.exam.service.AnswerSheetService;
import com.aiexam.exam.service.ExamService;
import com.aiexam.exam.vo.ExamDetailVO;
import com.aiexam.exam.vo.ExamStartVO;
import com.aiexam.exam.vo.ExamVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 考试控制器
 */
@RestController
@RequestMapping("/exam")
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private AnswerSheetService answerSheetService;

    /**
     * 发布考试：选择试卷、设置时间/时长/防作弊参数、指定考生
     */
    @PostMapping("/publish")
    public AjaxResult<Long> publish(@Valid @RequestBody ExamPublishDTO dto) {
        return AjaxResult.success(examService.publish(dto));
    }

    /**
     * 分页查询考试列表（教师端）
     */
    @GetMapping("/list")
    public AjaxResult<PageVO<ExamVO>> list(ExamQueryDTO dto) {
        return AjaxResult.success(examService.listExams(dto));
    }

    /**
     * 考试详情（含试卷摘要与学生列表）
     */
    @GetMapping("/{id}")
    public AjaxResult<ExamDetailVO> detail(@PathVariable Long id) {
        return AjaxResult.success(examService.getExamDetail(id));
    }

    /**
     * 学生进入考试：校验名单与时间、创建答卷、返回乱序题目（不含答案）
     */
    @PostMapping("/{id}/start")
    public AjaxResult<ExamStartVO> start(@PathVariable Long id, HttpServletRequest request) {
        return AjaxResult.success(answerSheetService.start(id,
                IpUtil.getIp(request), request.getHeader("User-Agent")));
    }

    /**
     * 定时保存答案（草稿态，只写 Redis）
     */
    @PostMapping("/{id}/save")
    public AjaxResult<Void> save(@PathVariable Long id, @Valid @RequestBody AnswerSaveDTO dto) {
        answerSheetService.saveAnswer(id, dto);
        return AjaxResult.success();
    }

    /**
     * 学生交卷：草稿落库 + 客观题自动判分 + 清空 Redis 草稿
     */
    @PostMapping("/{id}/submit")
    public AjaxResult<Void> submit(@PathVariable Long id) {
        answerSheetService.submit(id);
        return AjaxResult.success();
    }
}
