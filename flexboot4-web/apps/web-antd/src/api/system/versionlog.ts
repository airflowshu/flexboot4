import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

export interface SysVersionLog {
  id: string;
  createTime?: string;
  lastModifyTime?: string;
  createBy?: string;
  lastModifyBy?: string;
  remark?: string;
  versionNo?: string;
  releaseDate?: string;
  type?: string;
  typeStr?: string;
  title?: string;
  description?: string;
  status?: number;
  features?: string[];
  fixes?: string[];
}

export interface PageResult<T> {
  pageNumber: number;
  pageSize: number;
  totalRow: number;
  totalPage?: number;
  records: T[];
}

export function getVersionLogPage(params: SearchRequest) {
  return requestClient.post<PageResult<SysVersionLog>>(
    '/admin/version-log/page',
    params,
  );
}

export function getVersionLogList(params: SearchRequest) {
  return requestClient.post<SysVersionLog[]>('/admin/version-log/list', params);
}

export function createVersionLog(data: Partial<SysVersionLog>) {
  return requestClient.post<boolean>('/admin/version-log', data);
}

export function updateVersionLog(id: string, data: Partial<SysVersionLog>) {
  return requestClient.put<boolean>(`/admin/version-log/${id}`, data);
}

export function deleteVersionLog(id: string) {
  return requestClient.delete<boolean>(`/admin/version-log/${id}`);
}
