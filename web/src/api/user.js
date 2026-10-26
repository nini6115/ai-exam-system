/**
 * 用户/认证接口 —— 对应 SysUserController（/user）
 */
import request from '@/utils/request'

/** 获取图形验证码 → {captchaKey, captchaImage(base64)} */
export const getCaptcha = () => request.get('/user/captcha')

/** 登录 body:{username,password,captchaKey,captchaCode} → LoginVO */
export const login = (data) => request.post('/user/login', data)

/** 登出（token 由拦截器注入） */
export const logout = () => request.post('/user/logout')

/** 分页查询用户列表 admin */
export const listUsers = (params) => request.get('/user/list', { params })

/** 学生列表（教师/管理员，发布考试选考生用；服务端锁定 student 角色） */
export const listStudents = (params) => request.get('/user/students', { params })

/** 用户详情 */
export const getUser = (id) => request.get(`/user/${id}`)

/** 新增用户 UserAddDTO */
export const addUser = (data) => request.post('/user', data)

/** 修改用户 UserUpdateDTO */
export const updateUser = (data) => request.put('/user', data)

/** 删除用户（逻辑删除） */
export const deleteUser = (id) => request.delete(`/user/${id}`)

/** 重置密码 body:{userId,newPassword} */
export const resetPassword = (data) => request.put('/user/resetPwd', data)

/** 启用/禁用 body:{userId,status} */
export const updateStatus = (data) => request.put('/user/status', data)

/** 分配角色（全量覆盖）body:{userId,roleIds} */
export const assignRole = (data) => request.put('/user/assignRole', data)

/** 查询个人资料（当前用户）→ UserVO */
export const getProfile = () => request.get('/user/profile')

/** 修改个人资料（当前用户）UpdateProfileDTO */
export const updateProfile = (data) => request.put('/user/profile', data)

/** 修改个人密码（当前用户）body:{oldPassword,newPassword} */
export const changePassword = (data) => request.put('/user/changePwd', data)
