import type { CmsCategory } from './category';
import type { CmsTag } from './tag';
import type { CmsFileObject, CmsPageResult } from './types';

import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

export type CmsArticleStatus = 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'REJECTED';

export interface CmsArticle {
  id: string;
  title: string;
  categoryId: string;
  createBy?: string;
  creatBy?: string;
  author?: string;
  coverFileId?: string;
  summary?: string;
  content?: string;
  status?: CmsArticleStatus;
  publishTime?: string;
  reviewComment?: string;
  sortOrder?: number;
  viewCount?: number;
  likeCount?: number;
  createTime?: string;
  lastModifyTime?: string;
  version?: number;
  category?: CmsCategory;
  coverFile?: CmsFileObject;
  tags?: CmsTag[];
}

export interface ReviewRequest {
  reviewComment?: string;
}

export function getArticlePage(params: SearchRequest) {
  return requestClient.post<CmsPageResult<CmsArticle>>(
    '/admin/cms/article/page',
    params,
  );
}

export function getArticleList(params: SearchRequest) {
  return requestClient.post<CmsArticle[]>('/admin/cms/article/list', params);
}

export function getArticleById(id: string) {
  return requestClient.get<CmsArticle>(`/admin/cms/article/${id}`);
}

export function createArticle(data: Partial<CmsArticle>) {
  return requestClient.post<boolean>('/admin/cms/article', data);
}

export function updateArticle(id: string, data: Partial<CmsArticle>) {
  return requestClient.put<boolean>(`/admin/cms/article/${id}`, data);
}

export function deleteArticle(id: string) {
  return requestClient.delete<boolean>(`/admin/cms/article/${id}`);
}

export function submitArticleForReview(id: string) {
  return requestClient.post<boolean>(`/admin/cms/article/${id}/submit`);
}

export function approveArticle(id: string, data: ReviewRequest) {
  return requestClient.post<boolean>(`/admin/cms/article/${id}/approve`, data);
}

export function rejectArticle(id: string, data: ReviewRequest) {
  return requestClient.post<boolean>(`/admin/cms/article/${id}/reject`, data);
}

export function incrementArticleViewCount(id: string) {
  return requestClient.post<boolean>(`/admin/cms/article/${id}/view`);
}
