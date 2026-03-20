<script lang="ts" setup>
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { CmsCategory } from '#/api/cms/category';
import type { SearchRequest } from '#/api/common';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { Button, message, Modal, Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteCategory, getCategoryList } from '#/api/cms/category';

import CategoryFormDrawer from './components/category-form-drawer.vue';

defineOptions({ name: 'CmsCategoryPage' });

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: CategoryFormDrawer,
});

const gridOptions: VxeGridProps<CmsCategory> = {
  columns: [
    { field: 'categoryName', title: '栏目名称', minWidth: 220, treeNode: true },
    { field: 'categoryCode', title: '栏目编码', minWidth: 140 },
    { field: 'description', title: '栏目描述', minWidth: 160 },
    { field: 'sortOrder', title: '排序', width: 80 },
    {
      field: 'status',
      title: '状态',
      width: 100,
      slots: { default: 'status' },
    },
    { field: 'createTime', title: '创建时间', width: 180 },
    {
      field: 'action',
      title: '操作',
      fixed: 'right',
      width: 180,
      slots: { default: 'action' },
    },
  ],
  pagerConfig: {
    enabled: false,
  },
  proxyConfig: {
    ajax: {
      query: async ({ sorts }) => {
        const params: SearchRequest = {
          pageNumber: 1,
          pageSize: 1000,
          orders: sorts.map((item) => ({
            asc: item.order === 'asc',
            column: item.field,
          })),
        };
        const data = await getCategoryList(params);
        return {
          items: data.map((item) => ({
            ...item,
            parentId:
              item.parentId === '0' || (item.parentId as unknown) === 0
                ? null
                : item.parentId,
          })),
        };
      },
    },
  },
  toolbarConfig: {
    custom: true,
    refresh: true,
    zoom: true,
  },
  treeConfig: {
    parentField: 'parentId',
    rowField: 'id',
    transform: true,
    expandAll: true,
  },
};

const [Grid, gridApi] = useVbenVxeGrid({ gridOptions });

function handleCreate() {
  formDrawerApi.setData({ mode: 'create' }).open();
}

function handleEdit(row: CmsCategory) {
  formDrawerApi.setData({ mode: 'edit', record: row }).open();
}

function handleDelete(row: CmsCategory) {
  Modal.confirm({
    title: '确认删除',
    content: `是否删除栏目「${row.categoryName}」？`,
    onOk: async () => {
      await deleteCategory(row.id);
      message.success('删除成功');
      gridApi.reload();
    },
  });
}

function handleSuccess() {
  gridApi.reload();
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="栏目管理">
      <template #toolbar-tools>
        <Button type="primary" @click="handleCreate">新增栏目</Button>
      </template>

      <template #status="{ row }">
        <Tag v-if="row.status === 1" color="success">启用</Tag>
        <Tag v-else color="error">禁用</Tag>
      </template>

      <template #action="{ row }">
        <Button type="link" size="small" @click="handleEdit(row)">编辑</Button>
        <Button danger type="link" size="small" @click="handleDelete(row)">
          删除
        </Button>
      </template>
    </Grid>

    <FormDrawer @success="handleSuccess" />
  </Page>
</template>
