import type { RouteRecordStringComponent } from '@vben/types';

import { requestClient } from '#/api/request';

/**
 * 获取用户所有菜单
 */
export async function getAllMenusApi() {
  const data =
    await requestClient.get<RouteRecordStringComponent[]>('/admin/menu/all');
  function normalize(
    list: null | RouteRecordStringComponent[] | undefined,
  ): RouteRecordStringComponent[] {
    if (!Array.isArray(list)) return [];
    return list
      .map((item) => {
        let path: null | string = null;
        if (item.path && item.path.length > 0) {
          path = item.path.startsWith('/') ? item.path : `/${item.path}`;
        }
        if (path === null) return null;
        const name =
          typeof item.name === 'string' && item.name.length > 0
            ? item.name
            : path.slice(1).replaceAll('/', '_');
        const children = normalize(item.children);
        return {
          ...item,
          name,
          path,
          children: children.length > 0 ? children : undefined,
        } as any as RouteRecordStringComponent;
      })
      .filter((v): v is RouteRecordStringComponent => v !== null);
  }
  return normalize(data);
}
