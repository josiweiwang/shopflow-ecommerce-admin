import request from '@/utils/request'
import type { DashboardOverview, TopProduct, TrendPoint } from '@/types'

/** 数据看板接口 */

export function fetchOverview() {
  return request.get<DashboardOverview>('/admin/dashboard/overview')
}

export function fetchTrend(days = 7) {
  return request.get<TrendPoint[]>('/admin/dashboard/trend', { days })
}

export function fetchTopProducts(limit = 10) {
  return request.get<TopProduct[]>('/admin/dashboard/top-products', { limit })
}