package com.aiexam.paper.service;

import com.aiexam.common.vo.PageVO;
import com.aiexam.paper.dto.PaperCreateDTO;
import com.aiexam.paper.dto.PaperGenerateDTO;
import com.aiexam.paper.dto.PaperQueryDTO;
import com.aiexam.paper.entity.ExamPaper;
import com.aiexam.paper.vo.PaperDetailVO;
import com.aiexam.paper.vo.PaperVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 试卷服务
 */
public interface ExamPaperService extends IService<ExamPaper> {

    /**
     * 智能组卷：按抽题配置从题库随机抽题，生成试卷
     *
     * @return 新试卷ID
     */
    Long generatePaper(PaperGenerateDTO dto);

    /**
     * 手动组卷：按题目ID列表直接组卷
     *
     * @return 新试卷ID
     */
    Long createPaper(PaperCreateDTO dto);

    /**
     * 分页查询试卷列表
     */
    PageVO<PaperVO> listPapers(PaperQueryDTO dto);

    /**
     * 试卷详情（基本信息 + 题目列表，不含答案）
     */
    PaperDetailVO getPaperDetail(Long id);
}
