import request from '@/utils/request'
import type { CategoryNode } from '@/types'

/** 商品分类接口 */

export interface CategoryPayload {
  parentId: string
  name: string
  sort?: number
  icon?: string
  status?: number
}

export function fetchCategoryTree() {
  return request.get<CategoryNode[]>('/categories')
}

export function fetchCategoryDetail(id: string) {
  return request.get<CategoryNode>(`/categories/${id}`)
}

export function createCategory(data: CategoryPayload) {
  return request.post<string>('/categories', data)
}

export function updateCategory(id: string, data: CategoryPayload) {
  return request.put<void>(`/categories/${id}`, data)
}

export function deleteCategory(id: string) {
  return request.delete<void>(`/categories/${id}`)
}