import type { CmsPageResult } from './types';

import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

export interface CmsCategory {
  id: string;
  categoryName: string;
  categoryCode: string;
  description?: string;
  parentId?: null | string;
  coverFileId?: string;
  sortOrder?: number;
  status?: number;
  createTime?: string;
  lastModifyTime?: string;
  version?: number;
}

export function getCategoryPage(params: SearchRequest) {
  return requestClient.post<CmsPageResult<CmsCategory>>(
    '/admin/cms/category/page',
    params,
  );
}

export function getCategoryList(params: SearchRequest) {
  return requestClient.post<CmsCategory[]>('/admin/cms/category/list', params);
}

export function createCategory(data: Partial<CmsCategory>) {
  return requestClient.post<boolean>('/admin/cms/category', data);
}

export function updateCategory(id: string, data: Partial<CmsCategory>) {
  return requestClient.put<boolean>(`/admin/cms/category/${id}`, data);
}

export function deleteCategory(id: string) {
  return requestClient.delete<boolean>(`/admin/cms/category/${id}`);
}
