# 用户管理模块 — 需求规格说明书

> 模块：用户管理（system/user）
> 版本：v1.0
> 日期：2026-10-04
> 状态：待开发

---

## 一、模块概述

用户管理模块，管理员对系统用户进行增删改查、角色分配、重置密码、启用禁用；登录用户可修改个人资料和密码。

**目标用户**：
- 超级管理员（admin）：全量用户管理
- 普通用户：仅管理自己的资料和密码

---

## 二、依赖表

| 表名 | 用途 | 操作 |
|---|---|---|
| sys_user | 用户主表 | 增删改查 |
| sys_role | 角色表 | 查询（回显角色列表） |
| sys_user_role | 用户角色关联 | 增删（分配角色时） |

---

## 三、接口清单

共 10 个接口。所有管理接口需要 admin 角色（当前粗粒度控制，后续 Shiro 细化）。

### 统一约定

- Base URL：`/api`
- 鉴权：Header `Authorization: <token>`
- 返回：`AjaxResult<T>`
- 分页参数：`pageNum`（默认1）、`pageSize`（默认10，最大100）

---

### 1. 用户分页列表

| 项 | 内容 |
|---|---|
| 接口 | `GET /user/list` |
| 权限 | admin |
| 描述 | 分页查询用户列表，支持多条件筛选 |

**请求参数（Query）：**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 10 |
| username | String | 否 | 用户名，模糊查询 |
| realName | String | 否 | 真实姓名，模糊查询 |
| status | Integer | 否 | 状态：0禁用 1正常 |
| roleCode | String | 否 | 按角色编码筛选 |

**返回数据：**

```json
{
  "list": [
    {
      "id": 1,
      "username": "admin",
      "realName": "系统管理员",
      "userNo": "ADMIN001",
      "gender": 0,
      "phone": "13800000000",
      "email": "admin@example.com",
      "avatar": "",
      "status": 1,
      "lastLoginTime": "2026-10-04 10:00:00",
      "createTime": "2026-10-01 00:00:00",
      "roles": [
        { "id": 1, "roleCode": "admin", "roleName": "超级管理员" }
      ]
    }
  ],
  "total": 100,
  "pageNum": 1,
  "pageSize": 10
}
```

---

### 2. 用户详情

| 项 | 内容 |
|---|---|
| 接口 | `GET /user/{id}` |
| 权限 | admin |
| 描述 | 根据ID查询用户详情（含角色） |

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 用户ID |

**返回数据：**

```json
{
  "id": 1,
  "username": "admin",
  "realName": "系统管理员",
  "userNo": "ADMIN001",
  "gender": 0,
  "phone": "13800000000",
  "email": "admin@example.com",
  "avatar": "",
  "status": 1,
  "createTime": "2026-10-01 00:00:00",
  "roles": [
    { "id": 1, "roleCode": "admin", "roleName": "超级管理员" }
  ]
}
```

---

### 3. 新增用户

| 项 | 内容 |
|---|---|
| 接口 | `POST /user` |
| 权限 | admin |
| 描述 | 创建新用户 |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | String | 是 | 登录账号，唯一，2-50字符 |
| password | String | 是 | 初始密码，6-100字符 |
| realName | String | 是 | 真实姓名 |
| userNo | String | 否 | 学号/工号 |
| gender | Integer | 否 | 性别：0未知 1男 2女 |
| phone | String | 否 | 手机号 |
| email | String | 否 | 邮箱 |
| roleIds | Long[] | 否 | 角色ID列表 |

**返回数据：**

```json
{ "id": 10 }
```

**业务规则：**
- username 唯一，重复返回"用户名已存在"
- 密码 BCrypt 加密后存储
- 角色列表为空则不分配角色

---

### 4. 修改用户

| 项 | 内容 |
|---|---|
| 接口 | `PUT /user` |
| 权限 | admin |
| 描述 | 修改用户基本信息 |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 用户ID |
| realName | String | 否 | 真实姓名 |
| userNo | String | 否 | 学号/工号 |
| gender | Integer | 否 | 性别 |
| phone | String | 否 | 手机号 |
| email | String | 否 | 邮箱 |
| avatar | String | 否 | 头像URL |
| status | Integer | 否 | 状态 |
| roleIds | Long[] | 否 | 角色ID列表（传了就全量覆盖） |

**返回数据：** 无

**业务规则：**
- 不能修改 username 和 password
- roleIds 传了就全量覆盖（先删后插），不传则不改动角色
- 超级管理员（id=1）的状态不能改成禁用

---

### 5. 删除用户

| 项 | 内容 |
|---|---|
| 接口 | `DELETE /user/{id}` |
| 权限 | admin |
| 描述 | 删除用户（逻辑删除） |

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 用户ID |

**返回数据：** 无

**业务规则：**
- 逻辑删除（update deleted=1）
- 不能删除自己
- 不能删除超级管理员（id=1）
- 同步删除用户角色关联（逻辑删除表保留）

---

### 6. 重置密码

| 项 | 内容 |
|---|---|
| 接口 | `PUT /user/resetPassword/{id}` |
| 权限 | admin |
| 描述 | 管理员重置用户密码 |

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 用户ID |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| newPassword | String | 是 | 新密码，6-100字符 |

**返回数据：** 无

**业务规则：**
- 密码 BCrypt 加密
- 重置后用户 token 不失效（简单处理，要失效的话后续加）

---

### 7. 启用/禁用用户

| 项 | 内容 |
|---|---|
| 接口 | `PUT /user/{id}/status` |
| 权限 | admin |
| 描述 | 启用或禁用用户 |

**路径参数：**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 用户ID |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| status | Integer | 是 | 0禁用 1正常 |

**返回数据：** 无

**业务规则：**
- 不能禁用自己
- 不能禁用超级管理员（id=1）
- 禁用后，用户的 token 是否失效？→ 暂不失效（下次登录时校验状态）

---

### 8. 分配角色

| 项 | 内容 |
|---|---|
| 接口 | `PUT /user/assignRole` |
| 权限 | admin |
| 描述 | 给用户分配角色（全量覆盖） |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户ID |
| roleIds | Long[] | 是 | 角色ID列表，可为空数组（清空角色） |

**返回数据：** 无

**业务规则：**
- 全量覆盖：先删除该用户所有角色关联，再批量插入
- 角色ID不存在时返回"角色不存在"
- 超级管理员（id=1）的 admin 角色不可被移除

---

### 9. 修改个人资料

| 项 | 内容 |
|---|---|
| 接口 | `PUT /user/profile` |
| 权限 | 登录用户 |
| 描述 | 用户修改自己的基本信息 |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| realName | String | 否 | 真实姓名 |
| gender | Integer | 否 | 性别 |
| phone | String | 否 | 手机号 |
| email | String | 否 | 邮箱 |
| avatar | String | 否 | 头像URL |

**返回数据：** 无

**业务规则：**
- 用户ID从 Token（ThreadLocal）中取，不从前端传
- 不能修改 username、password、status、userNo
- 不允许越权修改他人

---

### 10. 修改个人密码

| 项 | 内容 |
|---|---|
| 接口 | `PUT /user/changePassword` |
| 权限 | 登录用户 |
| 描述 | 用户修改自己的密码 |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| oldPassword | String | 是 | 原密码 |
| newPassword | String | 是 | 新密码，6-100字符 |

**返回数据：** 无

**业务规则：**
- 用户ID从 Token 中取
- 必须验证原密码正确
- 新密码 BCrypt 加密后存储
- 修改后 token 不失效（简单处理）

---

## 四、公共业务规则

1. **超级管理员保护**：id=1 的用户不可删除、不可禁用、不可移除 admin 角色
2. **用户名唯一**：新增和修改时校验 username 唯一性
3. **密码加密**：所有密码存储前用 BCrypt 加密
4. **逻辑删除**：删除操作只改 deleted 字段，保留历史数据
5. **敏感字段不返回**：列表和详情接口不返回 password 字段
6. **操作日志**：管理类操作（新增、修改、删除、重置密码）打 info 日志，记录操作人

---

## 五、涉及文件

### 后端（待实现）

> 技术栈：MyBatis-Plus 3.5.x，单表 CRUD 用 BaseMapper + IService，联表查询写 XML。

```
src/main/java/com/aiexam/
├── common/
│   ├── context/UserContext.java        # ThreadLocal 当前用户
│   ├── interceptor/TokenInterceptor.java # Token 拦截器
│   └── vo/PageVO.java                  # 分页通用VO
├── config/
│   └── WebMvcConfig.java               # 拦截器注册
└── system/
    ├── controller/SysUserController.java   # 扩展（已有登录相关）
    ├── service/SysUserService.java         # 接口，继承 IService<SysUser>
    ├── service/impl/SysUserServiceImpl.java # 实现，继承 ServiceImpl
    ├── mapper/SysUserMapper.java           # 继承 BaseMapper<SysUser>
    ├── mapper/SysRoleMapper.java           # 继承 BaseMapper<SysRole>
    ├── entity/SysUser.java                 # 加 @TableName / @TableId / @TableLogic
    ├── entity/SysRole.java                 # 加 @TableName / @TableId
    ├── dto/
    │   ├── UserQueryDTO.java               # 分页查询参数
    │   ├── UserAddDTO.java                 # 新增用户
    │   ├── UserUpdateDTO.java              # 修改用户
    │   ├── AssignRoleDTO.java              # 分配角色
    │   ├── ResetPasswordDTO.java           # 重置密码
    │   ├── UpdateStatusDTO.java            # 启用禁用
    │   ├── UpdateProfileDTO.java           # 修改个人资料
    │   └── ChangePasswordDTO.java          # 修改个人密码
    └── vo/
        └── UserVO.java                     # 用户详情VO

src/main/resources/mapper/system/
├── SysUserMapper.xml   # 联表查询（分页列表、带角色查询）
└── SysRoleMapper.xml   # 联表查询（用户角色列表）
```

**单表 CRUD 用 MyBatis-Plus 内置方法：**
- 新增：`save()`、`saveBatch()`
- 修改：`updateById()`、`lambdaUpdate()`
- 删除：`removeById()`（逻辑删除自动生效）
- 查询：`getById()`、`lambdaQuery().eq()...`

**必须写 XML 的场景：**
- 多表联查（用户列表带角色）
- 复杂条件查询（多条件动态 SQL）
- 分页查询（配合 MyBatis-Plus Page 对象）
