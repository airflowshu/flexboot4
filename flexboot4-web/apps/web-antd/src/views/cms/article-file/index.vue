<script lang="ts" setup>
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { CmsArticleFile } from '#/api/cms/article-file';
import type { SearchRequest } from '#/api/common';

import { reactive } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { Button, Input, message, Modal } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteArticleFile, getArticleFilePage } from '#/api/cms/article-file';

import FileLinkDrawer from './components/file-link-drawer.vue';

defineOptions({ name: 'CmsArticleFilePage' });

const filters = reactive({ articleId: '' });

const [LinkDrawer, linkDrawerApi] = useVbenDrawer({
  connectedComponent: FileLinkDrawer,
});

const gridOptions: VxeGridProps<CmsArticleFile> = {
  columns: [
    { field: 'articleId', title: '文章ID', minWidth: 180 },
    {
      field: 'article',
      title: '文章标题',
      minWidth: 220,
      formatter: ({ row }) => row.article?.title || '-',
    },
    { field: 'fileId', title: '文件ID', minWidth: 180 },
    {
      field: 'file',
      title: '文件名',
      minWidth: 220,
      formatter: ({ row }) => row.file?.fileName || '-',
    },
    { field: 'sortOrder', title: '排序', width: 80 },
    { field: 'createTime', title: '创建时间', width: 180 },
    {
      field: 'action',
      title: '操作',
      fixed: 'right',
      width: 120,
      slots: { default: 'action' },
    },
  ],
  pagerConfig: { enabled: true },
  proxyConfig: {
    ajax: {
      query: async ({ page }) => {
        const items = filters.articleId
          ? [{ field: 'articleId', op: 'eq', val: filters.articleId }]
          : [];

        const params: SearchRequest = {
          pageNumber: page.currentPage,
          pageSize: page.pageSize,
          logic: 'AND',
          items,
          orders: [{ column: 'lastModifyTime', asc: false }],
        };
        const res = await getArticleFilePage(params);
        return { items: res.records, total: res.totalRow };
      },
    },
  },
  toolbarConfig: { custom: true, refresh: true, zoom: true },
};

const [Grid, gridApi] = useVbenVxeGrid({ gridOptions });

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  filters.articleId = '';
  gridApi.reload();
}

function handleCreate() {
  linkDrawerApi.open();
}

function handleDelete(row: CmsArticleFile) {
  Modal.confirm({
    title: '确认删除',
    content: `是否解除附件关联（${row.id}）？`,
    onOk: async () => {
      await deleteArticleFile(row.id);
      message.success('删除成功');
      gridApi.reload();
    },
  });
}

function handleDrawerSuccess() {
  gridApi.reload();
}
</script>

<template>
  <Page auto-content-height>
    <div class="mb-3 flex items-center gap-2">
      <Input
        v-model:value="filters.articleId"
        class="w-72"
        allow-clear
        placeholder="按文章ID筛选"
        @press-enter="handleSearch"
      />
      <Button type="primary" @click="handleSearch">搜索</Button>
      <Button @click="handleReset">重置</Button>
    </div>

    <Grid table-title="文章附件管理">
      <template #toolbar-tools>
        <Button type="primary" @click="handleCreate">关联附件</Button>
      </template>

      <template #action="{ row }">
        <Button danger type="link" size="small" @click="handleDelete(row)">
          删除
        </Button>
      </template>
    </Grid>

    <LinkDrawer @success="handleDrawerSuccess" />
  </Page>
</template>
