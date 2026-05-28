import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

export interface {{entity}}CreateReq {
{{tsCreateFields}}
}

export interface {{entity}}UpdateReq {
{{tsUpdateFields}}
}

export interface {{entity}}ListVO {
{{tsListFields}}
}

export interface {{entity}}DetailVO extends {{entity}}ListVO {
{{tsDetailFields}}
}

export interface PageResult<T> {
  pageNumber: number;
  pageSize: number;
  totalRow: number;
  totalPage: number;
  records: T[];
}

export function page{{entity}}(params: SearchRequest) {
  return requestClient.post<PageResult<{{entity}}ListVO>>('{{frontendBasePath}}/page', params);
}

export function list{{entity}}(params: SearchRequest) {
  return requestClient.post<{{entity}}ListVO[]>('{{frontendBasePath}}/list', params);
}

export function get{{entity}}(id: string) {
  return requestClient.get<{{entity}}DetailVO>(`{{frontendBasePath}}/${id}`);
}

export function create{{entity}}(data: {{entity}}CreateReq) {
  return requestClient.post<boolean>('{{frontendBasePath}}', data);
}

export function update{{entity}}(id: string, data: {{entity}}UpdateReq) {
  return requestClient.put<boolean>(`{{frontendBasePath}}/${id}`, data);
}

export function delete{{entity}}(id: string) {
  return requestClient.delete<boolean>(`{{frontendBasePath}}/${id}`);
}
