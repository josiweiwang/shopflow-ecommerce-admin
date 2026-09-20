import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePermissionStore } from '@/stores/permission'
import { useUserStore } from '@/stores/user'
import { getAccessToken } from '@/utils/token'
import { routes } from './routes'

const APP_TITLE = 'ShopFlow 电商后台管理系统'

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

/**
 * 全局前置守卫。
 *
 * 处理四件事：
 * 1. 设置浏览器标题；
 * 2. 未登录访问受保护页面 -> 跳登录页并记住来源；
 * 3. 有令牌但内存中没有用户信息（例如刷新页面）-> 拉一次 /auth/me 恢复状态；
 * 4. 已登录但缺少页面所需权限 -> 提示并跳到该用户有权访问的第一个页面。
 *
 * 注意：前端的权限过滤只是体验优化，真正的权限边界在后端接口上。
 */
router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} | ${APP_TITLE}` : APP_TITLE

  const userStore = useUserStore()
  const permissionStore = usePermissionStore()

  if (to.path === '/login') {
    return getAccessToken() && userStore.userInfo ? permissionStore.firstAccessiblePath : true
  }

  if (!getAccessToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (!userStore.userInfo) {
    try {
      await userStore.loadUserInfo()
    } catch {
      userStore.reset()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  if (to.meta.permission && !userStore.hasPermission(to.meta.permission)) {
    ElMessage.error('没有访问该页面的权限')
    const fallback = permissionStore.firstAccessiblePath
    return fallback === to.path ? '/403' : fallback
  }

  return true
})

export default router