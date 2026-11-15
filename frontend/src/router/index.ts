import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'project',
        name: 'Project',
        component: () => import('@/views/project/index.vue'),
        meta: { title: '项目管理' }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/role.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'env',
        name: 'Env',
        component: () => import('@/views/env/index.vue'),
        meta: { title: '环境管理' }
      },
      {
        path: 'api',
        name: 'Api',
        component: () => import('@/views/api/index.vue'),
        meta: { title: '接口测试' }
      },
      {
        path: 'case',
        name: 'Case',
        component: () => import('@/views/case/index.vue'),
        meta: { title: '测试用例' }
      },
      {
        path: 'ui',
        name: 'Ui',
        component: () => import('@/views/ui/index.vue'),
        meta: { title: 'UI自动化' }
      },
      {
        path: 'scene',
        name: 'Scene',
        component: () => import('@/views/scene/index.vue'),
        meta: { title: '场景编排' }
      },
      {
        path: 'exec',
        name: 'Exec',
        component: () => import('@/views/exec/index.vue'),
        meta: { title: '执行与报告' }
      },
      {
        path: 'schedule',
        name: 'Schedule',
        component: () => import('@/views/schedule/index.vue'),
        meta: { title: '定时任务' }
      },
      {
        path: 'suite',
        name: 'Suite',
        component: () => import('@/views/suite/index.vue'),
        meta: { title: '测试套件' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }
  if (to.path === '/login' && token) {
    next('/dashboard')
    return
  }
  next()
})

router.afterEach((to) => {
  document.title = `${to.meta.title ?? ''} - 通用自动化测试平台`
})

export default router
