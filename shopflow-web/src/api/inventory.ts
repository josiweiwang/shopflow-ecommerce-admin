import request from '@/utils/request'
import type { InventoryItem, InventoryLogItem, PageQuery, PageResult } from '@/types'

/** 库存接口 */

export interface InventoryQuery extends PageQuery {
  productId?: string
  lowStockOnly?: boolean
}

export interface InventoryLogQuery extends PageQuery {
  bizType?: number
  orderNo?: string
}

export function fetchInventoryPage(params: InventoryQuery) {
  return request.get<PageResult<InventoryItem>>('/inventories', params)
}

export function fetchInventory(productId: string) {
  return request.get<InventoryItem>(`/inventories/${productId}`)
}

export function inboundStock(productId: string, data: { quantity: number; remark?: string }) {
  return request.post<void>(`/inventories/${productId}/inbound`, data)
}

export function adjustStock(productId: string, data: { delta: number; remark?: string }) {
  return request.post<void>(`/inventories/${productId}/adjust`, data)
}

export function fetchInventoryLogs(productId: string, params: InventoryLogQuery) {
  return request.get<PageResult<InventoryLogItem>>(`/inventories/${productId}/logs`, params)
}