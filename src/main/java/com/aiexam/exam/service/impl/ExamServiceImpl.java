package com.aiexam.exam.service.impl;

import com.aiexam.common.context.UserContext;
import com.aiexam.common.vo.PageVO;
import com.aiexam.exam.dto.ExamPublishDTO;
import com.aiexam.exam.dto.ExamQueryDTO;
import com.aiexam.exam.entity.Exam;
import com.aiexam.exam.entity.ExamUser;
import com.aiexam.exam.mapper.ExamMapper;
import com.aiexam.exam.mapper.ExamUserMapper;
import com.aiexam.exam.service.ExamService;
import com.aiexam.exam.vo.ExamDetailVO;
import com.aiexam.exam.vo.ExamStudentVO;
import com.aiexam.exam.vo.ExamVO;
import com.aiexam.paper.entity.ExamPaper;
import com.aiexam.paper.mapper.ExamPaperMapper;
import com.aiexam.system.entity.SysRole;
import com.aiexam.system.entity.SysUser;
import com.aiexam.system.mapper.SysRoleMapper;
import com.aiexam.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 考试发布服务实现
 */
@Slf4j
@Service
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam> implements ExamService {

    /** 每页最大条数 */
    private static final int MAX_PAGE_SIZE = 100;
    /** 管理员角色编码 */
    private static final String ADMIN_ROLE_CODE = "admin";
    /** 教师角色编码 */
    private static final String TEACHER_ROLE_CODE = "teacher";
    /** 学生角色编码 */
    private static final String STUDENT_ROLE_CODE = "student";
    /** 防作弊参数默认值（与表默认一致，DTO 未传时补齐） */
    private static final int DEFAULT_ALLOW_LATE = 0;
    private static final int DEFAULT_MAX_ATTEMPTS = 1;
    private static final int DEFAULT_RANDOM_ORDER = 1;
    private static final int DEFAULT_RANDOM_OPTIONS = 1;
    private static final int DEFAULT_MAX_SCREEN_SWITCH = 5;
    private static final int DEFAULT_SCREEN_SWITCH_ACTION = 1;
    private static final int DEFAULT_AWAY_TIMEOUT = 60;
    private static final int DEFAULT_FORBID_COPY = 1;
    private static final int DEFAULT_SHOW_SCORE_AFTER = 0;
    /** 考试状态：1未开始 2进行中 3已结束 */
    private static final int STATUS_NOT_STARTED = 1;
    private static final int STATUS_ONGOING = 2;
    private static final int STATUS_FINISHED = 3;

    @Autowired
    private ExamUserMapper examUserMapper;

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    // ==================== 发布考试 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(ExamPublishDTO dto) {
        checkTeacherOrAdmin();

        // 试卷校验
        ExamPaper paper = examPaperMapper.selectById(dto.getPaperId());
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }

        // 时间校验
        LocalDateTime now = LocalDateTime.now();
        if (!dto.getStartTime().isAfter(now)) {
            throw new RuntimeException("考试开始时间必须晚于当前时间");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new RuntimeException("考试结束时间必须晚于开始时间");
        }

        // 时长：未传则沿用试卷时长
        int duration = dto.getDuration() != null ? dto.getDuration() : paper.getDuration();
        if (duration <= 0) {
            throw new RuntimeException("考试时长必须大于0");
        }

        // 考生校验：去重 → 存在性 → 必须是学生角色
        List<Long> userIds = new ArrayList<>(new LinkedHashSet<>(dto.getUserIds()));
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        if (users.size() != userIds.size()) {
            Set<Long> found = users.stream().map(SysUser::getId).collect(Collectors.toSet());
            String missing = userIds.stream().filter(id -> !found.contains(id))
                    .map(String::valueOf).collect(Collectors.joining(","));
            throw new RuntimeException("考生不存在，ID：" + missing);
        }
        Set<Long> studentIds = sysRoleMapper.selectByUserIds(userIds).stream()
                .filter(r -> STUDENT_ROLE_CODE.equals(r.getRoleCode()))
                .map(SysRole::getUserId)
                .collect(Collectors.toSet());
        if (studentIds.size() != userIds.size()) {
            Map<Long, String> nameMap = users.stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
            String notStudent = userIds.stream().filter(id -> !studentIds.contains(id))
                    .map(id -> nameMap.getOrDefault(id, String.valueOf(id)))
                    .collect(Collectors.joining(","));
            throw new RuntimeException("以下用户不是学生角色：" + notStudent);
        }

        // 写入考试
        Exam exam = new Exam();
        exam.setName(dto.getName());
        exam.setPaperId(dto.getPaperId());
        exam.setCreatorId(UserContext.getUserId());
        exam.setStartTime(dto.getStartTime());
        exam.setEndTime(dto.getEndTime());
        exam.setDuration(duration);
        exam.setAllowLateMinutes(dto.getAllowLateMinutes() != null ? dto.getAllowLateMinutes() : DEFAULT_ALLOW_LATE);
        exam.setMaxAttempts(dto.getMaxAttempts() != null ? dto.getMaxAttempts() : DEFAULT_MAX_ATTEMPTS);
        exam.setRandomOrder(dto.getRandomOrder() != null ? dto.getRandomOrder() : DEFAULT_RANDOM_ORDER);
        exam.setRandomOptions(dto.getRandomOptions() != null ? dto.getRandomOptions() : DEFAULT_RANDOM_OPTIONS);
        exam.setMaxScreenSwitch(dto.getMaxScreenSwitch() != null ? dto.getMaxScreenSwitch() : DEFAULT_MAX_SCREEN_SWITCH);
        exam.setScreenSwitchAction(dto.getScreenSwitchAction() != null ? dto.getScreenSwitchAction() : DEFAULT_SCREEN_SWITCH_ACTION);
        exam.setAwayTimeout(dto.getAwayTimeout() != null ? dto.getAwayTimeout() : DEFAULT_AWAY_TIMEOUT);
        exam.setForbidCopy(dto.getForbidCopy() != null ? dto.getForbidCopy() : DEFAULT_FORBID_COPY);
        exam.setShowScoreAfter(dto.getShowScoreAfter() != null ? dto.getShowScoreAfter() : DEFAULT_SHOW_SCORE_AFTER);
        exam.setStatus(STATUS_NOT_STARTED);
        save(exam);

        // 写入考生关联
        LocalDateTime assignTime = LocalDateTime.now();
        List<ExamUser> examUsers = userIds.stream().map(userId -> {
            ExamUser eu = new ExamUser();
            eu.setExamId(exam.getId());
            eu.setUserId(userId);
            eu.setAttempts(0);
            eu.setAssignTime(assignTime);
            return eu;
        }).collect(Collectors.toList());
        Db.saveBatch(examUsers);

        log.info("用户[{}]发布考试「{}」，考试ID：{}，试卷ID：{}，考生{}人",
                UserContext.getUsername(), exam.getName(), exam.getId(), exam.getPaperId(), examUsers.size());
        return exam.getId();
    }

    // ==================== 考试查询 ====================

    @Override
    public PageVO<ExamVO> listExams(ExamQueryDTO dto) {
        checkTeacherOrAdmin();
        Page<ExamVO> page = new Page<>(dto.getPageNum(),
                Math.min(dto.getPageSize(), MAX_PAGE_SIZE));
        String name = (dto.getName() != null && !dto.getName().isEmpty()) ? dto.getName() : null;
        return PageVO.of(page, baseMapper.selectExamPage(page, name).getRecords());
    }

    @Override
    public ExamDetailVO getExamDetail(Long id) {
        checkTeacherOrAdmin();
        Exam exam = getById(id);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }
        ExamPaper paper = examPaperMapper.selectById(exam.getPaperId());

        ExamDetailVO vo = new ExamDetailVO();
        vo.setId(exam.getId());
        vo.setName(exam.getName());
        vo.setPaperId(exam.getPaperId());
        vo.setPaperName(paper != null ? paper.getName() : null);
        vo.setTotalScore(paper != null ? paper.getTotalScore() : null);
        vo.setPassScore(paper != null ? paper.getPassScore() : null);
        vo.setQuestionCount(paper != null ? paper.getQuestionCount() : null);
        vo.setCreatorId(exam.getCreatorId());
        vo.setStartTime(exam.getStartTime());
        vo.setEndTime(exam.getEndTime());
        vo.setDuration(exam.getDuration());
        vo.setAllowLateMinutes(exam.getAllowLateMinutes());
        vo.setMaxAttempts(exam.getMaxAttempts());
        vo.setRandomOrder(exam.getRandomOrder());
        vo.setRandomOptions(exam.getRandomOptions());
        vo.setMaxScreenSwitch(exam.getMaxScreenSwitch());
        vo.setScreenSwitchAction(exam.getScreenSwitchAction());
        vo.setAwayTimeout(exam.getAwayTimeout());
        vo.setForbidCopy(exam.getForbidCopy());
        vo.setShowScoreAfter(exam.getShowScoreAfter());
        vo.setStatus(deriveStatus(exam));
        vo.setCreateTime(exam.getCreateTime());
        vo.setStudents(examUserMapper.selectExamStudents(id));
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

    /**
     * 按当前时间推导考试展示状态：1未开始 2进行中 3已结束
     */
    private int deriveStatus(Exam exam) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(exam.getStartTime())) {
            return STATUS_NOT_STARTED;
        }
        return now.isBefore(exam.getEndTime()) || now.isEqual(exam.getEndTime())
                ? STATUS_ONGOING : STATUS_FINISHED;
    }
}
