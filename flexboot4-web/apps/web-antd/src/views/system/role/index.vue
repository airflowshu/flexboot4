<script lang="ts" setup>
import type { VxeGridListeners, VxeGridProps } from '#/adapter/vxe-table';
import type { SearchRequest } from '#/api/common';
import type { MenuTreeNode, RoleRecord } from '#/api/system/role';

import { ref } from 'vue';

import { AccessControl } from '@vben/access';
import { ColPage, useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Input,
  message,
  Modal,
  Select,
  Spin,
  Tag,
  Tree,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { buildSearchGroup, buildSearchItem } from '#/api/common';
import {
  deleteRole,
  getMenuTree,
  getRoleMenuIds,
  getRolePage,
  saveRoleMenus,
} from '#/api/system/role';
import { $t } from '#/locales';

import RoleDrawer from './role-drawer.vue';

defineOptions({ name: 'SystemRole' });
// 搜索关键词
const searchKeyword = ref('');
// 选中的状态
const selectedStatus = ref<'' | number>('');
// 状态选项
const statusOptions = [
  { label: '全部', value: '' },
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
];

// 当前选中的角色
const selectedRole = ref<null | RoleRecord>(null);
// 菜单树数据
const menuTreeData = ref<MenuTreeNode[]>([]);
// 加载状态
const loadingMenu = ref(false);

// 右侧已选中的菜单Keys
const checkedMenuKeys = ref<string[]>([]);
// 右侧半选中的菜单Keys（父节点部分选中）
const halfCheckedMenuKeys = ref<string[]>([]);
// 默认展开的菜单Keys（根节点）
const expandedMenuKeys = ref<string[]>([]);

// 构建搜索参数
const buildSearchParams = (): Partial<SearchRequest> => {
  const items: any[] = [];

  if (searchKeyword.value) {
    items.push(
      buildSearchGroup('OR', [
        buildSearchItem('sysRole.roleName', 'like', searchKeyword.value),
        buildSearchItem('sysRole.roleValue', 'like', searchKeyword.value),
        buildSearchItem('sysRole.remark', 'like', searchKeyword.value),
      ]),
    );
  }

  if (selectedStatus.value !== '') {
    items.push(buildSearchItem('sysRole.status', 'eq', selectedStatus.value));
  }

  return {
    pageNumber: 1,
    pageSize: 10,
    logic: 'AND',
    items: items.length > 0 ? items : undefined,
    orders: [{ column: 'sysRole.lastModifyTime', asc: false }],
  };
};

const gridOptions: VxeGridProps<RoleRecord> = {
  columns: [
    {
      field: 'roleName',
      title: '角色名称',
      width: 150,
    },
    {
      field: 'roleValue',
      title: '角色编码',
      width: 150,
    },
    {
      field: 'status',
      title: '状态',
      width: 80,
      slots: { default: 'status' },
    },
    {
      field: 'remark',
      title: '备注',
      minWidth: 150,
    },
    {
      field: 'createTime',
      title: '创建时间',
      width: 180,
      formatter: ({ row }) =>
        row.createTime
          ? dayjs(row.createTime).format('YYYY-MM-DD HH:mm:ss')
          : '-',
    },
    {
      field: 'action',
      fixed: 'right',
      title: '操作',
      width: 200,
      slots: { default: 'action' },
    },
  ],
  pagerConfig: {
    enabled: true,
  },
  proxyConfig: {
    ajax: {
      query: async ({ page }) => {
        try {
          const params = buildSearchParams();
          params.pageNumber = page.currentPage;
          params.pageSize = page.pageSize;

          const resp = await getRolePage(params as SearchRequest);
          return {
            items: resp.records,
            total: resp.totalRow,
          };
        } catch (error) {
          console.error(error);
          return { items: [], total: 0 };
        }
      },
    },
  },
  rowConfig: {
    keyField: 'id',
    isHover: true,
    isCurrent: true,
  },
  rowClassName: ({ row }) => {
    return selectedRole.value?.id === row.id ? 'row-current-row' : '';
  },
  toolbarConfig: {
    export: true,
    refresh: true,
    resizable: true,
    zoom: true,
    slots: {
      buttons: 'toolbar-buttons',
    },
  },
  stripe: true,
};

const gridEvents: VxeGridListeners<RoleRecord> = {
  cellClick: ({ row }) => {
    handleRoleRowClick(row);
  },
};

const [Grid, gridApi] = useVbenVxeGrid({ gridOptions, gridEvents });

// 将列表转换为树形结构
function toTree(list: any[]) {
  const map = new Map<string, any>();
  const roots: any[] = [];

  // 1. 初始化映射
  list.forEach((item) => {
    map.set(item.id, { ...item, children: [] });
  });

  // 2. 构建树
  map.forEach((node) => {
    const pid = node.parentId;
    // 如果有父节点且父节点存在于映射中（防止孤儿节点）
    if (pid && map.has(pid)) {
      map.get(pid).children.push(node);
    } else {
      roots.push(node);
    }
  });

  return roots;
}

// 递归清理菜单数据，移除 parent 引用避免循环
function cleanMenuData(menus: any[]): any[] {
  return menus.map((item) => {
    const { parent, ...rest } = item;
    const children =
      item.children && item.children.length > 0
        ? cleanMenuData(item.children)
        : undefined;
    return {
      ...rest,
      key: item.id,
      title: item.title || item.name,
      children,
    };
  });
}

// 加载菜单树
const loadMenuTree = async () => {
  try {
    const menus = await getMenuTree({});
    // 先转换为树形结构
    const treeData = toTree(menus);
    // 转换数据格式，添加 key 字段，并清理 parent 引用避免循环
    menuTreeData.value = cleanMenuData(treeData);
    // 设置根节点默认展开
    expandedMenuKeys.value = menuTreeData.value.map((node) => node.key);
  } catch (error) {
    console.error('Failed to load menu tree', error);
    message.error('加载菜单树失败');
  }
};

// 加载角色的菜单权限
const loadRoleMenus = async (roleId: string) => {
  loadingMenu.value = true;
  try {
    const menuIds = await getRoleMenuIds(roleId);
    checkedMenuKeys.value = Array.isArray(menuIds) ? [...menuIds] : [];
    halfCheckedMenuKeys.value = [];
  } catch (error) {
    console.error('Failed to load role menus', error);
    message.error('加载角色菜单权限失败');
  } finally {
    loadingMenu.value = false;
  }
};

// 从菜单树中递归提取所有菜单ID
function extractMenuIds(menus: MenuTreeNode[]): string[] {
  const ids: string[] = [];
  const collect = (nodes: MenuTreeNode[]) => {
    for (const node of nodes) {
      ids.push(node.key);
      if (node.children && node.children.length > 0) {
        collect(node.children);
      }
    }
  };
  collect(menus);
  return ids;
}

// 处理角色行点击
function handleRoleRowClick(row: RoleRecord) {
  selectedRole.value = row;
  if (row.menus && row.menus.length > 0) {
    const collectIds = (menus: any[]): string[] => {
      const ids: string[] = [];
      const collect = (nodes: any[]) => {
        for (const node of nodes) {
          if (node.id) {
            ids.push(String(node.id));
          }
          if (Array.isArray(node.children) && node.children.length > 0) {
            collect(node.children);
          }
        }
      };
      collect(menus);
      return ids;
    };
    checkedMenuKeys.value = collectIds(row.menus as any[]);
    halfCheckedMenuKeys.value = [];
  } else {
    loadRoleMenus(row.id);
  }
}

// 处理重置
function handleReset() {
  searchKeyword.value = '';
  selectedStatus.value = '';
  gridApi.reload();
}

// 处理搜索
function handleSearch() {
  gridApi.reload();
}

function handleAdd() {
  drawerApi.setData({ isUpdate: false });
  drawerApi.open();
}

function handleEdit(row: RoleRecord) {
  drawerApi.setData({ isUpdate: true, record: row });
  drawerApi.open();
}

function handleDelete(row: RoleRecord) {
  Modal.confirm({
    title: '确认删除',
    content: `是否确认删除角色 "${row.roleName}"？`,
    onOk: async () => {
      try {
        await deleteRole(row.id);
        message.success('删除成功');
        // 如果删除的是当前选中的角色，清空右侧
        if (selectedRole.value?.id === row.id) {
          selectedRole.value = null;
          checkedMenuKeys.value = [];
          halfCheckedMenuKeys.value = [];
        }
        await gridApi.reload();
      } catch (error) {
        console.error(error);
      }
    },
  });
}

function handleSuccess() {
  gridApi.reload();
}

// 保存角色菜单权限
async function handleSavePermissions() {
  if (!selectedRole.value) {
    message.warning('请先选择角色');
    return;
  }

  try {
    // 计算所有选中的菜单（包括半选中的）
    const allCheckedKeys = [
      ...checkedMenuKeys.value,
      ...halfCheckedMenuKeys.value,
    ];
    await saveRoleMenus(selectedRole.value.id, allCheckedKeys);
    message.success('保存成功');
    // 刷新左侧角色列表，刷新后重新点击当前角色行以更新右侧数据
    const currentRoleId = selectedRole.value?.id;
    await gridApi.reload();
    // 重新获取角色数据并模拟点击
    if (currentRoleId) {
      const tableData = gridApi.grid?.getData?.() || [];
      const rowData = tableData.find((r: RoleRecord) => r.id === currentRoleId);
      if (rowData) {
        handleRoleRowClick(rowData);
      }
    }
  } catch (error) {
    console.error(error);
    message.error('保存失败');
  }
}

// 全选/取消全选
function handleCheckAll(checked: boolean) {
  if (checked) {
    // 获取所有菜单ID
    const allKeys: string[] = [];
    const collectKeys = (nodes: MenuTreeNode[]) => {
      for (const node of nodes) {
        allKeys.push(node.key);
        if (node.children && node.children.length > 0) {
          collectKeys(node.children);
        }
      }
    };
    collectKeys(menuTreeData.value);
    checkedMenuKeys.value = allKeys;
  } else {
    checkedMenuKeys.value = [];
  }
}

// 初始化
loadMenuTree();

const [Drawer, drawerApi] = useVbenDrawer({
  connectedComponent: RoleDrawer,
  destroyOnClose: true,
});
</script>

<template>
  <ColPage
    auto-content-height
    title="角色管理"
    :left-width="75"
    :left-min-width="50"
    :left-max-width="85"
    :right-width="25"
  >
    <template #left="{ isCollapsed, expand }">
      <!-- 搜索栏 -->
      <div
        class="mb-4 flex flex-wrap items-end gap-4 rounded-[var(--radius)] border border-border bg-card p-4"
      >
        <div class="min-w-[200px] flex-1">
          <label class="mb-1.5 block text-sm text-muted-foreground">
            关键词搜索
          </label>
          <Input
            v-model:value="searchKeyword"
            placeholder="角色名称/编码/备注"
            allow-clear
            @press-enter="handleSearch"
          />
        </div>
        <div class="w-[150px]">
          <label class="mb-1.5 block text-sm text-muted-foreground">状态</label>
          <Select
            v-model:value="selectedStatus"
            :options="statusOptions"
            style="width: 100%"
          />
        </div>
        <div class="flex gap-2">
          <Button type="primary" @click="handleSearch">
            <span class="i-ant-design:search-outlined mr-1"></span>
            搜索
          </Button>
          <Button @click="handleReset">
            <span class="i-ant-design:reload-outlined mr-1"></span>
            重置
          </Button>
        </div>
      </div>
      <div v-if="isCollapsed" class="flex h-full items-center justify-center">
        <Button
          shape="circle"
          type="primary"
          class="flex items-center justify-center"
          @click="expand"
        >
          <template #icon>
            <span class="i-bi:arrow-right text-xl"></span>
          </template>
        </Button>
      </div>
      <div
        v-else
        class="flex h-full w-full flex-col rounded-[var(--radius)] border border-border bg-card"
      >
        <div class="flex items-center gap-2 border-b border-border px-4 py-3">
          <IconifyIcon class="text-primary" icon="carbon:user-role" />
          <span class="font-medium">{{ $t('system.role.list') }}</span>
        </div>
        <div class="flex flex-1 flex-col overflow-hidden">
          <!-- 表格 -->
          <div class="flex-1 overflow-hidden">
            <div
              class="h-full rounded-[var(--radius)] border border-border bg-card p-4"
            >
              <Grid @cell-click="({ row }) => handleRoleRowClick(row)">
                <template #toolbar-buttons>
                  <AccessControl :codes="['super', 'admin']" type="role">
                    <Button type="primary" @click="handleAdd">
                      <span class="i-ant-design:plus-outlined mr-1"></span>
                      新增角色
                    </Button>
                  </AccessControl>
                </template>

                <template #status="{ row }">
                  <Tag v-if="row.status === 1" color="success">启用</Tag>
                  <Tag v-else color="error">禁用</Tag>
                </template>

                <template #action="{ row }">
                  <Button size="small" type="link" @click="handleEdit(row)">
                    编辑
                  </Button>
                  <Button
                    size="small"
                    type="link"
                    danger
                    @click="handleDelete(row)"
                  >
                    删除
                  </Button>
                </template>
              </Grid>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 右侧权限配置区域 -->
    <div class="flex h-full flex-col">
      <div
        class="mb-4 flex items-center justify-between rounded-[var(--radius)] border border-border bg-card px-4 py-3"
      >
        <div class="flex items-center gap-2">
          <IconifyIcon icon="carbon:security" class="text-primary" />
          <span class="font-medium">菜单权限</span>
          <Tag v-if="selectedRole" color="processing">
            {{ selectedRole.roleName }}
          </Tag>
          <Tag v-else color="default"> 请选择角色 </Tag>
        </div>
        <div class="flex items-center gap-2">
          <Button
            size="small"
            @click="handleCheckAll(checkedMenuKeys.length === 0)"
          >
            {{ checkedMenuKeys.length > 0 ? '取消全选' : '全选' }}
          </Button>
          <Button
            type="primary"
            :disabled="!selectedRole"
            @click="handleSavePermissions"
          >
            <span class="i-ant-design:save-outlined mr-1"></span>
            保存权限
          </Button>
        </div>
      </div>

      <!-- 菜单权限树 -->
      <div
        class="flex-1 overflow-auto rounded-[var(--radius)] border border-border bg-card p-4"
      >
        <Spin :spinning="loadingMenu">
          <div
            v-if="!selectedRole"
            class="flex h-full items-center justify-center"
          >
            <div class="text-center text-muted-foreground">
              <IconifyIcon icon="carbon:information" class="mb-2 text-3xl" />
              <p>请选择角色以配置权限</p>
            </div>
          </div>
          <Tree
            v-else
            v-model:checked-keys="checkedMenuKeys"
            v-model:half-checked-keys="halfCheckedMenuKeys"
            v-model:expanded-keys="expandedMenuKeys"
            :tree-data="menuTreeData"
            :field-names="{
              children: 'children',
              title: 'title',
              key: 'key',
            }"
            checkable
            :show-icon="false"
            block-node
          >
            <template #title="node">
              <div class="flex items-center gap-2">
                <IconifyIcon
                  v-if="node.dataRef?.icon || node.icon"
                  :icon="node.dataRef?.icon || node.icon"
                  class="shrink-0 text-base text-primary"
                />
                <IconifyIcon
                  v-else-if="(node.dataRef?.type || node.type) === 'button'"
                  icon="carbon:button"
                  class="shrink-0 text-base text-muted-foreground"
                />
                <IconifyIcon
                  v-else-if="(node.dataRef?.type || node.type) === 'catalog'"
                  icon="carbon:folder"
                  class="shrink-0 text-base text-primary"
                />
                <IconifyIcon
                  v-else-if="(node.dataRef?.type || node.type) === 'menu'"
                  icon="carbon:menu"
                  class="shrink-0 text-base text-success"
                />
                <IconifyIcon
                  v-else-if="(node.dataRef?.type || node.type) === 'link'"
                  icon="carbon:link"
                  class="shrink-0 text-base text-warning"
                />
                <IconifyIcon
                  v-else-if="(node.dataRef?.type || node.type) === 'embedded'"
                  icon="carbon:embed"
                  class="text-info shrink-0 text-base"
                />
                <IconifyIcon
                  v-else
                  icon="carbon:tree-alt"
                  class="shrink-0 text-base text-primary"
                />
                <span class="flex-1 truncate align-middle">
                  {{ $t(node.dataRef?.title || node.title) }}
                </span>
                <span
                  v-if="(node.dataRef?.type || node.type) === 'button'"
                  class="rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground"
                >
                  按钮
                </span>
                <span
                  v-else-if="(node.dataRef?.type || node.type) === 'menu'"
                  class="rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground"
                >
                  菜单
                </span>
                <span
                  v-else-if="(node.dataRef?.type || node.type) === 'catalog'"
                  class="rounded bg-muted px-1.5 py-0.5 text-xs text-muted-foreground"
                >
                  目录
                </span>
              </div>
            </template>
          </Tree>
        </Spin>
      </div>
    </div>

    <Drawer @success="handleSuccess" />
  </ColPage>
</template>

<style lang="scss" scoped>
:deep(.ant-tree-node-content-wrapper) {
  display: flex;
  align-items: center;
  min-height: 32px;
}

:deep(.ant-tree-treenode) {
  padding-top: 2px;
  padding-bottom: 2px;
}

:deep(
  .vxe-table--body-wrapper .vxe-table--body .vxe-body--row.row-current-row
) {
  background-color: #ecfccb !important;
}
</style>
