import request from '@/utils/request'
import type { PageResult, ProductItem, ProductQuery } from '@/types'

/** 商品接口 */

export interface ProductPayload {
  categoryId: string
  name: string
  sku: string
  subtitle?: string
  mainImage?: string
  detail?: string
  price: number
  originalPrice?: number
  status?: number
  sort?: number
  initStock?: number
  warnStock?: number
}

export function fetchProductPage(params: ProductQuery) {
  return request.get<PageResult<ProductItem>>('/products', params)
}

export function fetchProductDetail(id: string) {
  return request.get<ProductItem>(`/products/${id}`)
}

export function createProduct(data: ProductPayload) {
  return request.post<string>('/products', data)
}

export function updateProduct(id: string, data: ProductPayload) {
  return request.put<void>(`/products/${id}`, data)
}

export function deleteProduct(id: string) {
  return request.delete<void>(`/products/${id}`)
}

export function updateProductStatus(id: string, status: number) {
  return request.patch<void>(`/products/${id}/status`, { status })
}