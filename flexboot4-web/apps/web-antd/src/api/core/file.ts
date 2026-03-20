import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

/**
 * 文件上传参数
 */
export interface UploadFileParams {
  /** 业务ID */
  bizId?: string;
  /** 业务类型，传入 `sys_user_avatar` 时文件会上传到公有库 */
  bizType?: string;
  /** 文件对象 */
  file: File;
  /** 项目ID */
  projectId?: string;
}

/**
 * 文件对象
 */
export interface FileObject {
  /** AI状态 */
  aiStatus?: string;
  /** 业务ID */
  bizId?: string;
  /** 业务类型 */
  bizType?: string;
  /** 分块数量 */
  chunkCount?: number;
  /** 嵌入模型 */
  embeddingModel?: string;
  /** 文件扩展名 */
  fileExt?: string;
  /** 文件名 */
  fileName?: string;
  /** 文件hash */
  fileHash?: string;
  /** 文件大小 */
  fileSize?: number;
  /** ID */
  id?: string;
  /** 文件位置信息 */
  location?: {
    bucket?: string;
    endpoint?: string;
    objectKey?: string;
    region?: string;
    storageType?: 'ALI_OSS' | 'LOCAL' | 'MINIO' | 'S3';
  };
  /** Mime类型 */
  mimeType?: string;
  /** 项目ID */
  projectId?: string;
  /** 租户ID */
  tenantId?: string;
  /** Token估算 */
  tokenEstimate?: number;
}

/**
 * 上传文件
 *
 * @param params 上传参数
 * @returns 文件信息
 */
export async function uploadSingleFileApi(params: UploadFileParams) {
  // 使用 requestClient.upload 方法，它会自动处理 FormData
  // 为了解决 "Current request is not a multipart request" 问题：
  // 我们显式将 Content-Type 设为 undefined，让浏览器自动生成带 boundary 的 Header
  return requestClient.upload<FileObject>('/admin/file/upload-single', params, {
    headers: {
      'Content-Type': undefined,
    },
  });
}

/**
 * 分页查询文件
 */
export function getFilePage(params: SearchRequest) {
  return requestClient.post<{
    records: FileObject[];
    totalRow: number;
  }>('/admin/file/page', params);
}

/**
 * 文件访问描述符
 */
export interface FileAccessDescriptor {
  /** 下载显示方式 */
  disposition?: string;
  /** 过期时间 */
  expireAt?: string;
  /** 文件访问URL */
  url?: string;
  /** 访问令牌 */
  token?: string;
}

/**
 * 批量删除文件
 */
export function deleteFiles(ids: string[]) {
  return requestClient.delete('/admin/file', { data: ids });
}

/**
 * 获取文件访问地址
 *
 * @param id 文件ID
 * @param ttlSeconds 有效期（秒），默认600
 * @param attachment 是否作为附件下载，默认true
 * @returns 文件访问描述符
 */
export function getFileAccessUrl(
  id: string,
  ttlSeconds?: number,
  attachment?: boolean,
) {
  return requestClient.get<FileAccessDescriptor>(`/admin/file/${id}/access-url`, {
    params: {
      ttlSeconds,
      attachment,
    },
  });
}
