import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

export interface SysConfig {
  id: string;
  configKey: string;
  configValue: string;
  configType: string;
  description?: string;
  status: number;
  statusStr?: string;
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
 * Get config page list
 */
export function getConfigPage(params: SearchRequest) {
  return requestClient.post<PageResult<SysConfig>>(
    '/admin/config/page',
    params,
  );
}

/**
 * Get config by ID
 */
export function getConfigById(id: string) {
  return requestClient.get<SysConfig>(`/admin/config/${id}`);
}

/**
 * Create config
 */
export function createConfig(data: Partial<SysConfig>) {
  return requestClient.post<boolean>('/admin/config', data);
}

/**
 * Update config
 */
export function updateConfig(id: string, data: Partial<SysConfig>) {
  return requestClient.put<boolean>(`/admin/config/${id}`, data);
}

/**
 * Delete config
 */
export function deleteConfig(id: string) {
  return requestClient.delete<boolean>(`/admin/config/${id}`);
}

/**
 * Batch delete configs
 */
export function batchDeleteConfig(ids: string[]) {
  return requestClient.delete<boolean>('/admin/config', { data: ids });
}

/**
 * Toggle config status (enable/disable)
 */
export function toggleConfigStatus(id: string, status: number) {
  return requestClient.put<boolean>(`/admin/config/${id}`, { status });
}
