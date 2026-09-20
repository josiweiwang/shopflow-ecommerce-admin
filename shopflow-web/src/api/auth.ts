import request from '@/utils/request'
import type { LoginResult, UserInfo } from '@/types'

/** 认证相关接口 */

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
}

export function login(data: LoginPayload) {
  return request.post<LoginResult>('/auth/login', data)
}

export function register(data: RegisterPayload) {
  return request.post<void>('/auth/register', data)
}

export function logout() {
  return request.post<void>('/auth/logout')
}

export function getCurrentUser() {
  return request.get<UserInfo>('/auth/me')
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request.put<void>('/auth/password', data)
}

export function updateProfile(data: {
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
}) {
  return request.put<void>('/auth/profile', data)
}