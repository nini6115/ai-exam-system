package com.aiexam.system.service.impl;

import com.aiexam.common.context.UserContext;
import com.aiexam.common.utils.CaptchaUtil;
import com.aiexam.common.vo.PageVO;
import com.aiexam.system.dto.AssignRoleDTO;
import com.aiexam.system.dto.ChangePasswordDTO;
import com.aiexam.system.dto.LoginDTO;
import com.aiexam.system.dto.ResetPasswordDTO;
import com.aiexam.system.dto.UpdateProfileDTO;
import com.aiexam.system.dto.UpdateStatusDTO;
import com.aiexam.system.dto.UserAddDTO;
import com.aiexam.system.dto.UserQueryDTO;
import com.aiexam.system.dto.UserUpdateDTO;
import com.aiexam.system.entity.SysRole;
import com.aiexam.system.entity.SysUser;
import com.aiexam.system.mapper.SysRoleMapper;
import com.aiexam.system.mapper.SysUserMapper;
import com.aiexam.system.service.SysUserService;
import com.aiexam.system.vo.CaptchaVO;
import com.aiexam.system.vo.LoginVO;
import com.aiexam.system.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /** 验证码 Redis key 前缀 */
    private static final String CAPTCHA_PREFIX = "login:captcha:";
    /** Token Redis key 前缀 */
    private static final String TOKEN_PREFIX = "login:token:";
    /** 验证码有效期（秒） */
    private static final long CAPTCHA_EXPIRE = 120;
    /** Token 有效期（秒）- 2小时 */
    private static final long TOKEN_EXPIRE = 7200;
    /** 每页最大条数 */
    private static final int MAX_PAGE_SIZE = 100;
    /** 超级管理员用户ID（受保护，不可删除/禁用/移除admin角色） */
    private static final long SUPER_ADMIN_ID = 1L;
    /** 管理员角色编码 */
    private static final String ADMIN_ROLE_CODE = "admin";

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public CaptchaVO getCaptcha() {
        CaptchaUtil.CaptchaResult result = CaptchaUtil.generate();
        String key = UUID.randomUUID().toString().replace("-", "");
        // 存 Redis，转小写存，比对时也转小写
        redisTemplate.opsForValue().set(CAPTCHA_PREFIX + key,
                result.getCode().toLowerCase(), CAPTCHA_EXPIRE, TimeUnit.SECONDS);
        return new CaptchaVO(key, result.getImageBase64());
    }

    @Override
    public LoginVO login(LoginDTO dto, String clientIp) {
        // 1. 校验验证码
        String captchaKey = CAPTCHA_PREFIX + dto.getCaptchaKey();
        String savedCode = redisTemplate.opsForValue().get(captchaKey);
        if (savedCode == null) {
            throw new RuntimeException("验证码已过期");
        }
        // 验证一次即删除
        redisTemplate.delete(captchaKey);
        if (!savedCode.equalsIgnoreCase(dto.getCaptchaCode())) {
            throw new RuntimeException("验证码错误");
        }

        // 2. 查询用户
        SysUser user = sysUserMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 校验状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }

        // 4. 校验密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 5. 生成 Token
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(TOKEN_PREFIX + token,
                String.valueOf(user.getId()), TOKEN_EXPIRE, TimeUnit.SECONDS);

        // 6. 更新登录信息
        sysUserMapper.updateLoginInfo(user.getId(), LocalDateTime.now(), clientIp);

        // 7. 组装返回
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(user.getRoles());
        return vo;
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            redisTemplate.delete(TOKEN_PREFIX + token);
        }
    }

    // ==================== 用户管理 ====================

    @Override
    public PageVO<UserVO> listUsers(UserQueryDTO dto) {
        checkAdmin();
        Page<SysUser> page = new Page<>(dto.getPageNum(), Math.min(dto.getPageSize(), MAX_PAGE_SIZE));
        sysUserMapper.selectUserPage(page, dto);
        fillRoles(page.getRecords());
        List<UserVO> vos = page.getRecords().stream().map(UserVO::from).collect(Collectors.toList());
        return PageVO.of(page, vos);
    }

    @Override
    public UserVO getUserDetail(Long id) {
        checkAdmin();
        SysUser user = getById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setRoles(sysRoleMapper.selectByUserId(id));
        return UserVO.from(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addUser(UserAddDTO dto) {
        checkAdmin();

        // 用户名唯一性校验
        long count = lambdaQuery().eq(SysUser::getUsername, dto.getUsername()).count();
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 角色ID校验
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            checkRoleIds(dto.getRoleIds());
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setId(null);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setStatus(1);
        save(user);

        // 保存角色关联
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            sysUserMapper.insertUserRoles(user.getId(), dto.getRoleIds());
        }

        log.info("用户[{}]新增用户[{}]，ID：{}", UserContext.getUsername(), user.getUsername(), user.getId());
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateDTO dto) {
        checkAdmin();
        SysUser exists = getById(dto.getId());
        if (exists == null) {
            throw new RuntimeException("用户不存在");
        }

        // 超级管理员保护：不可禁用、不可移除 admin 角色
        if (dto.getId() == SUPER_ADMIN_ID) {
            if (dto.getStatus() != null && dto.getStatus() != 1) {
                throw new RuntimeException("超级管理员不能被禁用");
            }
            if (dto.getRoleIds() != null && !dto.getRoleIds().contains(getAdminRoleId())) {
                throw new RuntimeException("超级管理员的admin角色不可被移除");
            }
        }

        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            checkRoleIds(dto.getRoleIds());
        }

        // MP updateById 跳过 null 字段，未传的字段不改动
        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        updateById(user);

        // roleIds 传了就全量覆盖（空数组 = 清空角色），不传不动
        if (dto.getRoleIds() != null) {
            overwriteRoles(dto.getId(), dto.getRoleIds());
        }

        log.info("用户[{}]修改用户[{}]，ID：{}", UserContext.getUsername(), exists.getUsername(), dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        checkAdmin();
        if (id == SUPER_ADMIN_ID) {
            throw new RuntimeException("超级管理员不能删除");
        }
        if (id.equals(UserContext.getUserId())) {
            throw new RuntimeException("不能删除自己");
        }
        SysUser exists = getById(id);
        if (exists == null) {
            throw new RuntimeException("用户不存在");
        }

        removeById(id); // @TableLogic 逻辑删除自动生效
        sysUserMapper.deleteUserRoles(id);
        forceOffline(id);
        log.info("用户[{}]删除用户[{}]，ID：{}", UserContext.getUsername(), exists.getUsername(), id);
    }

    @Override
    public void resetPassword(ResetPasswordDTO dto) {
        checkAdmin();
        SysUser exists = getById(dto.getUserId());
        if (exists == null) {
            throw new RuntimeException("用户不存在");
        }

        SysUser user = new SysUser();
        user.setId(dto.getUserId());
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        updateById(user);

        forceOffline(dto.getUserId());
        log.info("用户[{}]重置用户[{}]的密码", UserContext.getUsername(), exists.getUsername());
    }

    @Override
    public void updateStatus(UpdateStatusDTO dto) {
        checkAdmin();
        if (dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw new RuntimeException("status只能为0或1");
        }
        if (dto.getUserId() == SUPER_ADMIN_ID && dto.getStatus() == 0) {
            throw new RuntimeException("超级管理员不能被禁用");
        }
        if (dto.getUserId().equals(UserContext.getUserId()) && dto.getStatus() == 0) {
            throw new RuntimeException("不能禁用自己");
        }
        SysUser exists = getById(dto.getUserId());
        if (exists == null) {
            throw new RuntimeException("用户不存在");
        }

        SysUser user = new SysUser();
        user.setId(dto.getUserId());
        user.setStatus(dto.getStatus());
        updateById(user);

        if (dto.getStatus() == 0) {
            forceOffline(dto.getUserId());
        }
        log.info("用户[{}]{}用户[{}]，ID：{}", UserContext.getUsername(),
                dto.getStatus() == 1 ? "启用" : "禁用", exists.getUsername(), dto.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRole(AssignRoleDTO dto) {
        checkAdmin();
        SysUser exists = getById(dto.getUserId());
        if (exists == null) {
            throw new RuntimeException("用户不存在");
        }
        // 超级管理员的 admin 角色不可被移除
        if (dto.getUserId() == SUPER_ADMIN_ID && !dto.getRoleIds().contains(getAdminRoleId())) {
            throw new RuntimeException("超级管理员的admin角色不可被移除");
        }
        if (!dto.getRoleIds().isEmpty()) {
            checkRoleIds(dto.getRoleIds());
        }
        overwriteRoles(dto.getUserId(), dto.getRoleIds());
        log.info("用户[{}]为用户[{}]分配角色：{}", UserContext.getUsername(),
                exists.getUsername(), dto.getRoleIds());
    }

    @Override
    public void updateProfile(UpdateProfileDTO dto) {
        // 只能改自己的，userId 从 Token 取，不从前端传
        Long userId = UserContext.getUserId();
        SysUser user = new SysUser();
        user.setId(userId);
        user.setRealName(dto.getRealName());
        user.setGender(dto.getGender());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        updateById(user);
        log.info("用户[{}]修改个人资料", UserContext.getUsername());
    }

    @Override
    public void changePassword(ChangePasswordDTO dto) {
        Long userId = UserContext.getUserId();
        SysUser user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        updateById(update);

        // 修改成功后强制下线，重新登录
        forceOffline(userId);
        log.info("用户[{}]修改个人密码", user.getUsername());
    }

    /**
     * 校验角色ID列表全部存在
     */
    private void checkRoleIds(List<Long> roleIds) {
        Set<Long> distinct = new HashSet<>(roleIds);
        if (sysRoleMapper.selectBatchIds(distinct).size() != distinct.size()) {
            throw new RuntimeException("角色不存在");
        }
    }

    /**
     * 全量覆盖用户角色（先删后插，空列表 = 清空角色）
     */
    private void overwriteRoles(Long userId, List<Long> roleIds) {
        sysUserMapper.deleteUserRoles(userId);
        if (!roleIds.isEmpty()) {
            sysUserMapper.insertUserRoles(userId, roleIds);
        }
    }

    /**
     * 查询 admin 角色ID
     */
    private Long getAdminRoleId() {
        SysRole admin = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, ADMIN_ROLE_CODE));
        if (admin == null) {
            throw new RuntimeException("admin角色不存在");
        }
        return admin.getId();
    }

    /**
     * 强制下线：删除该用户所有已登录 Token
     * ponytail: KEYS 遍历 token，量大时改维护 userId→token 反向索引
     */
    private void forceOffline(Long userId) {
        String userIdStr = String.valueOf(userId);
        Set<String> keys = redisTemplate.keys(TOKEN_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        List<String> toDelete = keys.stream()
                .filter(k -> userIdStr.equals(redisTemplate.opsForValue().get(k)))
                .collect(Collectors.toList());
        if (!toDelete.isEmpty()) {
            redisTemplate.delete(toDelete);
        }
    }

    /**
     * 批量填充用户角色列表
     */
    private void fillRoles(List<SysUser> users) {
        if (users.isEmpty()) {
            return;
        }
        List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());
        Map<Long, List<SysRole>> roleMap = sysRoleMapper.selectByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(SysRole::getUserId));
        users.forEach(u -> u.setRoles(roleMap.getOrDefault(u.getId(), Collections.emptyList())));
    }

    /**
     * 管理权限校验（粗粒度：当前登录用户须持有 admin 角色）
     */
    private void checkAdmin() {
        Long userId = UserContext.getUserId();
        boolean admin = userId != null && sysRoleMapper.selectByUserId(userId).stream()
                .anyMatch(r -> ADMIN_ROLE_CODE.equals(r.getRoleCode()));
        if (!admin) {
            throw new RuntimeException("无权限操作");
        }
    }
}
