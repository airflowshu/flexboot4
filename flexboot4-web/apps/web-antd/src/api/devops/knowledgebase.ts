import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

/**
 * 知识库类型
 */
export type KbType = 'private' | 'public' | 'team';

/**
 * 知识库状态
 */
export type KbStatus = 0 | 1;

/**
 * 知识库成员信息
 */
export interface KbMember {
  userId: string;
  username: string;
  realName: string;
  role: string;
  joinTime: string;
}

/**
 * 知识库文件关联信息
 */
export interface KbFileRelation {
  id: string;
  fileId: string;
  fileName: string;
  fileSize: number;
  fileType: string;
  indexed: boolean;
  indexedAt?: string;
  status: string;
  remark?: string;
  createTime?: string;
}

/**
 * 知识库
 */
export interface KnowledgeBase {
  id: string;
  name: string;
  description?: string;
  type: KbType;
  ownerId: string;
  ownerName?: string;
  embeddingModel?: string;
  status: KbStatus;
  remark?: string;
  createTime?: string;
  lastModifyTime?: string;
  createBy?: string;
  lastModifyBy?: string;
  memberCount?: number;
  fileCount?: number;
}

/**
 * 知识库列表请求参数
 */
export interface KnowledgeBaseSearch extends SearchRequest {
  name?: string;
  type?: KbType;
  status?: KbStatus;
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  pageNumber: number;
  pageSize: number;
  totalRow: number;
  totalPage?: number;
  records: T[];
}

/**
 * 获取知识库列表
 */
export function getKnowledgeBaseList(params?: KnowledgeBaseSearch) {
  return requestClient.post<KnowledgeBase[]>('/admin/kb/list', params || {});
}

/**
 * 获取知识库分页
 */
export function getKnowledgeBasePage(params: KnowledgeBaseSearch) {
  return requestClient.post<PageResult<KnowledgeBase>>(
    '/admin/kb/page',
    params,
  );
}

/**
 * 获取知识库详情
 */
export function getKnowledgeBaseById(id: string) {
  return requestClient.get<KnowledgeBase>(`/admin/kb/${id}`);
}

/**
 * 创建知识库
 */
export function createKnowledgeBase(data: Partial<KnowledgeBase>) {
  return requestClient.post<KnowledgeBase>('/admin/kb', data);
}

/**
 * 更新知识库
 */
export function updateKnowledgeBase(id: string, data: Partial<KnowledgeBase>) {
  return requestClient.put<boolean>(`/admin/kb/${id}`, data);
}

/**
 * 删除知识库
 */
export function deleteKnowledgeBase(id: string) {
  return requestClient.delete<boolean>(`/admin/kb/${id}`);
}

/**
 * 手动触发知识库索引
 */
export function triggerKnowledgeBaseIndex(kbId: string) {
  return requestClient.post<boolean>(`/admin/kb/${kbId}/index`);
}

/**
 * 获取知识库成员列表
 */
export function getKnowledgeBaseMembers(kbId: string) {
  return requestClient.get<KbMember[]>(`/admin/kb/${kbId}/members`);
}

/**
 * 添加知识库成员
 */
export function addKnowledgeBaseMember(kbId: string, userIds: string[]) {
  return requestClient.post<boolean>(`/admin/kb/${kbId}/members`, { userIds });
}

/**
 * 移除知识库成员
 */
export function removeKnowledgeBaseMember(kbId: string, userId: string) {
  return requestClient.delete<boolean>(`/admin/kb/${kbId}/members`, {
    data: { userId },
  });
}

/**
 * 获取知识库关联文件列表
 */
export function getKnowledgeBaseFiles(kbId: string) {
  return requestClient.get<KbFileRelation[]>(`/admin/kb/${kbId}/files`);
}

/**
 * 关联文件到知识库
 */
export function addKnowledgeBaseFile(kbId: string, fileIds: string[]) {
  return requestClient.post<boolean>(`/admin/kb/${kbId}/files`, { fileIds });
}

/**
 * 取消知识库文件关联
 */
export function removeKnowledgeBaseFile(kbId: string, fileId: string) {
  return requestClient.delete<boolean>(`/admin/kb/${kbId}/files`, {
    data: { fileId },
  });
}

/**
 * 知识库文件/文件夹项
 */
export interface KbFileItem {
  id: string;
  parentId?: null | string;
  name?: string;
  fileName?: string;
  isFolder: number;
  fileSize?: number;
  fileExt?: null | string;
  createTime?: string;
  lastModifyTime?: string;
  aiStatus?: string;
  aiParseStatus?: null | string;
  aiEmbedStatus?: null | string;
  chunkCount?: number;
  bizId?: null | string;
  bizType?: null | string;
  bucketName?: null | string;
  createBy?: string;
  embeddingModel?: null | string;
  fileHash?: null | string;
  lastModifyBy?: null | string;
  mimeType?: null | string;
  objectKey?: null | string;
  remark?: null | string;
  storageType?: string;
  tenantId?: string;
  tokenEstimate?: null | string;
  version?: null | string;
  kbId?: string;
}

/**
 * 列表响应
 */
export interface KbFileListResponse {
  code: number;
  data: KbFileItem[];
}

/**
 * 新建文件夹请求
 */
export interface FolderCreateRequest {
  parentId?: string;
  name: string;
}

/**
 * 列出目录下文件/文件夹
 * 由于 requestClient 配置了 responseReturn: 'data'，直接返回数组
 */
export function listKbFiles(kbId: string, parentId?: string) {
  return requestClient.get<KbFileItem[]>(`/admin/kb/${kbId}/fs/list`, {
    params: { parentId },
  });
}

/**
 * 新建文件夹
 */
export function createKbFolder(kbId: string, data: FolderCreateRequest) {
  return requestClient.post<boolean>(`/admin/kb/${kbId}/fs/folder`, data);
}

/**
 * 删除文件/文件夹
 */
export function deleteKbFile(kbId: string, id: string) {
  return requestClient.delete<boolean>(`/admin/kb/${kbId}/fs/delete/${id}`);
}

/**
 * 重命名文件/文件夹
 */
export function renameKbFile(kbId: string, id: string, newName: string) {
  return requestClient.put<boolean>(`/admin/kb/${kbId}/fs/rename/${id}`, {
    name: newName,
  });
}

/**
 * 手动触发知识库索引
 */
export function buildKbIndex(kbId: string, fileTreeIds: string[]) {
  return requestClient.post<number>(`/admin/kb/${kbId}/index`, fileTreeIds);
}

/**
 * 移动文件/文件夹
 */
export function moveKbFile(kbId: string, id: string, targetParentId: string) {
  return requestClient.post<boolean>(`/admin/kb/${kbId}/fs/move/${id}`, {
    targetParentId,
  });
}

/**
 * 文件上传请求参数
 */
export interface FileUploadParams {
  kbId: string;
  parentId?: string;
  bizType?: string;
  bizId?: string;
}

/**
 * 上传文件
 */
export function uploadFile(params: FileUploadParams, file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return requestClient.post<{ id: string; name: string }>(
    '/admin/file/upload-single',
    formData,
    {
      params: {
        kbId: params.kbId,
        parentId: params.parentId,
        bizType: params.bizType,
        bizId: params.bizId,
      },
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  );
}

/**
 * 批量上传文件到知识库
 */
export function uploadFiles(kbId: string, files: File[], parentId?: string) {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append('files', file);
  });
  if (parentId) {
    formData.append('parentId', parentId);
  }
  return requestClient.post<string[]>(`/admin/kb/${kbId}/files/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

/**
 * 目录树节点类型
 */
export type TreeNodeType = 'folder' | 'file';

/**
 * 目录树节点
 */
export interface KbTreeNode {
  id: string;
  parentId?: string | null;
  name: string;
  type: TreeNodeType;
  children?: KbTreeNode[];
  isLeaf?: boolean;
  expanded?: boolean;
  fileExt?: string | null;
  fileSize?: number;
  aiStatus?: string;
  createTime?: string;
  lastModifyTime?: string;
  loading?: boolean;
}

/**
 * 创建文件夹请求
 */
export interface CreateFolderRequest {
  parentId?: string;
  name: string;
}

/**
 * 获取目录树
 */
export function getKbTree(kbId: string) {
  return requestClient.get<KbTreeNode[]>(`/admin/kb/${kbId}/tree`);
}

/**
 * 获取目录树子节点
 */
export function getKbTreeChildren(kbId: string, parentId?: string) {
  return requestClient.get<KbTreeNode[]>(`/admin/kb/${kbId}/tree/children`, {
    params: { parentId },
  });
}

/**
 * 创建文件夹
 */
export function createKbTreeFolder(kbId: string, data: CreateFolderRequest) {
  return requestClient.post(`/admin/kb/${kbId}/tree/folders`, data);
}

/**
 * 删除目录树节点
 */
export function deleteKbTreeNode(kbId: string, id: string) {
  return requestClient.delete(`/admin/kb/${kbId}/tree/${id}`);
}

/**
 * 重命名目录树节点
 */
export function renameKbTreeNode(kbId: string, id: string, name: string) {
  return requestClient.post(`/admin/kb/${kbId}/tree/${id}/rename`, { name });
}

/**
 * 文件列表项
 */
export interface KbFileListItem {
  id: string;
  kbId: string;
  parentId: string | null;
  name: string;
  type: 'FILE' | 'FOLDER';
  fileId?: string | null;
  sortOrder: number;
  delFlag: number;
}

/**
 * 获取文件列表
 */
export function getKbFileList(kbId: string, parentId?: string) {
  return requestClient.get<KbFileListItem[]>(
    `/admin/kb/${kbId}/file-list`,
    {
      params: { parentId },
    },
  );
}
