import type { CmsPageResult } from './types';

import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

export interface CmsTag {
  id: string;
  tagName: string;
  tagColor?: string;
  useCount?: number;
  createTime?: string;
  lastModifyTime?: string;
  version?: number;
}

export function getTagPage(params: SearchRequest) {
  return requestClient.post<CmsPageResult<CmsTag>>(
    '/admin/cms/tag/page',
    params,
  );
}

export function getTagList(params: SearchRequest) {
  return requestClient.post<CmsTag[]>('/admin/cms/tag/list', params);
}

export function createTag(data: Partial<CmsTag>) {
  return requestClient.post<boolean>('/admin/cms/tag', data);
}

export function updateTag(id: string, data: Partial<CmsTag>) {
  return requestClient.put<boolean>(`/admin/cms/tag/${id}`, data);
}

export function deleteTag(id: string) {
  return requestClient.delete<boolean>(`/admin/cms/tag/${id}`);
}
