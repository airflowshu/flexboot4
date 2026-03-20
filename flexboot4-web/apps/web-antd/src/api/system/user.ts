import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

/**
 * 用户类型定义
 */
export interface UserRecord {
  id: string;
  username: string;
  realName: string;
  avatar?: string;
  email?: string;
  phone?: string;
  gender?: string;
  genderStr?: string;
  deptId?: string;
  status: number;
  remark?: string;
  createTime?: string;
  lastModifyTime?: string;
  createBy?: string;
  lastModifyBy?: string;
  dept?: DeptRecord;
  roles?: RoleRecord[];
}

export interface DeptRecord {
  id: string;
  parentId: string;
  deptName: string;
  orderNo: number;
  status: number;
  remark?: string;
  createTime?: string;
}

export interface RoleRecord {
  id: string;
  roleName: string;
  roleValue: string;
  status: number;
  orderNo: number;
  remark?: string;
  description?: string;
}

/**
 * 分页结果类型
 */
export interface PageResult<T> {
  pageNumber: number;
  pageSize: number;
  totalRow: number;
  totalPage: number;
  records: T[];
}

/**
 * 获取用户分页列表
 */
export function getUserPage(params: SearchRequest) {
  return requestClient.post<PageResult<UserRecord>>('/admin/user/page', params);
}

/**
 * 获取用户列表
 */
export function getUserList(params: SearchRequest) {
  return requestClient.post<UserRecord[]>('/admin/user/list', params);
}

/**
 * 获取用户详情
 */
export function getUserById(id: string) {
  return requestClient.get<UserRecord>(`/admin/user/${id}`);
}

/**
 * 获取当前用户信息
 */
export function getUserInfo() {
  return requestClient.get<Record<string, any>>('/admin/user/info');
}

/**
 * 新增用户
 */
export function createUser(data: Partial<UserRecord>) {
  return requestClient.post('/admin/user', data);
}

/**
 * 修改用户
 */
export function updateUser(id: string, data: Partial<UserRecord>) {
  return requestClient.put(`/admin/user/${id}`, data);
}

/**
 * 删除用户
 */
export function deleteUser(id: string) {
  return requestClient.delete<boolean>(`/admin/user/${id}`);
}

/**
 * 批量删除用户
 */
export function deleteUserBatch(ids: string[]) {
  return requestClient.delete('/admin/user', { data: ids });
}

/**
 * 获取部门列表（用于用户表单选择）
 */
export function getDeptList(params: SearchRequest) {
  return requestClient.post<DeptRecord[]>('/admin/dept/list', params);
}

/**
 * 获取角色列表（用于用户表单选择）
 */
export function getRoleList(params: SearchRequest) {
  return requestClient.post<RoleRecord[]>('/admin/role/list', params);
}

/**
 * 为用户分配角色（先清除该用户的所有角色关联，再批量新增）
 */
export function assignUserRole(userId: string, roleIds: string[]) {
  return requestClient.post(`/admin/user-role/assign/${userId}`, roleIds);
}

/**
 * 重置用户密码
 */
export function resetPasswordApi(data: {
  newPassword: string;
  userId?: string;
}) {
  return requestClient.post('/admin/auth/reset-password', data);
}

/**
 * 导出用户列表
 */
export function exportUsers(params: SearchRequest): Promise<Blob> {
  return requestClient.download('/admin/user/export', {
    method: 'POST',
    data: params,
  });
}
