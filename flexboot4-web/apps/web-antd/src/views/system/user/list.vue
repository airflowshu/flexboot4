<script lang="ts" setup>
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SearchRequest } from '#/api/common';
import type { DeptRecord, UserRecord } from '#/api/system/user';

import { computed, ref } from 'vue';

import { AccessControl } from '@vben/access';
import { ColPage, useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlob } from '@vben/utils';

import {
  Button,
  Input,
  message,
  Modal,
  Select,
  Tag,
  Tree,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { buildSearchGroup, buildSearchItem } from '#/api/common';
import {
  deleteUser,
  deleteUserBatch,
  exportUsers,
  getDeptList,
  getUserPage,
} from '#/api/system/user';
import { $t } from '#/locales';

import RoleAssignDrawer from './role-assign-drawer.vue';
import UserDrawer from './user-drawer.vue';

defineOptions({ name: 'SystemUser' });

const [Drawer, drawerApi] = useVbenDrawer({
  connectedComponent: UserDrawer,
  destroyOnClose: true,
});

const [RoleDrawer, roleDrawerApi] = useVbenDrawer({
  connectedComponent: RoleAssignDrawer,
  destroyOnClose: true,
});

// 当前选中的部门ID
const selectedDeptId = ref<null | string>(null);
// 搜索关键词
const searchKeyword = ref('');
// 用户状态选项
const statusOptions = [
  { label: '全部', value: '' },
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
];
// 选中的状态
const selectedStatus = ref<'' | number>('');

// 部门树数据
const deptTreeData = ref<DeptRecord[]>([]);

const treeData = computed(() => {
  const allDeptNode = {
    id: 'ALL',
    deptName: '全部部门',
    children: [],
    status: 1, // 默认为启用状态，用于显示绿色对勾
    key: 'ALL',
    isRoot: true,
  };
  return [allDeptNode, ...deptTreeData.value];
});

// 选中的树节点
const selectedKeys = ref<string[]>(['ALL']);

// 展开的树节点
const expandedKeys = ref<string[]>([]);

// 处理树节点选择
function handleTreeSelect(keys: string[], { node: _node }: any) {
  if (keys.length > 0) {
    selectedKeys.value = keys;
    const key = keys[0];
    selectedDeptId.value = key === 'ALL' ? null : key;
    gridApi.reload();
  } else {
    // 如果取消选中，默认回退到“全部部门”
    selectedKeys.value = ['ALL'];
    selectedDeptId.value = null;
    gridApi.reload();
  }
}

// 将平铺数据转换为树形结构
function listToTree(list: DeptRecord[]): DeptRecord[] {
  const map = new Map<
    string,
    DeptRecord & { children?: DeptRecord[]; key: string }
  >();
  const roots: (DeptRecord & { children?: DeptRecord[]; key: string })[] = [];

  // 第一遍：建立所有节点的映射
  for (const item of list) {
    map.set(item.id, { ...item, children: [], key: item.id });
  }

  // 第二遍：建立父子关系
  for (const node of list) {
    const parentId =
      node.parentId === '0' || node.parentId === '0' ? null : node.parentId;
    if (parentId && map.has(parentId)) {
      map.get(parentId)!.children!.push(map.get(node.id)!);
    } else {
      roots.push(map.get(node.id)!);
    }
  }

  return roots;
}

// 加载部门树
const loadDeptTree = async () => {
  try {
    const depts = await getDeptList({ pageNumber: 1, pageSize: 100 });
    deptTreeData.value = listToTree(depts);
    // 默认展开所有一级节点
    expandedKeys.value = ['ALL', ...deptTreeData.value.map((node) => node.id)];
  } catch (error) {
    console.error('Failed to load dept tree', error);
  }
};

// 构建搜索参数
const buildSearchParams = (): Partial<SearchRequest> => {
  const items: any[] = [];

  if (searchKeyword.value) {
    items.push(
      buildSearchGroup('OR', [
        buildSearchItem('sysUser.username', 'like', searchKeyword.value),
        buildSearchItem('sysUser.realName', 'like', searchKeyword.value),
        buildSearchItem('sysUser.phone', 'like', searchKeyword.value),
        buildSearchItem('sysUser.email', 'like', searchKeyword.value),
      ]),
    );
  }

  if (selectedStatus.value !== '') {
    items.push(buildSearchItem('sysUser.status', 'eq', selectedStatus.value));
  }

  if (selectedDeptId.value) {
    items.push(buildSearchItem('sysUser.deptId', 'eq', selectedDeptId.value));
  }

  return {
    pageNumber: 1,
    pageSize: 10,
    logic: 'AND',
    items: items.length > 0 ? items : undefined,
    orders: [{ column: 'sysUser.createTime', asc: false }],
  };
};

const gridOptions: VxeGridProps<UserRecord> = {
  columns: [
    { type: 'checkbox', width: 60 },
    { field: 'username', title: '用户名', width: 120 },
    { field: 'realName', title: '真实姓名', width: 120 },
    {
      field: 'genderStr',
      title: '性别',
      width: 80,
      slots: { default: 'gender' },
    },
    {
      field: 'dept',
      title: '所属部门',
      width: 150,
      slots: { default: 'dept' },
    },
    { field: 'phone', title: '手机号', width: 130 },
    { field: 'email', title: '邮箱', minWidth: 180 },
    {
      field: 'status',
      title: '状态',
      width: 80,
      slots: { default: 'status' },
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
      width: 260,
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

          const resp = await getUserPage(params as SearchRequest);
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
  toolbarConfig: {
    custom: true,
    export: true,
    refresh: true,
    zoom: true,
    slots: {
      buttons: 'toolbar-buttons',
    },
  },
  exportConfig: {
    remote: true,
    useStyle: true,
    exportMethod: async ({ options }) => {
      const { mode } = options;
      // 构建导出参数
      const exportParams: any = buildSearchParams();

      // 根据导出模式调整参数
      if (mode === 'selected') {
        // 导出选中行
        const selectedRecords = gridApi.grid?.getCheckboxRecords();
        if (selectedRecords && selectedRecords.length > 0) {
          const ids = selectedRecords.map((row) => row.id);
          exportParams.items = [
            buildSearchGroup(
              'OR',
              ids.map((id) => buildSearchItem('sysUser.id', 'eq', id)),
            ),
          ];
        }
      }
      // current 和 all 模式都导出全部数据，只是分页参数不同

      const response = await exportUsers(exportParams);
      // 从响应头中提取文件名
      const contentDisposition =
        (response.headers?.['content-disposition'] as string) || '';
      const fileNameMatch = contentDisposition.match(
        /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/,
      );
      const fileName =
        fileNameMatch?.[1]?.replaceAll('"', '') || '用户列表.xlsx';

      // 触发下载
      downloadFileFromBlob({ fileName, source: response.data });
    },
  },
  stripe: true,
};

const [Grid, gridApi] = useVbenVxeGrid({ gridOptions });

// 重置搜索
function handleReset() {
  searchKeyword.value = '';
  selectedStatus.value = '';
  selectedDeptId.value = null;
  gridApi.reload();
}

// 搜索
function handleSearch() {
  gridApi.reload();
}

function handleAdd() {
  drawerApi.setData({ isUpdate: false });
  drawerApi.open();
}

function handleEdit(row: UserRecord) {
  drawerApi.setData({ isUpdate: true, record: row });
  drawerApi.open();
}

function handleAssignRole(row: UserRecord) {
  roleDrawerApi.setData(row);
  roleDrawerApi.open();
}

function handleDelete(row: UserRecord) {
  Modal.confirm({
    title: '确认删除',
    content: `是否确认删除用户 "${row.realName}"？`,
    onOk: async () => {
      try {
        await deleteUser(row.id);
        message.success('删除成功');
        await gridApi.reload();
      } catch (error) {
        console.error(error);
      }
    },
  });
}

function handleBatchDelete() {
  const selectedRecords = gridApi.grid?.getCheckboxRecords();
  if (!selectedRecords || selectedRecords.length === 0) {
    message.warning('请先选择要删除的用户');
    return;
  }

  const ids = selectedRecords.map((row) => row.id);

  Modal.confirm({
    title: '确认删除',
    content: `是否确认删除选中的 ${ids.length} 个用户？`,
    onOk: async () => {
      try {
        await deleteUserBatch(ids);
        message.success('删除成功');
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

// 初始化
loadDeptTree();
</script>

<template>
  <ColPage
    auto-content-height
    title="用户管理"
    :left-width="20"
    :left-min-width="15"
    :left-max-width="35"
  >
    <template #left="{ isCollapsed, expand }">
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
          <IconifyIcon class="text-primary" icon="carbon:tree-view-alt" />
          <span class="font-medium">{{ $t('system.dept.list') }}</span>
        </div>
        <div class="flex-1 overflow-auto p-3">
          <Tree
            v-model:expanded-keys="expandedKeys"
            v-model:selected-keys="selectedKeys"
            :tree-data="treeData"
            :field-names="{
              children: 'children',
              title: 'deptName',
              key: 'id',
            }"
            block-node
            @select="handleTreeSelect"
          >
            <template #title="{ data }">
              <div class="flex items-center">
                <IconifyIcon
                  :icon="
                    data.status === 1
                      ? 'carbon:checkmark-filled'
                      : 'carbon:close-filled'
                  "
                  class="mr-2 text-lg"
                  :class="{
                    'text-success': data.status === 1,
                    'text-destructive': data.status !== 1,
                  }"
                />
                <span class="truncate">{{ data.deptName }}</span>
              </div>
            </template>
            <template #icon="{ data }">
              <IconifyIcon
                v-if="data.isRoot"
                icon="carbon:folder"
                class="text-muted-foreground"
              />
            </template>
          </Tree>
        </div>
      </div>
    </template>

    <div class="flex h-full flex-col">
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
            placeholder="用户名/真实姓名/手机号/邮箱"
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

      <!-- 表格 -->
      <div class="flex-1">
        <div
          class="h-full rounded-[var(--radius)] border border-border bg-card p-4"
        >
          <Grid>
            <template #toolbar-buttons>
              <!--      type声明code为权限码控制，type声明为role为角色控制        -->
              <AccessControl :codes="['sys:user:create']" type="code">
                <Button type="primary" @click="handleAdd">新增用户</Button>
              </AccessControl>
              <AccessControl :codes="['sys:user:delete']" type="code">
                <Button danger class="ml-2" @click="handleBatchDelete">
                  批量删除
                </Button>
              </AccessControl>
            </template>

            <template #gender="{ row }">
              <Tag v-if="row.genderStr === '男'" color="blue">男</Tag>
              <Tag v-else-if="row.genderStr === '女'" color="pink">女</Tag>
              <Tag v-else color="default">未知</Tag>
            </template>

            <template #dept="{ row }">
              {{ row.dept?.deptName || '-' }}
            </template>

            <template #status="{ row }">
              <Tag v-if="row.status === 1" color="success">启用</Tag>
              <Tag v-else color="error">禁用</Tag>
            </template>

            <template #action="{ row }">
              <AccessControl :codes="['super', 'admin']" type="role">
                <Button size="small" type="link" @click="handleEdit(row)">
                  编辑
                </Button>
                <Button size="small" type="link" @click="handleAssignRole(row)">
                  关联角色
                </Button>
                <Button
                  danger
                  size="small"
                  type="link"
                  @click="handleDelete(row)"
                >
                  删除
                </Button>
              </AccessControl>
            </template>
          </Grid>
        </div>
      </div>
    </div>

    <Drawer @success="handleSuccess" />
    <RoleDrawer @success="handleSuccess" />
  </ColPage>
</template>

<style lang="scss" scoped>
:deep(.ant-tree-node-content-wrapper) {
  display: flex;
  align-items: center;
  min-height: 32px;
}
</style>
