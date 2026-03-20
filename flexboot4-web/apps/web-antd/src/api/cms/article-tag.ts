import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

export interface CmsArticleTag {
  id: string;
  articleId: string;
  tagId: string;
  createTime?: string;
  lastModifyTime?: string;
  version?: number;
}

export function getArticleTagList(params: SearchRequest) {
  return requestClient.post<CmsArticleTag[]>(
    '/admin/cms/article-tag/list',
    params,
  );
}

export function createArticleTag(data: Partial<CmsArticleTag>) {
  return requestClient.post<boolean>('/admin/cms/article-tag', data);
}

export function deleteArticleTag(id: string) {
  return requestClient.delete<boolean>(`/admin/cms/article-tag/${id}`);
}
