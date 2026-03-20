<script lang="ts" setup>
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { CmsArticle, CmsArticleStatus } from '#/api/cms/article';
import type { CmsCategory } from '#/api/cms/category';
import type { SearchRequest } from '#/api/common';

import { reactive, ref } from 'vue';

import { AccessControl } from '@vben/access';
import { Page, useVbenDrawer } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { Button, Input, message, Modal, Select, Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  approveArticle,
  deleteArticle,
  getArticlePage,
  rejectArticle,
  submitArticleForReview,
} from '#/api/cms/article';
import { getCategoryList } from '#/api/cms/category';

import ArticleFormDrawer from './components/article-form-drawer.vue';
import ReviewModal from './components/review-modal.vue';

defineOptions({ name: 'CmsArticleListPage' });

const statusOptions: Array<{ label: string; value: CmsArticleStatus }> = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已驳回', value: 'REJECTED' },
];

const filters = reactive({
  categoryId: undefined as string | undefined,
  keyword: '',
  status: undefined as CmsArticleStatus | undefined,
});

const categoryOptions = ref<CmsCategory[]>([]);
const userStore = useUserStore();
const reviewOpen = ref(false);
const reviewLoading = ref(false);
const reviewAction = ref<'approve' | 'reject'>('approve');
const reviewArticleId = ref('');

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: ArticleFormDrawer,
});

const gridOptions: VxeGridProps<CmsArticle> = {
  columns: [
    { field: 'title', title: '标题', minWidth: 220 },
    {
      field: 'category',
      title: '栏目',
      minWidth: 80,
      formatter: ({ row }) => row.category?.categoryName || '-',
    },
    { field: 'author', title: '作者', width: 120 },
    {
      field: 'status',
      title: '状态',
      width: 100,
      slots: { default: 'status' },
    },
    { field: 'viewCount', title: '浏览量', width: 90 },
    {
      field: 'publishTime',
      title: '发布时间',
      width: 180,
      formatter: ({ row }) => row.publishTime ?? '-',
    },
    { field: 'createTime', title: '创建时间', width: 180 },
    {
      field: 'action',
      title: '操作',
      fixed: 'right',
      width: 320,
      slots: { default: 'action' },
    },
  ],
  pagerConfig: {
    enabled: true,
  },
  proxyConfig: {
    ajax: {
      query: async ({ page }) => {
        const items = [] as Array<{ field: string; op: string; val: string }>;
        if (filters.keyword) {
          items.push({
            field: 'cmsArticle.title',
            op: 'like',
            val: filters.keyword,
          });
        }
        if (filters.status) {
          items.push({
            field: 'cmsArticle.status',
            op: 'eq',
            val: filters.status,
          });
        }
        if (filters.categoryId) {
          items.push({
            field: 'cmsArticle.categoryId',
            op: 'eq',
            val: filters.categoryId,
          });
        }

        const params: SearchRequest = {
          pageNumber: page.currentPage,
          pageSize: page.pageSize,
          logic: 'AND',
          items,
          orders: [{ column: 'cmsArticle.lastModifyTime', asc: false }],
        };
        const res = await getArticlePage(params);
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

async function loadCategories() {
  categoryOptions.value = await getCategoryList({
    pageNumber: 1,
    pageSize: 500,
    orders: [{ column: 'sortOrder', asc: true }],
  });
}

loadCategories();

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  filters.keyword = '';
  filters.status = undefined;
  filters.categoryId = undefined;
  gridApi.reload();
}

function handleCreate() {
  formDrawerApi.setData({ mode: 'create' }).open();
}

function handleEdit(row: CmsArticle) {
  formDrawerApi.setData({ mode: 'edit', record: row }).open();
}

function handleDelete(row: CmsArticle) {
  Modal.confirm({
    title: '确认删除',
    content: `是否删除文章「${row.title}」？`,
    onOk: async () => {
      await deleteArticle(row.id);
      message.success('删除成功');
      gridApi.reload();
    },
  });
}

async function handleSubmitReview(row: CmsArticle) {
  await submitArticleForReview(row.id);
  message.success('已提交审核');
  gridApi.reload();
}

function openReview(action: 'approve' | 'reject', row: CmsArticle) {
  reviewAction.value = action;
  reviewArticleId.value = row.id;
  reviewOpen.value = true;
}

async function handleReviewSubmit(comment: string) {
  if (!reviewArticleId.value) return;

  try {
    reviewLoading.value = true;
    if (reviewAction.value === 'approve') {
      await approveArticle(reviewArticleId.value, { reviewComment: comment });
      message.success('审核通过');
    } else {
      await rejectArticle(reviewArticleId.value, { reviewComment: comment });
      message.success('已驳回');
    }
    reviewOpen.value = false;
    gridApi.reload();
  } finally {
    reviewLoading.value = false;
  }
}

function handleReviewCancel() {
  reviewOpen.value = false;
}

function handleDrawerSuccess() {
  gridApi.reload();
}

function canSubmitReview(row: CmsArticle) {
  const currentUserId = userStore.userInfo?.id;
  if (!currentUserId) {
    return false;
  }
  const creatorId = row.createBy;
  return row.status === 'DRAFT' && `${creatorId ?? ''}` === `${currentUserId}`;
}

function canDelete(row: CmsArticle) {
  const currentUserId = userStore.userInfo?.id;
  if (!currentUserId) {
    return false;
  }
  return row.createBy === currentUserId;
}

function statusColor(status?: CmsArticleStatus) {
  switch (status) {
    case 'PENDING': {
      return 'warning';
    }
    case 'PUBLISHED': {
      return 'success';
    }
    case 'REJECTED': {
      return 'error';
    }
    default: {
      return 'default';
    }
  }
}

function statusLabel(status?: CmsArticleStatus) {
  return statusOptions.find((item) => item.value === status)?.label || '未知';
}
</script>

<template>
  <Page auto-content-height>
    <div class="mb-3 flex flex-wrap items-center gap-2">
      <Input
        v-model:value="filters.keyword"
        class="w-64"
        allow-clear
        placeholder="按标题搜索"
        @press-enter="handleSearch"
      />
      <Select
        v-model:value="filters.status"
        class="w-40"
        allow-clear
        placeholder="状态"
        :options="statusOptions"
      />
      <Select
        v-model:value="filters.categoryId"
        class="w-52"
        allow-clear
        placeholder="栏目"
        :options="
          categoryOptions.map((item) => ({
            label: item.categoryName,
            value: item.id,
          }))
        "
      />
      <Button type="primary" @click="handleSearch">搜索</Button>
      <Button @click="handleReset">重置</Button>
    </div>

    <Grid table-title="文章管理">
      <template #toolbar-tools>
        <Button type="primary" @click="handleCreate">创建文章</Button>
      </template>

      <template #status="{ row }">
        <Tag :color="statusColor(row.status)">
          {{ statusLabel(row.status) }}
        </Tag>
      </template>

      <template #action="{ row }">
        <Button type="link" size="small" @click="handleEdit(row)">编辑</Button>
        <Button
          v-if="canSubmitReview(row)"
          type="link"
          size="small"
          @click="handleSubmitReview(row)"
        >
          提交审核
        </Button>
        <AccessControl :codes="['cms:article:review']" type="code">
          <Button type="link" size="small" @click="openReview('approve', row)">
            通过
          </Button>
          <Button type="link" size="small" @click="openReview('reject', row)">
            驳回
          </Button>
        </AccessControl>
        <Button
          v-if="canDelete(row)"
          danger
          type="link"
          size="small"
          @click="handleDelete(row)"
        >
          删除
        </Button>
      </template>
    </Grid>

    <FormDrawer @success="handleDrawerSuccess" />

    <ReviewModal
      :open="reviewOpen"
      :loading="reviewLoading"
      :action="reviewAction"
      @cancel="handleReviewCancel"
      @submit="handleReviewSubmit"
    />
  </Page>
</template>
