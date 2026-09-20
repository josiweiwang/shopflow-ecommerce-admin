import request from '@/utils/request'
import type { OperationLogRow, PageQuery, PageResult, RoleOption, UserQuery, UserRow } from '@/types'

/** 后台用户与操作日志接口 */

export function fetchUserPage(params: UserQuery) {
  return request.get<PageResult<UserRow>>('/admin/users', params)
}

export function updateUserStatus(id: string, status: number) {
  return request.patch<void>(`/admin/users/${id}/status`, { status })
}

export function assignUserRoles(id: string, roleIds: string[]) {
  return request.put<void>(`/admin/users/${id}/roles`, { roleIds })
}

export function fetchRoleOptions() {
  return request.get<RoleOption[]>('/admin/users/roles')
}

export interface OperationLogQuery extends PageQuery {
  keyword?: string
  module?: string
  success?: number
}

export function fetchOperationLogs(params: OperationLogQuery) {
  return request.get<PageResult<OperationLogRow>>('/admin/operation-logs', params)
}