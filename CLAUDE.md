\# 项目：AI 辅助在线考试系统



\## 技术栈

\- 后端：Spring Boot + MyBatis + MySQL + Redis + Quartz

\- 前端：Vue + Element UI

\- 部署：Docker



\## 核心模块

\- 用户与权限管理（Shiro）

\- 题库与智能组卷

\- 在线考试与防作弊

\- AI 辅助判卷（调用大模型 API）

\- 成绩分析与可视化

\- 系统管理与日志



\## 数据库表设计（待补充）

\- user（用户表）

\- exam\_paper（试卷表）

\- question（题目表）

\- answer\_record（答题记录表）

\- ai\_score（AI 判分结果表）



\## 编码规范

\- Controller 统一返回 AjaxResult

\- Service 层先写接口再写实现

\- 所有 SQL 写在 Mapper.xml 中

