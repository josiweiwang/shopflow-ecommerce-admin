import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult, LoginResult } from '@/types'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from './token'

/** 业务成功码 */
const SUCCESS_CODE = 200

/** 需要跳转登录页的错误码：未携带令牌 */
const CODE_UNAUTHORIZED = 40101

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000
})

/** 是否正在刷新令牌（并发请求只刷新一次，其余排队等待） */
let refreshing = false

/** 等待刷新结果的回调队列 */
let waitingQueue: Array<(token: string | null) => void> = []

/** 请求拦截：自动注入 accessToken */
service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** 响应拦截：统一处理业务码与 HTTP 状态 */
service.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const body = response.data
    if (body && body.code === SUCCESS_CODE) {
      // 直接把业务数据交给调用方，业务代码不需要每次写 res.data.data
      return body as unknown as AxiosResponse
    }
    const message = body?.message || '操作失败'
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
  async (error: AxiosError<ApiResult>) => {
    const response = error.response
    if (!response) {
      ElMessage.error('网络异常，请确认后端服务已启动')
      return Promise.reject(error)
    }

    const body = response.data
    const config = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined

    if (response.status === 401) {
      // 令牌过期/非法：尝试静默续期后重放原请求，用户无感知
      if (body?.code !== CODE_UNAUTHORIZED && config && !config._retry && getRefreshToken()) {
        config._retry = true
        const newToken = await refreshAccessToken()
        if (newToken) {
          config.headers.Authorization = `Bearer ${newToken}`
          return service.request(config)
        }
      }
      redirectToLogin()
      return Promise.reject(new Error(body?.message || '登录状态已失效'))
    }

    ElMessage.error(body?.message || `请求失败（HTTP ${response.status}）`)
    return Promise.reject(error)
  }
)

/**
 * 刷新令牌。
 *
 * 用原生 axios 发起，避免再次进入本实例的拦截器造成递归；
 * 并发场景下只真正刷新一次，其余请求排队等待同一个结果。
 */
async function refreshAccessToken(): Promise<string | null> {
  if (refreshing) {
    return new Promise<string | null>((resolve) => {
      waitingQueue.push(resolve)
    })
  }

  refreshing = true
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    refreshing = false
    return null
  }

  try {
    const { data } = await axios.post<ApiResult<LoginResult>>(
      `${import.meta.env.VITE_API_BASE_URL}/auth/refresh`,
      { refreshToken },
      { timeout: 10000 }
    )
    if (data.code === SUCCESS_CODE) {
      setTokens(data.data.accessToken, data.data.refreshToken)
      waitingQueue.forEach((resolve) => resolve(data.data.accessToken))
      return data.data.accessToken
    }
    waitingQueue.forEach((resolve) => resolve(null))
    return null
  } catch {
    waitingQueue.forEach((resolve) => resolve(null))
    return null
  } finally {
    refreshing = false
    waitingQueue = []
  }
}

/** 清理登录态并跳转登录页，同时记住当前位置便于登录后回跳 */
function redirectToLogin(): void {
  clearTokens()
  const current = window.location.pathname + window.location.search
  if (!current.startsWith('/login')) {
    window.location.href = `/login?redirect=${encodeURIComponent(current)}`
  }
}

/**
 * 统一请求入口。
 *
 * 返回值直接是后端的 R<T>（含 code / message / data / traceId），
 * 出错时已经被拦截器统一提示过，业务层通常只需要处理成功分支。
 */
const request = {
  get<T>(url: string, params?: unknown, config?: AxiosRequestConfig) {
    return service.get<unknown, ApiResult<T>>(url, { params, ...config })
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return service.post<unknown, ApiResult<T>>(url, data, config)
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return service.put<unknown, ApiResult<T>>(url, data, config)
  },
  patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return service.patch<unknown, ApiResult<T>>(url, data, config)
  },
  delete<T>(url: string, config?: AxiosRequestConfig) {
    return service.delete<unknown, ApiResult<T>>(url, config)
  }
}

export default request