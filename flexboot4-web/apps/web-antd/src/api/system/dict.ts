import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

export interface DictItem {
  id: string;
  itemText: string;
  itemValue: string;
  itemCode: string; // From json: itemCode (e.g. "china")
  orderNo: number;
  status: number; // 1: enabled, 0: disabled
  remark?: string;
  parentCode?: null | string;
  typeCode?: string;
  createTime?: string;
  version: number;
}

export interface DictType {
  id: string;
  name: string;
  code: string;
  orderNo: number;
  status: number;
  remark?: string;
  createTime?: string;
  dictItems?: DictItem[];
  version: number;
}

export interface PageResult<T> {
  pageNumber: number;
  pageSize: number;
  totalRow: number;
  records: T[];
}

/**
 * Get dict type page list
 */
export function getDictTypePage(params: SearchRequest) {
  return requestClient.post<PageResult<DictType>>('/admin/dict-type/page', params);
}

/**
 * Create dict type
 */
export function createDictType(data: Partial<DictType>) {
  return requestClient.post('/admin/dict-type', data);
}

/**
 * Update dict type
 */
export function updateDictType(id: string, data: Partial<DictType>) {
  return requestClient.put(`/admin/dict-type/${id}`, data);
}

/**
 * Delete dict type
 */
export function deleteDictType(id: string) {
  return requestClient.delete<boolean>(`/admin/dict-type/${id}`);
}

/**
 * Get dict item list by type id or code
 */
export function getDictItemList(params: SearchRequest) {
  return requestClient.post<DictItem[]>('/admin/dict-item/list', params);
}

/**
 * Create dict item
 */
export function createDictItem(data: Partial<DictItem>) {
  return requestClient.post('/admin/dict-item', data);
}

/**
 * Update dict item
 */
export function updateDictItem(id: string, data: Partial<DictItem>) {
  return requestClient.put(`/admin/dict-item/${id}`, data);
}

/**
 * Delete dict item
 */
export function deleteDictItem(id: string) {
  return requestClient.delete(`/admin/dict-item/${id}`);
}
