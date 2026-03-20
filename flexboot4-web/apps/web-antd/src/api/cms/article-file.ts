import type { CmsArticle } from './article';
import type { CmsFileObject, CmsPageResult } from './types';

import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

export interface CmsArticleFile {
  id: string;
  articleId: string;
  fileId: string;
  sortOrder?: number;
  createTime?: string;
  lastModifyTime?: string;
  version?: number;
  article?: CmsArticle;
  file?: CmsFileObject;
}

export function getArticleFilePage(params: SearchRequest) {
  return requestClient.post<CmsPageResult<CmsArticleFile>>(
    '/admin/cms/article-file/page',
    params,
  );
}

export function createArticleFile(data: Partial<CmsArticleFile>) {
  return requestClient.post<boolean>('/admin/cms/article-file', data);
}

export function deleteArticleFile(id: string) {
  return requestClient.delete<boolean>(`/admin/cms/article-file/${id}`);
}
