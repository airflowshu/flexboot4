<script lang="ts" setup>
import type { VbenFormProps } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SearchRequest } from '#/api/common';
import type { AiApiKey } from '#/api/devops/aikey';

import { ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, message, Modal, Popconfirm, Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteApiKey,
  getApiKeyPage,
  toggleApiKeyStatus,
} from '#/api/devops/aikey';
import { $t } from '#/locales';

import CreateKeyModal from './create-key-modal.vue';
import DetailDrawer from './detail-drawer.vue';

const [DetailModal, detailApi] = useVbenDrawer({
  connectedComponent: DetailDrawer,
});

const [CreateModal, createApi] = useVbenDrawer({
  connectedComponent: CreateKeyModal,
});

const selectedRowId = ref<string>('');

// 刷新表格
function refreshGrid() {
  gridApi.reload();
}

// 状态选项
const statusOptions = [
  { value: 0, label: '禁用', color: 'error' },
  { value: 1, label: '启用', color: 'success' },
];

const formOptions: VbenFormProps = {
  collapsed: true,
  schema: [
    {
      component: 'Input',
      fieldName: 'keyName',
      label: 'Key名称',
      componentProps: {
        placeholder: '请输入Key名称',
      },
    },
    {
      component: 'Input',
      fieldName: 'apiKey',
      label: 'API Key',
      componentProps: {
        placeholder: '请输入API Key',
      },
    },
    {
      component: 'Select',
      fieldName: 'status',
      label: '状态',
      componentProps: {
        placeholder: '请选择状态',
        options: statusOptions,
        allowClear: true,
      },
    },
    {
      component: 'Input',
      fieldName: 'modelScope',
      label: '模型范围',
      componentProps: {
        placeholder: '请输入模型范围',
      },
    },
  ],
  showCollapseButton: true,
  submitOnChange: true,
  submitOnEnter: true,
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  columns: [
    { field: 'keyName', title: 'Key名称', minWidth: 150 },
    {
      field: 'apiKey',
      title: 'API Key',
      minWidth: 250,
      slots: { default: 'apiKey' },
    },
    {
      field: 'user',
      title: '使用人',
      minWidth: 100,
      slots: { default: 'user' },
    },
    { field: 'modelScope', title: '模型范围', minWidth: 120 },
    {
      field: 'status',
      title: '状态',
      width: 100,
      slots: { default: 'status' },
    },
    { field: 'quote', title: '配额', width: 80 },
    { field: 'used', title: '已使用', width: 80 },
    {
      field: 'expiresAt',
      title: '过期时间',
      width: 180,
      slots: { default: 'expiresAt' },
    },
    {
      field: 'lastUsedTime',
      title: '最后使用',
      width: 180,
      slots: { default: 'lastUsedTime' },
    },
    { field: 'createTime', title: '创建时间', width: 180 },
    {
      field: 'action',
      title: '操作',
      width: 180,
      fixed: 'right',
      slots: { default: 'action' },
    },
  ],
  pagerConfig: {
    enabled: true,
  },
  proxyConfig: {
    ajax: {
      query: async ({ page }, formValues) => {
        try {
          const params: SearchRequest = {
            pageNumber: page.currentPage,
            pageSize: page.pageSize,
            logic: 'AND',
            orders: [
              {
                column: 'aiApikey.lastModifyTime',
                asc: false,
              },
            ],
          };

          const items: SearchRequest['items'] = [];

          // Key名称
          if (formValues.keyName) {
            items.push({
              field: 'aiApikey.keyName',
              op: 'like',
              val: formValues.keyName,
            });
          }

          // API Key
          if (formValues.apiKey) {
            items.push({
              field: 'aiApikey.apiKey',
              op: 'like',
              val: formValues.apiKey,
            });
          }

          // 状态
          if (formValues.status !== undefined && formValues.status !== null) {
            items.push({
              field: 'aiApikey.status',
              op: 'eq',
              val: formValues.status,
            });
          }

          // 模型范围
          if (formValues.modelScope) {
            items.push({
              field: 'aiApikey.modelScope',
              op: 'like',
              val: formValues.modelScope,
            });
          }

          if (items.length > 0) {
            params.items = items;
          }

          const resp = await getApiKeyPage(params);
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
    resizable: true,
    zoom: true,
  },
  height: 'auto',
  rowConfig: {
    isHover: true,
    isCurrent: true,
  },
  rowClassName: ({ row }) => {
    return selectedRowId.value === row.id ? 'row-current-row' : '';
  },
  stripe: true,
};

function handleRowClick(row: any) {
  selectedRowId.value = row.id;
  detailApi.setData(row);
  detailApi.open();
}

// 切换状态
async function handleStatusChange(row: AiApiKey) {
  const newStatus = row.status === 1 ? 0 : 1;
  try {
    await toggleApiKeyStatus(row.id, newStatus);
    message.success(newStatus === 1 ? '已启用' : '已禁用');
    gridApi.reload();
  } catch (error) {
    console.error(error);
    message.error('操作失败');
  }
}

// 删除
async function handleDelete(row: AiApiKey) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除API Key "${row.keyName}"吗？`,
    onOk: async () => {
      try {
        await deleteApiKey(row.id);
        message.success('删除成功');
        await gridApi.reload();
      } catch (error) {
        console.error(error);
        message.error('删除失败');
      }
    },
  });
}

const gridEvents = {
  cellClick: ({ row }: any) => {
    handleRowClick(row);
  },
};

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions,
  gridOptions,
  gridEvents,
} as any);
</script>

<template>
  <Page auto-content-height>
    <Grid :table-title="$t('devops.aikey.title')">
      <template #apiKey="{ row }">
        <code class="rounded bg-gray-100 px-1 text-xs dark:bg-gray-700">{{
          row.apiKey
        }}</code>
      </template>

      <template #user="{ row }">
        {{ row.user?.username || '-' }}
      </template>

      <template #status="{ row }">
        <Tag v-if="row.status === 1" color="success">启用</Tag>
        <Tag v-else color="error">禁用</Tag>
      </template>

      <template #expiresAt="{ row }">
        <span
          :class="{
            'text-red-500':
              row.expiresAt && new Date(row.expiresAt) < new Date(),
          }"
        >
          {{ row.expiresAt || '-' }}
        </span>
      </template>

      <template #lastUsedTime="{ row }">
        {{ row.lastUsedTime || '-' }}
      </template>

      <template #action="{ row }">
        <Popconfirm title="确定要切换状态吗？" @confirm="handleStatusChange(row)">
          <Tag
            :color="row.status === 1 ? 'error' : 'success'"
            class="cursor-pointer hover:opacity-80"
            @click.stop
          >
            <template #icon>
              <IconifyIcon
                :icon="row.status === 1 ? 'ant-design:close-circle' : 'ant-design:check-circle'"
              />
            </template>
            {{ row.status === 1 ? '禁用' : '启用' }}
          </Tag>
        </Popconfirm>
        <Button
          type="link"
          size="small"
          danger
          @click.stop="() => handleDelete(row)"
        >
          删除
        </Button>
      </template>

      <template #toolbar-tools>
        <Button type="primary" @click="createApi.open()">
          创建新Key
        </Button>
      </template>
    </Grid>
    <DetailModal />
    <CreateModal @success="refreshGrid" />
  </Page>
</template>

<style lang="scss" scoped>
:deep(
  .vxe-table--body-wrapper .vxe-table--body .vxe-body--row.row-current-row
) {
  background-color: #ecfccb !important;
}
</style>
