/**
 * 侧边菜单配置树（与路由表人工保持一致，独立维护以支持分组/图标/排序）
 * roles: 可见角色码，不配 = 全角色可见（仍需登录）
 */
export const menus = [
  {
    path: '/dashboard',
    title: '工作台',
    icon: 'Odometer',
    roles: ['admin', 'teacher'],
  },
  {
    path: '/question',
    title: '题库管理',
    icon: 'Collection',
    roles: ['admin', 'teacher'],
  },
  {
    title: '试卷管理',
    icon: 'Notebook',
    roles: ['admin', 'teacher'],
    children: [
      { path: '/paper', title: '试卷列表' },
      { path: '/paper/generate', title: '智能组卷' },
      { path: '/paper/manual', title: '手动组卷' },
    ],
  },
  {
    title: '考试管理',
    icon: 'Bell',
    roles: ['admin', 'teacher'],
    children: [
      { path: '/exam', title: '考试列表' },
      { path: '/exam/publish', title: '发布考试' },
    ],
  },
  {
    path: '/grading',
    title: 'AI 判卷',
    icon: 'MagicStick',
    roles: ['admin', 'teacher'],
  },
  {
    path: '/analysis',
    title: '成绩分析',
    icon: 'TrendCharts',
    roles: ['admin', 'teacher'],
  },
  {
    path: '/student/exams',
    title: '考试大厅',
    icon: 'AlarmClock',
    roles: ['student'],
  },
  {
    title: '系统管理',
    icon: 'Setting',
    roles: ['admin'],
    children: [
      { path: '/system/users', title: '用户管理' },
      { path: '/system/roles', title: '角色管理' },
      { path: '/system/dicts', title: '字典管理' },
      { path: '/system/logs', title: '操作日志' },
    ],
  },
]

/** 按 roleCodes 过滤菜单树（子菜单全不可见时隐藏父级） */
export function filterMenus(tree, roleCodes) {
  return tree
    .map((item) => {
      if (item.roles && !item.roles.some((r) => roleCodes.includes(r))) return null
      if (item.children) {
        const children = filterMenus(item.children, roleCodes)
        if (!children.length) return null
        return { ...item, children }
      }
      return { ...item }
    })
    .filter(Boolean)
}
