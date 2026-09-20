import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser, login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginPayload } from '@/api/auth'
import type { UserInfo } from '@/types'
import { clearTokens, setTokens } from '@/utils/token'

/**
 * 登录用户状态。
 *
 * 权限判断统一走 hasPermission()，组件和路由守卫都只依赖这一个入口，
 * 避免"有的地方判断角色、有的地方判断权限码"这种不一致。
 */
export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo | null>(null)

  const roles = computed(() => userInfo.value?.roleCodes ?? [])
  const permissions = computed(() => userInfo.value?.permissions ?? [])
  const isAdmin = computed(() => roles.value.includes('ADMIN'))
  const displayName = computed(
    () => userInfo.value?.nickname || userInfo.value?.username || '未登录'
  )

  /** 是否拥有指定权限（管理员拥有全部权限） */
  function hasPermission(code?: string): boolean {
    if (!code) {
      return true
    }
    return isAdmin.value || permissions.value.includes(code)
  }

  /** 登录：保存双令牌与用户信息 */
  async function doLogin(payload: LoginPayload): Promise<UserInfo> {
    const { data } = await loginApi(payload)
    setTokens(data.accessToken, data.refreshToken)
    userInfo.value = data.userInfo
    return data.userInfo
  }

  /** 拉取当前用户信息（刷新页面后恢复登录态） */
  async function loadUserInfo(): Promise<UserInfo> {
    const { data } = await getCurrentUser()
    userInfo.value = data
    return data
  }

  /** 登出：即使后端调用失败也要清空本地登录态 */
  async function doLogout(): Promise<void> {
    try {
      await logoutApi()
    } catch {
      // 令牌可能已过期，忽略即可
    } finally {
      reset()
    }
  }

  function reset(): void {
    clearTokens()
    userInfo.value = null
  }

  return {
    userInfo,
    roles,
    permissions,
    isAdmin,
    displayName,
    hasPermission,
    doLogin,
    loadUserInfo,
    doLogout,
    reset
  }
})