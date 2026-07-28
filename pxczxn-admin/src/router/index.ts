import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import type { MenuInfo } from '@/api/auth'

// 动态导入所有页面组件
const modules = import.meta.glob('/src/views/**/*.vue')

// iframe 通用组件
const IframeComponent = () => import('@/views/common/iframe.vue')

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layout/index.vue'),
    redirect: '/community/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '系统概览', icon: 'HomeOutline' }
      },
      // 社区运营
      {
        path: 'community/dashboard',
        name: 'CommunityDashboard',
        component: () => import('@/views/community/dashboard/index.vue'),
        meta: { title: '运营总览', icon: 'GridOutline', permission: 'community:dashboard:view' }
      },
      {
        path: 'community/users',
        name: 'CommunityUsers',
        component: () => import('@/views/community/users/index.vue'),
        meta: { title: '社区用户', icon: 'PersonOutline', permission: 'community:user:list' }
      },
      {
        path: 'community/blogs',
        name: 'CommunityBlogs',
        component: () => import('@/views/community/blogs/index.vue'),
        meta: { title: '博客管理', icon: 'AppsOutline', permission: 'community:blog:list' }
      },
      {
        path: 'community/articles',
        name: 'CommunityArticles',
        component: () => import('@/views/community/articles/index.vue'),
        meta: { title: '文章管理', icon: 'DocumentTextOutline', permission: 'community:article:list' }
      },
      {
        path: 'community/comments',
        name: 'CommunityComments',
        component: () => import('@/views/community/comments/index.vue'),
        meta: { title: '评论治理', icon: 'ChatbubblesOutline', permission: 'community:comment:list' }
      },
      {
        path: 'community/moments',
        name: 'CommunityMoments',
        component: () => import('@/views/community/moments/index.vue'),
        meta: { title: '动态治理', icon: 'PlanetOutline', permission: 'community:moment:list' }
      },
      {
        path: 'community/interactions',
        name: 'CommunityInteractions',
        component: () => import('@/views/community/interactions/index.vue'),
        meta: { title: '互动查询', icon: 'PulseOutline', permission: 'community:interaction:list' }
      },
      {
        path: 'community/reviews',
        name: 'CommunityReviews',
        component: () => import('@/views/community/reviews/index.vue'),
        meta: { title: '文章审核', icon: 'ShieldOutline', permission: 'community:review:list' }
      },
      {
        path: 'community/reports',
        name: 'CommunityReports',
        component: () => import('@/views/community/reports/index.vue'),
        meta: { title: '举报中心', icon: 'FlagOutline', permission: 'community:report:list' }
      },
      {
        path: 'community/appeals',
        name: 'CommunityAppeals',
        component: () => import('@/views/community/appeals/index.vue'),
        meta: { title: '申诉中心', icon: 'ShieldCheck', permission: 'community:appeal:list' }
      },
      { path: 'community/sanctions', name: 'CommunitySanctions', component: () => import('@/views/community/sanctions/index.vue'), meta: { title: '处罚体系', icon: 'HammerOutline', permission: 'community:sanction:list' } },
      {
        path: 'community/content-rules',
        name: 'ContentRules',
        component: () => import('@/views/community/content-rules/index.vue'),
        meta: { title: '内容规则', icon: 'FilterOutline', permission: 'community:content-rule:list' }
      },
      {
        path: 'community/team-applications',
        name: 'TeamApplications',
        component: () => import('@/views/community/team-applications/index.vue'),
        meta: { title: '团队申请审核', icon: 'PeopleOutline', permission: 'community:team:review' }
      },
      {
        path: 'community/team-submissions',
        name: 'TeamSubmissions',
        component: () => import('@/views/community/team-submissions/index.vue'),
        meta: { title: '外部投稿审核', icon: 'SendOutline', permission: 'community:team:review' }
      },
      {
        path: 'community/tags',
        name: 'CommunityTags',
        component: () => import('@/views/community/tags/index.vue'),
        meta: { title: '平台标签', icon: 'PricetagOutline', permission: 'community:tag:list' }
      },
      {
        path: 'community/teams',
        name: 'CommunityTeams',
        component: () => import('@/views/community/teams/index.vue'),
        meta: { title: '团队管理', icon: 'PeopleOutline', permission: 'community:team:list' }
      },
      {
        path: 'community/series',
        name: 'CommunitySeries',
        component: () => import('@/views/community/series/index.vue'),
        meta: { title: '系列审核', icon: 'AlbumsOutline', permission: 'community:series:review' }
      },
      {
        path: 'community/analytics',
        name: 'CommunityAnalytics',
        component: () => import('@/views/community/planned/index.vue'),
        meta: { title: '社区数据分析', icon: 'PulseOutline', permission: 'community:analytics:view' }
      },
      // 个人中心
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', icon: 'PersonOutline' }
      },
      // 系统管理
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '平台人员', icon: 'PersonOutline' }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '平台角色与权限', icon: 'PeopleOutline' }
      },
      {
        path: 'system/menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'MenuOutline' }
      },
      {
        path: 'system/dict',
        name: 'SystemDict',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '字典管理', icon: 'BookOutline' }
      },
      {
        path: 'system/config',
        name: 'SystemConfig',
        component: () => import('@/views/system/config/index.vue'),
        meta: { title: '系统配置', icon: 'SettingsSharp' }
      },
      {
        path: 'system/file',
        name: 'SystemFile',
        component: () => import('@/views/system/file/index.vue'),
        meta: { title: '文件列表', icon: 'FolderOutline' }
      },
      {
        path: 'system/file-config',
        name: 'SystemFileConfig',
        component: () => import('@/views/system/file-config/index.vue'),
        meta: { title: '文件配置', icon: 'CloudOutline' }
      },
      {
        path: 'system/customer',
        name: 'Customer',
        component: () => import('@/views/system/customer/index.vue'),
        meta: { title: '客户管理', icon: 'StarOutline' }
      },
      // 消息中心
      {
        path: 'message/notice',
        name: 'MessageNotice',
        component: () => import('@/views/message/notice/index.vue'),
        meta: { title: '系统通知', icon: 'NotificationsOutline' }
      },
      {
        path: 'message/chat',
        name: 'MessageChat',
        component: () => import('@/views/message/chat/index.vue'),
        meta: { title: '即时聊天', icon: 'ChatbubbleOutline' }
      },
      // 组织管理
      {
        path: 'org/dept',
        name: 'OrgDept',
        component: () => import('@/views/org/dept/index.vue'),
        meta: { title: '平台组织', icon: 'GitNetworkOutline' }
      },
      {
        path: 'org/post',
        name: 'OrgPost',
        component: () => import('@/views/org/post/index.vue'),
        meta: { title: '平台岗位', icon: 'IdCardOutline' }
      },
      // 系统日志
      {
        path: 'log/operlog',
        name: 'LogOper',
        component: () => import('@/views/log/operlog/index.vue'),
        meta: { title: '操作日志', icon: 'ListOutline' }
      },
      {
        path: 'log/loginlog',
        name: 'LogLogin',
        component: () => import('@/views/log/loginlog/index.vue'),
        meta: { title: '登录日志', icon: 'LogInOutline' }
      },
      // 系统监控
      {
        path: 'monitor/online',
        name: 'MonitorOnline',
        component: () => import('@/views/monitor/online/index.vue'),
        meta: { title: '在线用户', icon: 'PeopleCircleOutline' }
      },
      {
        path: 'monitor/job',
        name: 'MonitorJob',
        component: () => import('@/views/monitor/job/index.vue'),
        meta: { title: '定时任务', icon: 'TimerOutline' }
      },
      {
        path: 'monitor/cache',
        name: 'MonitorCache',
        component: () => import('@/views/monitor/cache/index.vue'),
        meta: { title: '缓存监控', icon: 'ServerOutline' }
      },
      {
        path: 'monitor/api-access',
        name: 'MonitorApiAccess',
        component: () => import('@/views/monitor/api-access/index.vue'),
        meta: { title: 'API访问统计', icon: 'StatsChartOutline' }
      },
      {
        path: 'monitor/server',
        name: 'MonitorServer',
        component: () => import('@/views/monitor/server/index.vue'),
        meta: { title: '服务监控', icon: 'DesktopOutline' }
      },
      {
        path: 'monitor/server-manager',
        name: 'ServerManager',
        component: () => import('@/views/monitor/server-manager/index.vue'),
        meta: { title: '服务器管理', icon: 'ServerOutline' }
      },
      {
        path: 'monitor/druid',
        name: 'MonitorDruid',
        component: IframeComponent,
        meta: { title: 'Druid监控', icon: 'PieChartOutline', frameSrc: '/druid/index.html' }
      },
      {
        path: 'test/test',
        name: 'Test',
        component: () => import('@/views/test/test/index.vue'),
        meta: { title: '测试菜单', icon: 'StarOutline' }
      },
      // 开发工具
      {
        path: 'tool/gen',
        name: 'ToolGen',
        component: () => import('@/views/tool/gen/index.vue'),
        meta: { title: '代码生成', icon: 'CodeSlashOutline' }
      },
      // 页签刷新中转路由
      {
        path: 'redirect/:path(.*)',
        name: 'Redirect',
        component: () => import('@/views/redirect/index.vue'),
        meta: { title: '重定向', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 已添加的动态路由
const addedRouteNames = new Set<string>()

/**
 * 根据菜单动态添加新路由（只添加静态路由中没有的）
 */
export function addDynamicRoutes(menus: MenuInfo[]) {
  console.log('[动态路由] 开始处理菜单:', menus)
  
  const addRoutes = (menuList: MenuInfo[]) => {
    for (const menu of menuList) {
      // 只处理菜单类型(type=2)，且有 path
      if (menu.type === 2 && menu.path) {
        const routeName = 'Dynamic-' + menu.id
        
        // 检查是否已经有同路径的静态路由
        const existingRoutes = router.getRoutes()
        const menuPath = menu.path.startsWith('/') ? menu.path.slice(1) : menu.path
        const pathExists = existingRoutes.some(r => r.path === '/' + menuPath || r.path === menuPath)
        
        if (pathExists) {
          console.log(`[动态路由] 跳过(已存在): ${menuPath}`)
          continue
        }
        
        if (addedRouteNames.has(routeName)) {
          console.log(`[动态路由] 跳过(已添加): ${menuPath}`)
          continue
        }
        
        // 判断是否是外链菜单
        if (menu.isFrame === 1 && menu.component) {
          // 外链菜单，使用 iframe 组件
          router.addRoute('Layout', {
            path: menuPath,
            name: routeName,
            component: IframeComponent,
            meta: {
              title: menu.name,
              icon: menu.icon,
              permission: menu.permission,
              frameSrc: menu.component  // 外链地址存在 component 字段
            }
          })
          addedRouteNames.add(routeName)
          console.log(`[动态路由] ✓ 添加外链成功: ${menuPath} -> ${menu.component}`)
        } else if (menu.component) {
          // 普通菜单，加载组件
          const componentName = menu.component.startsWith('/') ? menu.component.slice(1) : menu.component
          const componentPath = `/src/views/${componentName}.vue`
          const component = modules[componentPath]
          
          console.log(`[动态路由] 处理: path=${menuPath}, component=${componentPath}, 组件存在=${!!component}`)
          
          if (component) {
            router.addRoute('Layout', {
              path: menuPath,
              name: routeName,
              component: component,
              meta: {
                title: menu.name,
                icon: menu.icon,
                permission: menu.permission
              }
            })
            addedRouteNames.add(routeName)
            console.log(`[动态路由] ✓ 添加成功: ${menuPath}`)
          } else {
            console.warn(`[动态路由] ✗ 组件不存在: ${componentPath}`)
          }
        }
      }
      
      // 递归处理子菜单
      if (menu.children && menu.children.length > 0) {
        addRoutes(menu.children)
      }
    }
  }
  
  addRoutes(menus)
  console.log('[动态路由] 当前所有路由:', router.getRoutes().map(r => r.path))
}

/**
 * 重置动态路由
 */
export function resetRouter() {
  addedRouteNames.forEach(name => {
    if (router.hasRoute(name)) {
      router.removeRoute(name)
    }
  })
  addedRouteNames.clear()
}

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  document.title = `${to.meta.title || ''} - 星语社区运营中心`

  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  if (!userStore.token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (!userStore.user) {
    try {
      await userStore.getInfo()
      // 添加动态路由（只添加新的，不影响已有的）
      addDynamicRoutes(userStore.menus)
      next({ ...to, replace: true })
      return
    } catch (error) {
      userStore.logout()
      next({ name: 'Login' })
      return
    }
  }

  const permission = to.meta.permission as string | undefined
  if (permission && !userStore.hasPermission(permission)) {
    window.$message?.warning('你没有访问该页面的权限')
    next({ path: '/community/dashboard', replace: true })
    return
  }

  next()
})

export default router
