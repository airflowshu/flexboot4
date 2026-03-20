import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

/**
 * 菜单类型定义
 * 参照 response.json 结构
 */
export interface DeptRecord {
  version: number;
  status: number;
  id: string;
  parentId: string;
  deptName: string;
  orderNo: number;
  remark: string;
  children?: DeptRecord[];
  createTime?: string;
}

/**
 * 获取菜单列表
 */
export function getDeptList(params: SearchRequest) {
  return requestClient.post<DeptRecord[]>('/admin/dept/list', params);
}

/**
 * 删除菜单
 */
export function deleteDept(id: string) {
  return requestClient.delete<boolean>(`/admin/dept/${id}`);
}

/**
 * 新增菜单
 */
export function createDept(params: Partial<DeptRecord>) {
  return requestClient.post('/admin/dept', params);
}

/**
 * 修改菜单
 */
export function updateDept(id: string, params: Partial<DeptRecord>) {
  return requestClient.put(`/admin/dept/${id}`, params);
}
