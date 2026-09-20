import request from '@/utils/request'
import type {
  CreateOrderPayload,
  OrderCreateResult,
  OrderDetail,
  OrderQuery,
  OrderRow,
  PageResult
} from '@/types'

/** 订单接口 */

export function createOrder(data: CreateOrderPayload) {
  return request.post<OrderCreateResult>('/orders', data)
}

export function fetchOrderPage(params: OrderQuery) {
  return request.get<PageResult<OrderRow>>('/orders', params)
}

export function fetchOrderDetail(orderNo: string) {
  return request.get<OrderDetail>(`/orders/${orderNo}`)
}

export function payOrder(orderNo: string) {
  return request.post<void>(`/orders/${orderNo}/pay`)
}

export function cancelOrder(orderNo: string, reason?: string) {
  return request.post<void>(`/orders/${orderNo}/cancel`, { reason })
}

export function updateOrderStatus(orderNo: string, data: { targetStatus: number; remark?: string }) {
  return request.patch<void>(`/orders/${orderNo}/status`, data)
}