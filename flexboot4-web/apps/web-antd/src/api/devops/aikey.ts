import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

export interface AiApiKey {
  id: string;
  keyName: string;
  apiKey: string;
  userId: string;
  status: number;
  statusStr: string;
  quote: number;
  used: number;
  modelScope: string;
  expiresAt?: string;
  lastUsedTime?: string;
  notes?: string;
  remark?: string;
  createTime?: string;
  lastModifyTime?: string;
  createBy?: string;
  lastModifyBy?: string;
}

export interface PageResult<T> {
  pageNumber: number;
  pageSize: number;
  totalRow: number;
  totalPage?: number;
  records: T[];
}

/**
 * Get API Key page list
 */
export function getApiKeyPage(params: SearchRequest) {
  return requestClient.post<PageResult<AiApiKey>>('/admin/api-key/page', params);
}

/**
 * Get API Key by ID
 */
export function getApiKeyById(id: string) {
  return requestClient.get<AiApiKey>(`/admin/api-key/${id}`);
}

/**
 * Create API Key
 */
export function createApiKey(data: Partial<AiApiKey>) {
  return requestClient.post<boolean>('/admin/api-key', data);
}

/**
 * Update API Key
 */
export function updateApiKey(id: string, data: Partial<AiApiKey>) {
  return requestClient.put<boolean>(`/admin/api-key/${id}`, data);
}

/**
 * Delete API Key
 */
export function deleteApiKey(id: string) {
  return requestClient.delete<boolean>(`/admin/api-key/${id}`);
}

/**
 * Batch delete API Keys
 */
export function batchDeleteApiKey(ids: string[]) {
  return requestClient.delete<boolean>('/admin/api-key', { data: ids });
}

/**
 * Toggle API Key status (enable/disable)
 */
export function toggleApiKeyStatus(id: string, status: number) {
  return requestClient.put<boolean>(`/admin/api-key/${id}`, { status });
}

/**
 * 用户选项类型（用于下拉选择）
 */
export interface UserOption {
  id: string;
  username: string;
  realName: string;
}

/**
 * 获取用户列表（用于创建 API Key 时选择用户）
 */
export function getUserOptions() {
  return requestClient.get<UserOption[]>('/admin/api-key/orphaned-users', {});
}
