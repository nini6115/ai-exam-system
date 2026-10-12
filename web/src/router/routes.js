/**
 * 全静态路由表
 *
 * meta 约定：
 * - title: 页面标题（面包屑/浏览器标签）
 * - roles: 可访问角色码数组（不配 = 登录即可）
 * - public: 免登录页面
 * - keepAlive: 列表页保留查询状态（AppMain 的 keep-alive include 按 name 匹配）
 * - hidden: 不出现在菜单
 *
 * 后端无菜单表，页面权限 = meta.roles 与登录返回 roles[].roleCode 求交。
 */
import Layout from '@/layout/index.vue'

export const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/login/index.vue'), meta: { title: '登录', public: true } },
  { path: '/403', name: 'Forbidden', component: () => import('@/views/error/403.vue'), meta: { title: '无权限', public: true } },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', public: true },
  },

  {
    path: '/',
    name: 'Layout',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', roles: ['admin', 'teacher'], keepAlive: true },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', hidden: true },
      },

      // ---- 系统管理（admin）----
      {
        path: 'system/users',
        name: 'SystemUser',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', roles: ['admin'], keepAlive: true },
      },
      {
        path: 'system/roles',
        name: 'SystemRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', roles: ['admin'], keepAlive: true },
      },
      {
        path: 'system/dicts',
        name: 'SystemDict',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '字典管理', roles: ['admin'], keepAlive: true },
      },
      {
        path: 'system/logs',
        name: 'SystemLog',
        component: () => import('@/views/system/log/index.vue'),
        meta: { title: '操作日志', roles: ['admin'], keepAlive: true },
      },

      // ---- 题库 / 试卷（admin+teacher）----
      {
        path: 'question',
        name: 'Question',
        component: () => import('@/views/question/index.vue'),
        meta: { title: '题库管理', roles: ['admin', 'teacher'], keepAlive: true },
      },
      {
        path: 'paper',
        name: 'Paper',
        component: () => import('@/views/paper/index.vue'),
        meta: { title: '试卷管理', roles: ['admin', 'teacher'], keepAlive: true },
      },
      {
        path: 'paper/generate',
        name: 'PaperGenerate',
        component: () => import('@/views/paper/generate.vue'),
        meta: { title: '智能组卷', roles: ['admin', 'teacher'], hidden: true },
      },
      {
        path: 'paper/manual',
        name: 'PaperManual',
        component: () => import('@/views/paper/manual.vue'),
        meta: { title: '手动组卷', roles: ['admin', 'teacher'], hidden: true },
      },
      {
        path: 'paper/detail/:id',
        name: 'PaperDetail',
        component: () => import('@/views/paper/detail.vue'),
        meta: { title: '试卷预览', roles: ['admin', 'teacher'], hidden: true },
      },

      // ---- 考试管理（admin+teacher）----
      {
        path: 'exam',
        name: 'Exam',
        component: () => import('@/views/exam/index.vue'),
        meta: { title: '考试管理', roles: ['admin', 'teacher'], keepAlive: true },
      },
      {
        path: 'exam/publish',
        name: 'ExamPublish',
        component: () => import('@/views/exam/publish.vue'),
        meta: { title: '发布考试', roles: ['admin', 'teacher'], hidden: true },
      },
      {
        path: 'exam/detail/:id',
        name: 'ExamDetail',
        component: () => import('@/views/exam/detail.vue'),
        meta: { title: '考试详情', roles: ['admin', 'teacher'], hidden: true },
      },

      // ---- AI 判卷（admin+teacher）----
      {
        path: 'grading',
        name: 'Grading',
        component: () => import('@/views/grading/index.vue'),
        meta: { title: 'AI 判卷', roles: ['admin', 'teacher'], keepAlive: true },
      },
      {
        path: 'grading/:examId/sheet/:sheetId',
        name: 'GradingSheet',
        component: () => import('@/views/grading/sheet.vue'),
        meta: { title: '答卷复核', roles: ['admin', 'teacher'], hidden: true },
      },

      // ---- 成绩分析（admin+teacher）----
      {
        path: 'analysis',
        name: 'Analysis',
        component: () => import('@/views/analysis/index.vue'),
        meta: { title: '成绩分析', roles: ['admin', 'teacher'], keepAlive: true },
      },

      // ---- 学生端 ----
      {
        path: 'student/exams',
        name: 'StudentHall',
        component: () => import('@/views/student/hall/index.vue'),
        meta: { title: '考试大厅', roles: ['student'], keepAlive: true },
      },
      {
        path: 'student/result/:examId',
        name: 'StudentResult',
        component: () => import('@/views/student/result/index.vue'),
        meta: { title: '考试结果', roles: ['student'], hidden: true },
      },
    ],
  },

  // 答题页不套 Layout，独立全屏防干扰
  {
    path: '/exam-taking/:examId',
    name: 'ExamTaking',
    component: () => import('@/views/student/taking/index.vue'),
    meta: { title: '在线答题', roles: ['student'] },
  },
]
