<script lang="ts" setup>
import type { VbenFormProps } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SearchRequest } from '#/api/common';
import type { SysConfig } from '#/api/devops/sysconfig';

import { ref } from 'vue';

import {AccessControl} from "@vben/access";
import { Page, useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, message, Modal, Popconfirm, Tag } from 'ant-design-vue';
import dayjs from "dayjs";

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteConfig,
  getConfigPage,
  toggleConfigStatus,
} from '#/api/devops/sysconfig';
import { $t } from '#/locales';

import CreateConfigModal from './create-config-modal.vue';
import DetailDrawer from './detail-drawer.vue';
import EditConfigModal from './edit-config-modal.vue';

const [DetailModal, detailApi] = useVbenDrawer({
  connectedComponent: DetailDrawer,
});

const [CreateModal, createApi] = useVbenDrawer({
  connectedComponent: CreateConfigModal,
});

const [EditModal, editApi] = useVbenDrawer({
  connectedComponent: EditConfigModal,
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

// 配置类型选项
const typeOptions = [
  { value: 'STRING', label: '字符串' },
  { value: 'NUMBER', label: '数字' },
  { value: 'BOOLEAN', label: '布尔值' },
  { value: 'JSON', label: 'JSON' },
  { value: 'ARRAY', label: '数组' },
];

const formOptions: VbenFormProps = {
  collapsed: true,
  schema: [
    {
      component: 'Input',
      fieldName: 'configKey',
      label: '配置键',
      componentProps: {
        placeholder: '请输入配置键',
      },
    },
    {
      component: 'Select',
      fieldName: 'configType',
      label: '配置类型',
      componentProps: {
        placeholder: '请选择配置类型',
        options: typeOptions,
        allowClear: true,
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
  ],
  showCollapseButton: false,
  submitOnChange: true,
  submitOnEnter: true,
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const gridOptions: VxeGridProps = {
  columns: [
    { field: 'configKey', title: '配置键', minWidth: 180 },
    { field: 'configValue', title: '配置值', minWidth: 200 },
    { field: 'configType', title: '类型', width: 100 },
    { field: 'description', title: '描述', minWidth: 150 },
    {
      field: 'status',
      title: '状态',
      width: 100,
      slots: { default: 'status' },
    },
    {
      field: 'lastModifyTime',
      title: '更新时间',
      width: 180,
      formatter: ({ row }) =>
        row.lastModifyTime
          ? dayjs(row.lastModifyTime).format('YYYY-MM-DD HH:mm:ss')
          : '-',
    },
    {
      field: 'action',
      title: '操作',
      width: 250,
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
                column: 'createTime',
                asc: false,
              },
            ],
          };

          const items: SearchRequest['items'] = [];

          // 配置键
          if (formValues.configKey) {
            items.push({
              field: 'configKey',
              op: 'like',
              val: formValues.configKey,
            });
          }

          // 配置类型
          if (formValues.configType) {
            items.push({
              field: 'configType',
              op: 'eq',
              val: formValues.configType,
            });
          }

          // 状态
          if (formValues.status !== undefined && formValues.status !== null) {
            items.push({
              field: 'status',
              op: 'eq',
              val: formValues.status,
            });
          }

          if (items.length > 0) {
            params.items = items;
          }

          const resp = await getConfigPage(params);
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
async function handleStatusChange(row: SysConfig) {
  const newStatus = row.status === 1 ? 0 : 1;
  try {
    await toggleConfigStatus(row.id, newStatus);
    message.success(newStatus === 1 ? '已启用' : '已禁用');
    await gridApi.reload();
  } catch (error) {
    console.error(error);
    message.error('操作失败');
  }
}

// 删除
async function handleDelete(row: SysConfig) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除配置 "${row.configKey}" 吗？`,
    onOk: async () => {
      try {
        await deleteConfig(row.id);
        message.success('删除成功');
        await gridApi.reload();
      } catch (error) {
        console.error(error);
        message.error('删除失败');
      }
    },
  });
}

// 打开编辑弹窗
function handleEdit(row: SysConfig) {
  editApi.setData(row);
  editApi.open();
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
    <Grid :table-title="$t('devops.config.title')">
      <template #status="{ row }">
        <Tag v-if="row.status === 1" color="success">启用</Tag>
        <Tag v-else color="error">禁用</Tag>
      </template>

      <template #action="{ row }">
        <AccessControl :codes="['super']" type="role">
          <Button type="link" size="small" @click.stop="() => handleEdit(row)">
            编辑
          </Button>
          <Popconfirm
            title="确定要切换状态吗？"
            @confirm="handleStatusChange(row)"
          >
            <Tag
              :color="row.statusStr === '启用' ? 'warning' : 'processing'"
              class="cursor-pointer hover:opacity-80"
              @click.stop
            >
              <template #icon>
                <IconifyIcon
                  :icon="
                    row.statusStr === '启用'
                      ? 'ant-design:close-circle'
                      : 'ant-design:check-circle'
                  "
                />
              </template>
              {{ row.statusStr === '启用' ? '禁用' : '启用' }}
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
        </AccessControl>
      </template>

      <template #toolbar-tools>
        <AccessControl :codes="['super']" type="role">
          <Button type="primary" @click="createApi.open()"> 新增配置 </Button>
        </AccessControl>
      </template>
    </Grid>
    <DetailModal />
    <CreateModal @success="refreshGrid" />
    <EditModal @success="refreshGrid" />
  </Page>
</template>

<style lang="scss" scoped>
:deep(
  .vxe-table--body-wrapper .vxe-table--body .vxe-body--row.row-current-row
) {
  background-color: #ecfccb !important;
}
</style>
