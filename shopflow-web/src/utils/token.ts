/**
 * 令牌本地存储。
 *
 * 这里刻意把读写封装起来：以后要改成 sessionStorage、加密存储或内存存储，
 * 只需要改这一个文件，业务代码不受影响。
 */
const ACCESS_TOKEN_KEY = 'shopflow_access_token'
const REFRESH_TOKEN_KEY = 'shopflow_refresh_token'

export function getAccessToken(): string {
  return localStorage.getItem(ACCESS_TOKEN_KEY) || ''
}

export function getRefreshToken(): string {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || ''
}

export function setTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}