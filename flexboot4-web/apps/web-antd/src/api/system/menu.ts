import type { Recordable } from '@vben/types';

import type { SearchRequest } from '#/api/common';

import { requestClient } from '#/api/request';

export namespace SystemMenuApi {
  /** 徽标颜色集合 */
  export const BadgeVariants = [
    'default',
    'destructive',
    'primary',
    'success',
    'warning',
  ] as const;
  /** 徽标类型集合 */
  export const BadgeTypes = ['dot', 'normal'] as const;
  /** 菜单类型集合 */
  export const MenuTypes = [
    'catalog',
    'menu',
    'embedded',
    'link',
    'button',
  ] as const;
  /** 系统菜单 */
  export interface SystemMenu {
    [key: string]: any;
    /** 后端权限标识 */
    authCode?: string;
    /** 子级 */
    children?: SystemMenu[];
    /** 组件 */
    component?: string;
    /** 菜单ID */
    id: string;
    version: number;
    /** 菜单元数据（用于列表展示） */
    meta?: {
      /** 激活时显示的图标 */
      activeIcon?: string;
      /** 作为路由时，需要激活的菜单的Path */
      activePath?: string;
      /** 固定在标签栏 */
      affixTab?: boolean;
      /** 在标签栏固定的顺序 */
      affixTabOrder?: number;
      /** 徽标内容(当徽标类型为normal时有效) */
      badge?: string;
      /** 徽标类型 */
      badgeType?: (typeof BadgeTypes)[number];
      /** 徽标颜色 */
      badgeVariants?: (typeof BadgeVariants)[number];
      /** 在菜单中隐藏下级 */
      hideChildrenInMenu?: boolean;
      /** 在面包屑中隐藏 */
      hideInBreadcrumb?: boolean;
      /** 在菜单中隐藏 */
      hideInMenu?: boolean;
      /** 在标签栏中隐藏 */
      hideInTab?: boolean;
      /** 菜单图标 */
      icon?: string;
      /** 内嵌Iframe的URL */
      iframeSrc?: string;
      /** 是否缓存页面 */
      keepAlive?: boolean;
      /** 外链页面的URL */
      link?: string;
      /** 同一个路由最大打开的标签数 */
      maxNumOfOpenTab?: number;
      /** 无需基础布局 */
      noBasicLayout?: boolean;
      /** 是否在新窗口打开 */
      openInNewWindow?: boolean;
      /** 菜单排序 */
      order?: number;
      /** 额外的路由参数 */
      query?: Recordable<any>;
      /** 菜单标题 */
      title?: string;
      version: number;
    };
    /** 菜单名称 */
    name: string;
    /** 路由路径 */
    path: string;
    /** 父级ID（提交给后端使用的字段） */
    parentId?: string;
    /** 重定向 */
    redirect?: string;
    /** 菜单类型 */
    type: (typeof MenuTypes)[number];

    // 平铺的元数据字段（后端接口返回的格式）
    title?: string;
    icon?: string;
    activeIcon?: string;
    badge?: string;
    badgeType?: string;
    badgeVariants?: string;
    keepAlive?: boolean;
    affixTab?: boolean;
    hideMenu?: boolean;
    hideChildrenInMenu?: boolean;
    hideBreadcrumb?: boolean;
    hideTab?: boolean;
    orderNo?: number;
  }
}

/**
 * 获取菜单数据列表
 */
async function getMenuList(params?: SearchRequest) {
  const payload =
    params ??
    ({
      orders: [],
      pageNumber: 1,
      pageSize: 10,
    } as SearchRequest);
  const raw = await requestClient.post<Array<Recordable<any>>>(
    '/admin/menu/list',
    payload,
  );
  const mapType = (v: unknown): SystemMenuApi.SystemMenu['type'] => {
    if (
      typeof v === 'string' &&
      ['button', 'catalog', 'embedded', 'link', 'menu'].includes(v)
    ) {
      return v as SystemMenuApi.SystemMenu['type'];
    }
    return 'menu';
  };
  return Array.isArray(raw)
    ? raw.map((it) => {
        const titleCandidate =
          typeof it.title === 'string' && it.title.length > 0
            ? it.title
            : typeof it.name === 'string' && it.name.length > 0
              ? it.name
              : // eslint-disable-next-line unicorn/no-nested-ternary
                typeof it.path === 'string' && it.path.length > 0
                ? it.path
                : 'Unknown';
        const meta: Recordable<any> = {
          title: titleCandidate,
          icon: it.icon,
          version: it.version,
          activeIcon: it.activeIcon,
          keepAlive: it.keepAlive,
          hideInMenu: it.hideMenu,
          hideInTab: it.hideTab,
          hideInBreadcrumb: it.hideBreadcrumb,
          hideChildrenInMenu: it.hideChildrenInMenu,
          iframeSrc: it.iframeSrc,
          link: it.link,
          badgeType: it.badgeType,
          badge: it.badge,
          badgeVariants: it.badgeVariants,
          order: it.orderNo ?? it.order,
        };
        // 转换 parentId 为 '0' 的节点为根节点
        const parentId = it.parentId === '0' ? null : it.parentId;
        return {
          ...it,
          parentId,
          meta,
          type: mapType(it.type),
          children: undefined,
        } as SystemMenuApi.SystemMenu;
      })
    : [];
}

async function isMenuNameExists(
  name: string,
  id?: SystemMenuApi.SystemMenu['id'],
) {
  return requestClient.get<boolean>('/admin/menu/name-exists', {
    params: { id, name },
  });
}

async function isMenuPathExists(
  path: string,
  id?: SystemMenuApi.SystemMenu['id'],
) {
  return requestClient.get<boolean>('/admin/menu/path-exists', {
    params: { id, path },
  });
}

/**
 * 创建菜单
 * @param data 菜单数据
 */
async function createMenu(
  data: Omit<SystemMenuApi.SystemMenu, 'children' | 'id'>,
) {
  return requestClient.post('/admin/menu', data);
}

/**
 * 更新菜单
 *
 * @param id 菜单 ID
 * @param data 菜单数据
 */
async function updateMenu(
  id: string,
  data: Omit<SystemMenuApi.SystemMenu, 'children' | 'id'>,
) {
  return requestClient.put(`/admin/menu/${id}`, data);
}

/**
 * 删除菜单
 * @param id 菜单 ID
 */
async function deleteMenu(id: string) {
  return requestClient.delete<boolean>(`/admin/menu/${id}`);
}

export {
  createMenu,
  deleteMenu,
  getMenuList,
  isMenuNameExists,
  isMenuPathExists,
  updateMenu,
};
