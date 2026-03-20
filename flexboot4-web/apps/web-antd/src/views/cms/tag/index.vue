<script lang="ts" setup>
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { CmsTag } from '#/api/cms/tag';
import type { SearchRequest } from '#/api/common';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { Tag as ATag, Button, Modal } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteTag, getTagPage } from '#/api/cms/tag';

import TagFormDrawer from './components/tag-form-drawer.vue';

defineOptions({ name: 'CmsTagPage' });

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: TagFormDrawer,
});

const gridOptions: VxeGridProps<CmsTag> = {
  columns: [
    { field: 'tagName', title: '标签名称', minWidth: 160 },
    {
      field: 'tagColor',
      title: '标签颜色',
      width: 150,
      slots: { default: 'tagColor' },
    },
    { field: 'useCount', title: '使用次数', width: 100 },
    { field: 'createTime', title: '创建时间', width: 180 },
    {
      field: 'action',
      title: '操作',
      width: 160,
      fixed: 'right',
      slots: { default: 'action' },
    },
  ],
  pagerConfig: {
    enabled: true,
  },
  proxyConfig: {
    ajax: {
      query: async ({ page }) => {
        const params: SearchRequest = {
          pageNumber: page.currentPage,
          pageSize: page.pageSize,
          orders: [{ column: 'lastModifyTime', asc: false }],
        };
        const res = await getTagPage(params);
        return {
          items: res.records,
          total: res.totalRow,
        };
      },
    },
  },
  toolbarConfig: {
    custom: true,
    refresh: true,
    zoom: true,
  },
};

const [Grid, gridApi] = useVbenVxeGrid({ gridOptions });

function handleCreate() {
  formDrawerApi.setData({ mode: 'create' }).open();
}

function handleEdit(row: CmsTag) {
  formDrawerApi.setData({ mode: 'edit', record: row }).open();
}

function handleDelete(row: CmsTag) {
  Modal.confirm({
    title: '确认删除',
    content: `是否删除标签「${row.tagName}」？`,
    onOk: async () => {
      await deleteTag(row.id);
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
    <Grid table-title="标签管理">
      <template #toolbar-tools>
        <Button type="primary" @click="handleCreate">新增标签</Button>
      </template>

      <template #tagColor="{ row }">
        <ATag :color="row.tagColor || '#1677ff'">
          {{ row.tagColor || '默认色' }}
        </ATag>
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
