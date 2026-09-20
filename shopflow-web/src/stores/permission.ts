import { computed } from 'vue'
import { defineStore } from 'pinia'
import type { RouteRecordRaw } from 'vue-router'
import { layoutRoutes } from '@/router/routes'
import { useUserStore } from './user'

/** 侧边栏菜单项 */
export interface MenuItem {
  path: string
  title: string
  icon?: string
}

/**
 * 菜单与访问范围。
 *
 * 菜单由路由表 + 当前用户权限推导出来：新增页面只需要在路由表加一条并声明 permission，
 * 不需要再维护一份单独的菜单配置（两份配置最容易出现"菜单能点但接口 403"的不一致）。
 */
export const usePermissionStore = defineStore('permission', () => {
  const userStore = useUserStore()

  const menus = computed<MenuItem[]>(() => {
    const children = (layoutRoutes[0].children ?? []) as RouteRecordRaw[]
    return children
      .filter((route) => {
        if (route.meta?.hidden) {
          return false
        }
        return userStore.hasPermission(route.meta?.permission)
      })
      .map((route) => ({
        path: `/${String(route.path)}`,
        title: route.meta?.title ?? '',
        icon: route.meta?.icon
      }))
  })

  /** 当前用户有权访问的第一个页面，用于登录后跳转与越权时的兜底跳转 */
  const firstAccessiblePath = computed(() => menus.value[0]?.path ?? '/403')

  return { menus, firstAccessiblePath }
})