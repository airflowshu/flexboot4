import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

/**
 * 角色类型定义
 */
export interface RoleRecord {
  id: string;
  roleName: string;
  roleValue: string;
  status: number;
  remark?: string;
  createTime?: string;
  lastModifyTime?: string;
  createBy?: string;
  lastModifyBy?: string;
  menus?: MenuTreeNode[];
}

/**
 * 角色菜单关联类型定义
 */
export interface RoleMenuRecord {
  id: string;
  roleId: string;
  menuId: string;
  remark?: string;
  createTime?: string;
  lastModifyTime?: string;
  createBy?: string;
  lastModifyBy?: string;
}

/**
 * 菜单树节点类型
 */
export interface MenuTreeNode {
  id: string;
  parentId: string | null;
  title: string;
  name: string;
  type: string;
  icon?: string;
  children?: MenuTreeNode[];
  key: string;
  halfChecked?: boolean;
  checked?: boolean;
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
 * 获取角色分页列表
 */
export function getRolePage(params: SearchRequest) {
  return requestClient.post<PageResult<RoleRecord>>('/admin/role/page', params);
}

/**
 * 获取角色列表
 */
export function getRoleList(params: SearchRequest) {
  return requestClient.post<RoleRecord[]>('/admin/role/list', params);
}

/**
 * 获取角色详情
 */
export function getRoleById(id: string) {
  return requestClient.get<RoleRecord>(`/admin/role/${id}`);
}

/**
 * 新增角色
 */
export function createRole(data: Partial<RoleRecord>) {
  return requestClient.post('/admin/role', data);
}

/**
 * 修改角色
 */
export function updateRole(id: string, data: Partial<RoleRecord>) {
  return requestClient.put(`/admin/role/${id}`, data);
}

/**
 * 删除角色
 */
export function deleteRole(id: string) {
  return requestClient.delete<boolean>(`/admin/role/${id}`);
}

/**
 * 获取角色关联的菜单ID列表
 */
export function getRoleMenuIds(roleId: string) {
  return requestClient.get<string[]>(`/admin/role-menu/${roleId}`);
}

/**
 * 保存角色菜单权限
 * @param roleId 角色ID
 * @param menuIds 菜单ID数组
 */
export function saveRoleMenus(roleId: string, menuIds: string[]) {
  return requestClient.post(`/admin/role-menu/assign/${roleId}`, menuIds);
}

/**
 * 获取菜单树列表（用于权限分配）
 */
export function getMenuTree(params?: SearchRequest) {
  return requestClient.post<MenuTreeNode[]>('/admin/menu/list', params ?? {});
}
