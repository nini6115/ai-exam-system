package com.aiexam.question.service.impl;

import com.aiexam.common.context.UserContext;
import com.aiexam.common.vo.PageVO;
import com.aiexam.question.dto.QuestionAddDTO;
import com.aiexam.question.dto.QuestionQueryDTO;
import com.aiexam.question.dto.QuestionUpdateDTO;
import com.aiexam.question.entity.Question;
import com.aiexam.question.mapper.QuestionMapper;
import com.aiexam.question.service.QuestionService;
import com.aiexam.question.vo.QuestionVO;
import com.aiexam.system.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目服务实现
 */
@Slf4j
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    /** 每页最大条数 */
    private static final int MAX_PAGE_SIZE = 100;
    /** 管理员角色编码 */
    private static final String ADMIN_ROLE_CODE = "admin";
    /** 教师角色编码 */
    private static final String TEACHER_ROLE_CODE = "teacher";
    /** 题型范围：1单选 2多选 3判断 4填空 5简答 */
    private static final List<Integer> VALID_TYPES = Arrays.asList(1, 2, 3, 4, 5);
    /** 判断题合法答案 */
    private static final List<String> JUDGE_ANSWERS = Arrays.asList("对", "错");

    @Autowired
    private SysRoleMapper sysRoleMapper;

    // ==================== 题目管理 ====================

    @Override
    public PageVO<QuestionVO> listQuestions(QuestionQueryDTO dto) {
        checkTeacherOrAdmin();
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(dto.getType() != null, Question::getType, dto.getType())
                .eq(dto.getDifficulty() != null, Question::getDifficulty, dto.getDifficulty())
                .eq(dto.getCategory() != null && !dto.getCategory().isEmpty(), Question::getCategory, dto.getCategory())
                .like(dto.getTitle() != null && !dto.getTitle().isEmpty(), Question::getTitle, dto.getTitle())
                .orderByDesc(Question::getId);
        Page<Question> page = page(new Page<>(dto.getPageNum(),
                Math.min(dto.getPageSize(), MAX_PAGE_SIZE)), wrapper);
        List<QuestionVO> vos = page.getRecords().stream().map(QuestionVO::from).collect(Collectors.toList());
        return PageVO.of(page, vos);
    }

    @Override
    public QuestionVO getQuestionDetail(Long id) {
        checkTeacherOrAdmin();
        Question question = getById(id);
        if (question == null) {
            throw new RuntimeException("题目不存在");
        }
        return QuestionVO.from(question);
    }

    @Override
    public Long addQuestion(QuestionAddDTO dto) {
        checkTeacherOrAdmin();
        String answer = validateAndNormalize(dto);

        Question question = new Question();
        BeanUtils.copyProperties(dto, question);
        question.setId(null);
        question.setAnswer(answer);
        // 创建人从当前登录用户取，不从前端传
        question.setCreatorId(UserContext.getUserId());
        save(question);

        log.info("用户[{}]新增题目，ID：{}", UserContext.getUsername(), question.getId());
        return question.getId();
    }

    @Override
    public void updateQuestion(QuestionUpdateDTO dto) {
        checkTeacherOrAdmin();
        Question exists = getById(dto.getId());
        if (exists == null) {
            throw new RuntimeException("题目不存在");
        }
        String answer = validateAndNormalize(dto);

        // MP updateById 跳过 null 字段，creatorId 不在 DTO 中不会被改动
        Question question = new Question();
        BeanUtils.copyProperties(dto, question);
        question.setAnswer(answer);
        updateById(question);

        // 题型改为判断/填空/简答时，updateById 跳过 null 不会清除旧选项，这里显式清掉
        if (question.getOptions() == null) {
            lambdaUpdate().eq(Question::getId, dto.getId())
                    .set(Question::getOptions, null)
                    .update();
        }

        log.info("用户[{}]修改题目，ID：{}", UserContext.getUsername(), dto.getId());
    }

    @Override
    public void deleteQuestion(Long id) {
        checkTeacherOrAdmin();
        Question exists = getById(id);
        if (exists == null) {
            throw new RuntimeException("题目不存在");
        }
        // ponytail: 组卷模块上线后，删除前需校验 exam_paper_question 引用
        removeById(id); // @TableLogic 逻辑删除自动生效
        log.info("用户[{}]删除题目，ID：{}", UserContext.getUsername(), id);
    }

    @Override
    public List<String> listCategories() {
        checkTeacherOrAdmin();
        List<Question> questions = list(new QueryWrapper<Question>()
                .select("DISTINCT category")
                .isNotNull("category")
                .ne("category", "")
                .orderByAsc("category"));
        return questions.stream().map(Question::getCategory).collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 按题型校验选项与答案的一致性，返回规范化后的答案（多选字母去重升序）
     */
    private String validateAndNormalize(QuestionAddDTO dto) {
        if (!VALID_TYPES.contains(dto.getType())) {
            throw new RuntimeException("题型错误，只能为1-5");
        }
        if (dto.getDifficulty() == null || dto.getDifficulty() < 1 || dto.getDifficulty() > 3) {
            throw new RuntimeException("难度错误，只能为1-3");
        }
        if (dto.getAnswer() != null) {
            dto.setAnswer(dto.getAnswer().trim());
        }
        List<String> options = dto.getOptions();
        switch (dto.getType()) {
            case 1: // 单选
                checkOptions(options);
                return checkSingleAnswer(dto.getAnswer(), options.size());
            case 2: // 多选
                checkOptions(options);
                return checkMultiAnswer(dto.getAnswer(), options.size());
            case 3: // 判断
                if (!JUDGE_ANSWERS.contains(dto.getAnswer())) {
                    throw new RuntimeException("判断题答案只能为\"对\"或\"错\"");
                }
                dto.setOptions(null);
                return dto.getAnswer();
            default: // 4填空 5简答
                dto.setOptions(null);
                return dto.getAnswer();
        }
    }

    /**
     * 校验选择题选项：至少2项且内容非空
     */
    private void checkOptions(List<String> options) {
        if (options == null || options.size() < 2) {
            throw new RuntimeException("选择题选项至少要有2项");
        }
        if (options.stream().anyMatch(o -> o == null || o.trim().isEmpty())) {
            throw new RuntimeException("选项内容不能为空");
        }
    }

    /**
     * 校验单选答案：单字母且在选项范围内
     */
    private String checkSingleAnswer(String answer, int optionCount) {
        if (answer.length() != 1) {
            throw new RuntimeException("单选题答案为单个字母，如\"A\"");
        }
        char c = answer.charAt(0);
        if (c < 'A' || c >= 'A' + optionCount) {
            throw new RuntimeException("单选题答案超出选项范围");
        }
        return answer;
    }

    /**
     * 校验多选答案：2个以上字母，去重升序后返回（如"BCA"→"ABC"）
     */
    private String checkMultiAnswer(String answer, int optionCount) {
        if (answer.length() < 2) {
            throw new RuntimeException("多选题答案至少为2个字母，如\"AB\"");
        }
        String sorted = answer.chars()
                .distinct()
                .sorted()
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        char first = sorted.charAt(0);
        char last = sorted.charAt(sorted.length() - 1);
        if (first < 'A' || last >= 'A' + optionCount) {
            throw new RuntimeException("多选题答案超出选项范围");
        }
        return sorted;
    }

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
