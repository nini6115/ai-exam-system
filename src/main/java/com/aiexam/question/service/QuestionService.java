package com.aiexam.question.service;

import com.aiexam.common.vo.PageVO;
import com.aiexam.question.dto.QuestionAddDTO;
import com.aiexam.question.dto.QuestionQueryDTO;
import com.aiexam.question.dto.QuestionUpdateDTO;
import com.aiexam.question.entity.Question;
import com.aiexam.question.vo.QuestionVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 题目服务接口
 */
public interface QuestionService extends IService<Question> {

    /** 分页查询题目列表 */
    PageVO<QuestionVO> listQuestions(QuestionQueryDTO dto);

    /** 查询题目详情 */
    QuestionVO getQuestionDetail(Long id);

    /** 新增题目 @return 新题目ID */
    Long addQuestion(QuestionAddDTO dto);

    /** 修改题目 */
    void updateQuestion(QuestionUpdateDTO dto);

    /** 删除题目（逻辑删除） */
    void deleteQuestion(Long id);

    /** 查询题目分类（去重） */
    List<String> listCategories();
}
