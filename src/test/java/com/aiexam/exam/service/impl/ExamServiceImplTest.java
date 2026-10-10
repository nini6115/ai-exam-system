package com.aiexam.exam.service.impl;

import com.aiexam.common.context.LoginUser;
import com.aiexam.common.context.UserContext;
import com.aiexam.common.vo.PageVO;
import com.aiexam.exam.dto.ExamHallQueryDTO;
import com.aiexam.exam.mapper.ExamUserMapper;
import com.aiexam.exam.vo.ExamHallVO;
import com.aiexam.paper.mapper.ExamPaperMapper;
import com.aiexam.system.entity.SysRole;
import com.aiexam.system.mapper.SysRoleMapper;
import com.aiexam.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 学生考试大厅单元测试（Mockito，不依赖 MySQL/Redis）
 * <p>
 * 覆盖：学生角色校验、状态参数校验、分数脱敏规则。
 */
@ExtendWith(MockitoExtension.class)
class ExamServiceImplTest {

    private static final Long USER_ID = 100L;

    @Mock
    private ExamUserMapper examUserMapper;

    @Mock
    private ExamPaperMapper examPaperMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @InjectMocks
    private ExamServiceImpl service;

    @BeforeEach
    void setUp() {
        UserContext.set(new LoginUser(USER_ID, "student01", "张三"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("大厅：非学生角色无权限访问")
    void getMyExams_notStudent_throws() {
        when(sysRoleMapper.selectByUserId(USER_ID)).thenReturn(List.of(buildRole("teacher")));

        assertThatThrownBy(() -> service.getMyExams(buildQuery(null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("无权限操作");
    }

    @Test
    @DisplayName("大厅：状态筛选参数非法应报错")
    void getMyExams_invalidStatus_throws() {
        when(sysRoleMapper.selectByUserId(USER_ID)).thenReturn(List.of(buildRole("student")));

        assertThatThrownBy(() -> service.getMyExams(buildQuery(5)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("状态筛选参数非法");
    }

    @Test
    @DisplayName("大厅：分数脱敏——未进入/答题中/未开即显成绩时不返回分数")
    void getMyExams_scoresMaskedCorrectly() {
        when(sysRoleMapper.selectByUserId(USER_ID)).thenReturn(List.of(buildRole("student")));
        List<ExamHallVO> records = List.of(
                buildHallVO(null, 1, new BigDecimal("90")),  // 未进入过考试
                buildHallVO(2, 1, new BigDecimal("88")),      // 已交卷 + 交卷即显 → 可见
                buildHallVO(2, 0, new BigDecimal("77")),      // 已交卷但未开即显 → 不可见
                buildHallVO(1, 1, new BigDecimal("66"))       // 答题中 → 不可见
        );
        when(examUserMapper.selectMyExamPage(any(), eq(USER_ID), eq(2))).thenAnswer(invocation -> {
            Page<ExamHallVO> page = invocation.getArgument(0);
            page.setRecords(records);
            page.setTotal(records.size());
            return page;
        });

        PageVO<ExamHallVO> vo = service.getMyExams(buildQuery(2));

        assertThat(vo.getList()).hasSize(4);
        List<ExamHallVO> list = vo.getList();
        assertThat(list.get(0).getScoreVisible()).isFalse();
        assertThat(list.get(0).getBestScore()).isNull();
        assertThat(list.get(1).getScoreVisible()).isTrue();
        assertThat(list.get(1).getBestScore()).isEqualByComparingTo("88");
        assertThat(list.get(2).getScoreVisible()).isFalse();
        assertThat(list.get(2).getBestScore()).isNull();
        assertThat(list.get(3).getScoreVisible()).isFalse();
        assertThat(list.get(3).getBestScore()).isNull();
    }

    // ==================== 测试数据 ====================

    private ExamHallQueryDTO buildQuery(Integer status) {
        ExamHallQueryDTO dto = new ExamHallQueryDTO();
        dto.setStatus(status);
        return dto;
    }

    private SysRole buildRole(String roleCode) {
        SysRole role = new SysRole();
        role.setRoleCode(roleCode);
        return role;
    }

    private ExamHallVO buildHallVO(Integer sheetStatus, Integer showScoreAfter, BigDecimal bestScore) {
        ExamHallVO vo = new ExamHallVO();
        vo.setExamId(10L);
        vo.setExamName("Java 期末考试");
        vo.setSheetStatus(sheetStatus);
        vo.setShowScoreAfter(showScoreAfter);
        vo.setBestScore(bestScore);
        return vo;
    }
}
