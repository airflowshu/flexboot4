import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

export interface OperLog {
  id: string;
  title: string;
  businessType: number;
  businessTypeStr: string;
  method: string;
  requestMethod: string;
  operatorType: number;
  operatorTypeStr: string;
  operName: string;
  operUserId: string;
  deptId: string;
  operUrl: string;
  operIp: string;
  operLocation: string;
  operParam: Record<string, any>;
  jsonResult: Record<string, any>;
  status: number;
  errorMsg: string;
  operTime: string;
  costTime: number;
  extParams: Record<string, any>;
  remark: string;
}

export interface PageResult<T> {
  pageNumber: number;
  pageSize: number;
  totalRow: number;
  records: T[];
}

/**
 * Get oper log page list
 */
export function getOperLogPage(params: SearchRequest) {
  return requestClient.post<PageResult<OperLog>>('/admin/oper-log/page', params);
}
