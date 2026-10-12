/**
 * 角色接口 —— 对应 SysRoleController（/role，全部 admin）
 */
import request from '@/utils/request'

/** 全量角色列表（下拉用） */
export const listRoles = () => request.get('/role/list')

/** 角色详情 */
export const getRole = (id) => request.get(`/role/${id}`)

/** 角色下用户分页 */
export const listRoleUsers = (id, params) => request.get(`/role/${id}/users`, { params })

/** 新增角色 */
export const addRole = (data) => request.post('/role', data)

/** 修改角色（roleCode 不可改） */
export const updateRole = (data) => request.put('/role', data)

/** 删除角色（内置角色/有用户时后端拒绝） */
export const deleteRole = (id) => request.delete(`/role/${id}`)
