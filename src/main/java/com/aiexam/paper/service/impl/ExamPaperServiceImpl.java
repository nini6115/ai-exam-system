package com.aiexam.paper.service.impl;

import com.aiexam.common.context.UserContext;
import com.aiexam.common.vo.PageVO;
import com.aiexam.paper.dto.PaperCreateDTO;
import com.aiexam.paper.dto.PaperGenerateDTO;
import com.aiexam.paper.dto.PaperQueryDTO;
import com.aiexam.paper.dto.QuestionConfigDTO;
import com.aiexam.paper.entity.ExamPaper;
import com.aiexam.paper.entity.ExamPaperQuestion;
import com.aiexam.paper.mapper.ExamPaperMapper;
import com.aiexam.paper.mapper.ExamPaperQuestionMapper;
import com.aiexam.paper.service.ExamPaperService;
import com.aiexam.paper.vo.PaperDetailVO;
import com.aiexam.paper.vo.PaperQuestionVO;
import com.aiexam.paper.vo.PaperVO;
import com.aiexam.question.entity.Question;
import com.aiexam.question.mapper.QuestionMapper;
import com.aiexam.system.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * 试卷服务实现
 */
@Slf4j
@Service
public class ExamPaperServiceImpl extends ServiceImpl<ExamPaperMapper, ExamPaper> implements ExamPaperService {

    /** 每页最大条数 */
    private static final int MAX_PAGE_SIZE = 100;
    /** 管理员角色编码 */
    private static final String ADMIN_ROLE_CODE = "admin";
    /** 教师角色编码 */
    private static final String TEACHER_ROLE_CODE = "teacher";
    /** 默认及格线：总分的60% */
    private static final BigDecimal DEFAULT_PASS_RATE = new BigDecimal("0.6");
    /** 题型名：1单选 2多选 3判断 4填空 5简答（大题分组名 + 库存不足提示用） */
    private static final Map<Integer, String> TYPE_NAMES =
            Map.of(1, "单选题", 2, "多选题", 3, "判断题", 4, "填空题", 5, "简答题");
    /** 难度名：1简单 2中等 3困难 */
    private static final Map<Integer, String> DIFFICULTY_NAMES =
            Map.of(1, "简单", 2, "中等", 3, "困难");

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ExamPaperQuestionMapper examPaperQuestionMapper;

    // ==================== 智能组卷 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generatePaper(PaperGenerateDTO dto) {
        checkTeacherOrAdmin();

        List<ExamPaperQuestion> paperQuestions = new ArrayList<>();
        // 已选题目ID：多条配置同题型同难度时避免抽到重复题（关联表有唯一键）
        List<Long> selectedIds = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        int sortOrder = 0;

        for (QuestionConfigDTO cfg : dto.getConfig()) {
            // config 未指定分类时用试卷级分类，都为空则不限
            String category = (cfg.getCategory() != null && !cfg.getCategory().isEmpty())
                    ? cfg.getCategory() : dto.getCategory();
            List<Long> ids = questionMapper.selectRandomIds(
                    cfg.getType(), cfg.getDifficulty(), category, selectedIds, cfg.getCount());
            if (ids.size() < cfg.getCount()) {
                throw new RuntimeException(String.format("%s（难度：%s）题目不足，需要%d道，题库只有%d道",
                        TYPE_NAMES.get(cfg.getType()), DIFFICULTY_NAMES.get(cfg.getDifficulty()),
                        cfg.getCount(), ids.size()));
            }
            selectedIds.addAll(ids);
            String section = TYPE_NAMES.get(cfg.getType());
            for (Long questionId : ids) {
                ExamPaperQuestion pq = new ExamPaperQuestion();
                pq.setQuestionId(questionId);
                pq.setQuestionScore(cfg.getScorePerQuestion());
                pq.setSortOrder(++sortOrder);
                pq.setSection(section);
                paperQuestions.add(pq);
            }
            totalScore = totalScore.add(cfg.getScorePerQuestion().multiply(BigDecimal.valueOf(cfg.getCount())));
        }

        // 总分由抽题配置计算，不信任前端传值；及格分默认总分的60%
        ExamPaper paper = new ExamPaper();
        paper.setName(dto.getTitle());
        paper.setDescription(dto.getDescription());
        paper.setTotalScore(totalScore);
        paper.setPassScore(dto.getPassScore() != null
                ? dto.getPassScore()
                : totalScore.multiply(DEFAULT_PASS_RATE).setScale(1, RoundingMode.HALF_UP));
        paper.setDuration(dto.getDuration());
        paper.setQuestionCount(sortOrder);
        paper.setCreatorId(UserContext.getUserId());
        save(paper);

        paperQuestions.forEach(pq -> pq.setPaperId(paper.getId()));
        Db.saveBatch(paperQuestions);

        log.info("用户[{}]智能组卷，试卷ID：{}，共{}题，总分{}", UserContext.getUsername(), paper.getId(), sortOrder, totalScore);
        return paper.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPaper(PaperCreateDTO dto) {
        checkTeacherOrAdmin();

        List<Long> questionIds = dto.getQuestionIds();
        if (new HashSet<>(questionIds).size() != questionIds.size()) {
            throw new RuntimeException("题目ID列表存在重复");
        }
        // selectBatchIds 走 @TableLogic，已删除的题目查不出来，视为不存在
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        if (questions.size() != questionIds.size()) {
            Set<Long> found = questions.stream().map(Question::getId).collect(Collectors.toSet());
            String missing = questionIds.stream().filter(id -> !found.contains(id))
                    .map(String::valueOf).collect(Collectors.joining(","));
            throw new RuntimeException("题目不存在，ID：" + missing);
        }

        // 按题型分组排序（同题型内保持传入顺序），保证 section 连续
        questions.sort(Comparator.comparing(Question::getType));
        BigDecimal totalScore = BigDecimal.ZERO;
        List<ExamPaperQuestion> paperQuestions = new ArrayList<>();
        int sortOrder = 0;
        for (Question q : questions) {
            BigDecimal score = q.getScore() != null ? q.getScore() : BigDecimal.ZERO;
            totalScore = totalScore.add(score);
            ExamPaperQuestion pq = new ExamPaperQuestion();
            pq.setQuestionId(q.getId());
            pq.setQuestionScore(score);
            pq.setSortOrder(++sortOrder);
            pq.setSection(TYPE_NAMES.get(q.getType()));
            paperQuestions.add(pq);
        }

        ExamPaper paper = new ExamPaper();
        paper.setName(dto.getTitle());
        paper.setDescription(dto.getDescription());
        paper.setTotalScore(totalScore);
        paper.setPassScore(dto.getPassScore() != null
                ? dto.getPassScore()
                : totalScore.multiply(DEFAULT_PASS_RATE).setScale(1, RoundingMode.HALF_UP));
        paper.setDuration(dto.getDuration());
        paper.setQuestionCount(sortOrder);
        paper.setCreatorId(UserContext.getUserId());
        save(paper);

        paperQuestions.forEach(pq -> pq.setPaperId(paper.getId()));
        Db.saveBatch(paperQuestions);

        log.info("用户[{}]手动组卷，试卷ID：{}，共{}题，总分{}", UserContext.getUsername(), paper.getId(), sortOrder, totalScore);
        return paper.getId();
    }

    // ==================== 试卷管理 ====================

    @Override
    public PageVO<PaperVO> listPapers(PaperQueryDTO dto) {
        checkTeacherOrAdmin();
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<ExamPaper>()
                .like(dto.getName() != null && !dto.getName().isEmpty(), ExamPaper::getName, dto.getName())
                .orderByDesc(ExamPaper::getId);
        Page<ExamPaper> page = page(new Page<>(dto.getPageNum(),
                Math.min(dto.getPageSize(), MAX_PAGE_SIZE)), wrapper);
        List<PaperVO> vos = page.getRecords().stream().map(PaperVO::from).collect(Collectors.toList());
        return PageVO.of(page, vos);
    }

    @Override
    public PaperDetailVO getPaperDetail(Long id) {
        checkTeacherOrAdmin();
        ExamPaper paper = getById(id);
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }
        PaperDetailVO vo = new PaperDetailVO();
        vo.setId(paper.getId());
        vo.setName(paper.getName());
        vo.setDescription(paper.getDescription());
        vo.setTotalScore(paper.getTotalScore());
        vo.setPassScore(paper.getPassScore());
        vo.setDuration(paper.getDuration());
        vo.setQuestionCount(paper.getQuestionCount());
        vo.setCreateTime(paper.getCreateTime());
        vo.setQuestions(examPaperQuestionMapper.selectPaperQuestions(id));
        return vo;
    }

    // ==================== 私有方法 ====================

    /**
     * 管理权限校验（粗粒度：当前登录用户须持有 admin 或 teacher 角色）
     */
    private void checkTeacherOrAdmin() {
        Long userId = UserContext.getUserId();
        boolean allowed = userId != null && sysRoleMapper.selectByUserId(userId).stream()
                .anyMatch(r -> ADMIN_ROLE_CODE.equals(r.getRoleCode())
                        || TEACHER_ROLE_CODE.equals(r.getRoleCode()));
        if (!allowed) {
            throw new RuntimeException("无权限操作");
        }
    }
}
