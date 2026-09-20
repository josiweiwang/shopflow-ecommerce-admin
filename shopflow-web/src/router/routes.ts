import type { RouteRecordRaw } from 'vue-router'
import Layout from '@/layout/index.vue'

/**
 * 扩展 vue-router 的路由元信息类型。
 *
 * 用类型增强代替「到处 as AppRouteMeta」的强制断言：
 * 这样 route.meta.title / route.meta.permission 在任何组件里都是类型安全的，
 * 新增元信息字段也只需要改这一处。
 */
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    permission?: string
    hidden?: boolean
  }
}

/** 路由元信息 */
export interface AppRouteMeta {
  /** 菜单与面包屑标题 */
  title: string
  /** Element Plus 图标组件名 */
  icon?: string
  /** 访问该页面所需的权限码，不填表示登录即可访问 */
  permission?: string
  /** 是否在侧边栏隐藏 */
  hidden?: boolean
}

/**
 * 全部业务页面。
 *
 * 这里采用「静态路由 + 权限过滤」：路由表一次注册，侧边栏与守卫按 meta.permission 过滤。
 * 相比运行时 addRoute 动态挂载，好处是路由结构可静态分析、出错时更容易定位；
 * 真正的权限边界始终在后端（每个接口都有 @RequirePermission 校验），前端过滤只影响展示。
 */
export const layoutRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据看板', icon: 'DataLine', permission: 'dashboard:read' }
      },
      {
        path: 'product',
        name: 'ProductList',
        component: () => import('@/views/product/index.vue'),
        meta: { title: '商品管理', icon: 'Goods', permission: 'product:read' }
      },
      {
        path: 'category',
        name: 'CategoryList',
        component: () => import('@/views/category/index.vue'),
        meta: { title: '商品分类', icon: 'Menu', permission: 'category:read' }
      },
      {
        path: 'inventory',
        name: 'InventoryList',
        component: () => import('@/views/inventory/index.vue'),
        meta: { title: '库存管理', icon: 'Box', permission: 'inventory:read' }
      },
      {
        path: 'order',
        name: 'OrderList',
        component: () => import('@/views/order/index.vue'),
        meta: { title: '订单管理', icon: 'Tickets', permission: 'order:read' }
      },
      {
        path: 'order/:orderNo',
        name: 'OrderDetail',
        component: () => import('@/views/order/detail.vue'),
        meta: { title: '订单详情', hidden: true, permission: 'order:read' }
      },
      {
        path: 'user',
        name: 'UserList',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理', icon: 'User', permission: 'user:read' }
      },
      {
        path: 'log',
        name: 'OperationLog',
        component: () => import('@/views/log/index.vue'),
        meta: { title: '操作日志', icon: 'Document', permission: 'log:read' }
      }
    ]
  }
]

/** 无需登录即可访问的路由 */
export const publicRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无访问权限', hidden: true }
  }
]

/** 兜底路由 */
export const fallbackRoutes: RouteRecordRaw[] = [
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', hidden: true }
  }
]

export const routes: RouteRecordRaw[] = [...publicRoutes, ...layoutRoutes, ...fallbackRoutes]