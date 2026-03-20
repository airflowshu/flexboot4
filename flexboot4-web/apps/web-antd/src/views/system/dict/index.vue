<script lang="ts" setup>
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SearchRequest } from '#/api/common';
import type { DictType } from '#/api/system/dict';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { Button, message, Modal, Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteDictType, getDictTypePage } from '#/api/system/dict';

import DictDrawer from './dict-drawer.vue';
import DictItemDrawer from './dict-item-drawer.vue';

const [TypeDrawer, typeDrawerApi] = useVbenDrawer({
  connectedComponent: DictDrawer,
});

const [ItemDrawer, itemDrawerApi] = useVbenDrawer({
  connectedComponent: DictItemDrawer,
});

const gridOptions: VxeGridProps<DictType> = {
  columns: [
    { field: 'name', title: '字典名称', minWidth: 150 },
    { field: 'code', title: '字典编码', minWidth: 150 },
    { field: 'orderNo', title: '排序', width: 80 },
    {
      field: 'status',
      title: '状态',
      width: 80,
      slots: { default: 'status' },
    },
    { field: 'remark', title: '备注', minWidth: 150 },
    { field: 'createTime', title: '创建时间', width: 180 },
    {
      field: 'action',
      fixed: 'right',
      title: '操作',
      width: 220,
      slots: { default: 'action' },
    },
  ],
  pagerConfig: {
    enabled: true,
  },
  exportConfig: {
    remote: true,
    // params: search
  },
  proxyConfig: {
    ajax: {
      query: async ({ page, sorts: _sorts, form: _form }) => {
        try {
          const params: SearchRequest = {
            pageNumber: page.currentPage,
            pageSize: page.pageSize,
            orders: [
              {
                column: 'lastModifyTime',
                asc: false,
              },
            ],
          };
          const resp = await getDictTypePage(params);
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
    slots: { buttons: 'toolbar-buttons' },
  },
};

const [Grid, gridApi] = useVbenVxeGrid({ gridOptions });

function handleAdd() {
  typeDrawerApi.setData({ isUpdate: false });
  typeDrawerApi.open();
}

function handleEdit(row: DictType) {
  typeDrawerApi.setData({ isUpdate: true, record: row });
  typeDrawerApi.open();
}

function handleDelete(row: DictType) {
  Modal.confirm({
    title: '确认删除',
    content: `是否确认删除字典类型 "${row.name}"？`,
    onOk: async () => {
      try {
        await deleteDictType(row.id);
        message.success('删除成功');
        await gridApi.reload();
      } catch (error) {
        console.error(error);
      }
    },
  });
}

function handleManageItems(row: DictType) {
  itemDrawerApi.setData({
    typeId: row.id, // 传递字典ID用于关联字典项
  });
  itemDrawerApi.open();
}

function handleSuccess() {
  gridApi.reload();
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="字典管理">
      <template #toolbar-tools>
        <Button type="primary" @click="handleAdd">新增字典</Button>
      </template>

      <template #status="{ row }">
        <Tag v-if="row.status === 1" color="success">启用</Tag>
        <Tag v-else color="error">禁用</Tag>
      </template>

      <template #action="{ row }">
        <Button size="small" type="link" @click="handleEdit(row)">编辑</Button>
        <Button size="small" type="link" @click="handleManageItems(row)">
          字典项
        </Button>
        <Button danger size="small" type="link" @click="handleDelete(row)">
          删除
        </Button>
      </template>
    </Grid>
    <TypeDrawer @success="handleSuccess" />
    <ItemDrawer @refresh="handleSuccess" />
  </Page>
</template>
