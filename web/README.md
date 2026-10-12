# AI 在线考试系统 - 前端（web/）

Vue 3 + Vite + Element Plus + Pinia + Axios，配合根目录 Spring Boot 后端使用。

## 环境要求

- Node.js ≥ 20（当前开发环境 v26.9.0）
- 后端已启动：`http://localhost:8080`（context-path `/api`）

## 启动

```bash
cd web
npm install
npm run dev     # 开发：http://localhost:5173，/api 自动代理到 localhost:8080
npm run build   # 生产构建 → dist/
npm run preview # 本地预览构建产物
```

## 内置账号（来自 Flyway 种子数据）

| 账号 | 密码 | 角色 |
|---|---|---|
| admin | admin123 | 管理员（全部功能） |

教师/学生账号：用 admin 登录 → 系统管理 → 用户管理 → 新增用户并分配 teacher / student 角色。

## 目录结构

```
src/
├── api/          # 与后端 Controller 一一对应的接口模块
├── utils/        # request(axios封装)/auth(token)/download(blob)/bus(事件)
├── constants/    # 业务枚举与答案序列化契约
├── router/       # 全静态路由 + meta.roles 守卫
├── store/        # user / dict
├── hooks/        # usePagination / useDict / useCountdown
├── components/   # EnumTag/DictSelect/QuestionForm/UserSelectDialog/EChart
├── layout/       # 后台布局（菜单按角色过滤）
└── views/        # 页面（system/question/paper/exam/grading/analysis/student）
```

## 关键约定（改代码前必读）

- **认证**：请求头 `Authorization: <token>`（无 Bearer），token 存 localStorage `ai_exam_token`，后端 2 小时硬过期
- **响应**：统一 `AjaxResult{code,msg,data}`，HTTP 恒 200（除 401）；`code===200` 才成功
- **分页**：`pageNum`/`pageSize`（≤100），返回 `{list,total,pageNum,pageSize}`
- **跨域**：开发走 Vite proxy；生产 Nginx 同域反代（后端不配 CORS）
- **答案格式**：单选 `"B"`、多选 `"ACD"`（排序后比对）、判断 `"对"/"错"`、填空/简答原文
- **选项乱序**：前端显示乱序，保存时必须映射回原始字母（见 `views/student/taking`）

详细设计：`docs/superpowers/specs/2026-10-12-vue3-前端设计.md`
