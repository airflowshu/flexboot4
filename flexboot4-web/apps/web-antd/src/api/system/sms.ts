import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

export interface SmsConfig {
  id: string;
  createTime?: string;
  lastModifyTime?: string;
  createBy?: string;
  lastModifyBy?: string;
  remark?: string;
  configName: string;
  supplierType: string;
  supplierTypeStr?: string;
  configId: string;
  accessKeyId?: string;
  accessKeySecret?: string;
  signature?: string;
  templateId?: string;
  sdkAppId?: string;
  weight?: number;
  isDefault?: number;
  extParams?: string;
  status: number;
  statusStr?: string;
}

/**
 * Get sms config list.
 */
export function getSmsConfigList(params: SearchRequest) {
  return requestClient.post<SmsConfig[]>('/admin/sms/config/list', params);
}

/**
 * Update a sms config.
 */
export function updateSmsConfig(id: string, data: Partial<SmsConfig>) {
  return requestClient.put<boolean>(`/admin/sms/config/${id}`, data);
}
