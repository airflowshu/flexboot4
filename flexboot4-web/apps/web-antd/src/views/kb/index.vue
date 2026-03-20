<script lang="ts" setup>
import type {
  KnowledgeBase,
  KnowledgeBaseSearch,
} from '#/api/devops/knowledgebase';

import { computed, onMounted, ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Card,
  Empty,
  Input,
  message,
  Modal,
  Spin,
  Tag,
  Tooltip,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  deleteKnowledgeBase,
  getKnowledgeBaseList,
} from '#/api/devops/knowledgebase';

import KbDataset from './dataset/kb-dataset.vue';
import KbDetailDrawer from './management/kb-detail-drawer.vue';
import KbFormModal from './management/kb-form-modal.vue';

// 视图模式：list-列表模式，dataset-数据集模式
const viewMode = ref<'dataset' | 'list'>('list');
const currentKb = ref<KnowledgeBase | null>(null);

// 加载状态
const loading = ref(false);
const knowledgeBaseList = ref<KnowledgeBase[]>([]);
const searchKeyword = ref('');

// 抽屉和弹窗
const [DetailDrawer, detailApi] = useVbenDrawer({
  connectedComponent: KbDetailDrawer,
});

const [FormModal, formApi] = useVbenDrawer({
  connectedComponent: KbFormModal,
});

// 进入数据集视图
function handleEnterDataset(kb: KnowledgeBase) {
  currentKb.value = kb;
  viewMode.value = 'dataset';
}

// 返回列表视图
function handleBackToList() {
  viewMode.value = 'list';
  currentKb.value = null;
}

// 加载知识库列表
async function fetchKnowledgeBases() {
  loading.value = true;
  try {
    const params: KnowledgeBaseSearch = {
      pageNumber: 1,
      pageSize: 1000,
      logic: 'AND',
      orders: [
        {
          column: 'lastModifyTime',
          asc: false,
        },
      ],
      items: [],
    };
    if (searchKeyword.value) {
      params.items?.push({
        field: 'name',
        op: 'like',
        val: searchKeyword.value,
      });
    }
    const list = await getKnowledgeBaseList(params);
    knowledgeBaseList.value = list || [];
  } catch (error) {
    console.error('加载知识库列表失败:', error);
    message.error('加载知识库列表失败');
  } finally {
    loading.value = false;
  }
}

// 搜索
function handleSearch() {
  fetchKnowledgeBases();
}

// 创建知识库
function handleCreate() {
  formApi.setData({ mode: 'create' });
  formApi.open();
}

function formatTime(time?: string) {
  if (!time) return '-';
  try {
    return dayjs(time).format('YYYY-MM-DD HH:mm:ss');
  } catch {
    return String(time ?? '-');
  }
}

// 编辑知识库
function handleEdit(row: KnowledgeBase) {
  formApi.setData({ mode: 'edit', record: row });
  formApi.open();
}

// 查看详情
function handleView(row: KnowledgeBase) {
  detailApi.setData({ record: row });
  detailApi.open();
}

// 删除知识库
function handleDelete(row: KnowledgeBase) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除知识库 "${row.name}" 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteKnowledgeBase(row.id);
        message.success('删除成功');
        await fetchKnowledgeBases();
      } catch (error) {
        console.error('删除知识库失败:', error);
        message.error('删除失败');
      }
    },
  });
}

// 刷新列表
function refreshList() {
  fetchKnowledgeBases();
}

// 知识库类型标签颜色
const typeColors: Record<string, string> = {
  public: 'blue',
  team: 'purple',
  private: 'orange',
};

// 知识库类型标签文本
const typeLabels: Record<string, string> = {
  public: '公开',
  team: '团队',
  private: '私有',
};
const typeIcons: Record<string, string> = {
  public: 'mdi:earth',
  team: 'mdi:account-group',
  private: 'mdi:lock',
};

// 状态标签颜色
const statusColors: Record<number, string> = {
  0: 'error',
  1: 'success',
};

// 状态标签文本
const statusLabels: Record<number, string> = {
  0: '禁用',
  1: '正常',
};

// 过滤后的列表
const filteredList = computed(() => {
  if (!searchKeyword.value) return knowledgeBaseList.value;
  const keyword = searchKeyword.value.toLowerCase();
  return knowledgeBaseList.value.filter(
    (kb) =>
      kb.name.toLowerCase().includes(keyword) ||
      kb.description?.toLowerCase().includes(keyword),
  );
});

// 卡片背景渐变
const cardGradients = [
  'linear-gradient(135deg, #747df3 0%, #4d55d1 100%)',
  'linear-gradient(135deg, #f885bc 0%, #f44a9b 100%)',
  'linear-gradient(135deg, #53c4ff 0%, #2999ff 100%)',
  'linear-gradient(135deg, #4ade80 0%, #22c55e 100%)',
  'linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%)',
];

function getCardGradient(index: number): string {
  return (cardGradients[index % cardGradients.length] ??
    cardGradients[0]) as string;
}

onMounted(() => {
  fetchKnowledgeBases();
});
</script>

<template>
  <Page auto-content-height>
    <div v-if="viewMode === 'list'" class="kb-container">
      <!-- 头部区域 -->
      <div class="kb-header">
        <div class="kb-header-left">
          <div class="kb-title-wrapper">
            <IconifyIcon
              icon="mdi:book-open-page-variant"
              class="kb-title-icon"
            />
            <h2 class="kb-title">知识库管理</h2>
          </div>
          <p class="kb-subtitle">管理和组织您的知识库文档</p>
        </div>
        <div class="kb-header-right">
          <Button type="primary" size="large" @click="handleCreate">
            <IconifyIcon icon="ant-design:plus-outlined" />
            新建知识库
          </Button>
        </div>
      </div>

      <!-- 搜索区域 -->
      <div class="kb-search-wrapper">
        <div class="kb-search">
          <Input
            v-model:value="searchKeyword"
            placeholder="搜索知识库名称..."
            size="large"
            allow-clear
            @press-enter="handleSearch"
          >
            <template #prefix>
              <IconifyIcon
                icon="ant-design:search-outlined"
                class="search-icon"
              />
            </template>
          </Input>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="kb-loading">
        <Spin size="large" />
      </div>

      <!-- 空状态 -->
      <Empty
        v-else-if="filteredList.length === 0"
        description="暂无知识库"
        class="kb-empty"
      >
        <Button type="primary" @click="handleCreate">创建第一个知识库</Button>
      </Empty>

      <!-- 知识库卡片列表 -->
      <div v-else class="kb-card-grid">
        <Card
          v-for="(kb, index) in filteredList"
          :key="kb.id"
          class="kb-card"
          :style="{ background: getCardGradient(index) }"
          :body-style="{ padding: '0' }"
          @click="handleEnterDataset(kb)"
        >
          <div class="kb-card-content">
            <!-- 卡片头部图标 -->
            <div class="kb-card-header">
              <div class="kb-card-icon">
                <IconifyIcon
                  :icon="typeIcons[kb.type] || 'mdi:help-circle'"
                  style="font-size: 52px"
                />
              </div>
              <div class="kb-card-badges">
                <Tag class="type-tag">
                  {{ typeLabels[kb.type] }}
                </Tag>
                <Tag :class="['status-tag', kb.status === 1 ? 'status-success' : 'status-error']">
                  {{ statusLabels[kb.status] }}
                </Tag>
              </div>
            </div>

            <!-- 卡片标题和描述 -->
            <div class="kb-card-body">
              <h3 class="kb-card-title" :title="kb.name">{{ kb.name }}</h3>
              <p class="kb-card-desc" :title="kb.description || '暂无描述'">
                {{ kb.description || '暂无描述' }}
              </p>
            </div>

            <!-- 卡片底部信息 -->
            <div class="kb-card-footer">
              <div class="kb-card-info">
                <span class="info-item">
                  <IconifyIcon icon="mdi:clock-outline" />
                  {{ formatTime(kb.lastModifyTime || kb.createTime) }}
                </span>
                <span v-if="kb.memberCount !== undefined" class="info-item">
                  <IconifyIcon icon="mdi:account-multiple-outline" />
                  {{ kb.memberCount }}
                </span>
                <span v-if="kb.fileCount !== undefined" class="info-item">
                  <IconifyIcon icon="mdi:file-document-outline" />
                  {{ kb.fileCount }}
                </span>
              </div>
              <div class="kb-card-actions">
                <Tooltip title="查看详情">
                  <Button
                    type="text"
                    size="small"
                    class="action-btn"
                    @click.stop="handleView(kb)"
                  >
                    <IconifyIcon icon="ant-design:eye-outlined" />
                  </Button>
                </Tooltip>
                <Tooltip title="编辑">
                  <Button
                    type="text"
                    size="small"
                    class="action-btn"
                    @click.stop="handleEdit(kb)"
                  >
                    <IconifyIcon icon="ant-design:edit-outlined" />
                  </Button>
                </Tooltip>
                <Tooltip title="删除">
                  <Button
                    type="text"
                    size="small"
                    danger
                    class="action-btn"
                    @click.stop="handleDelete(kb)"
                  >
                    <IconifyIcon icon="ant-design:delete-outlined" />
                  </Button>
                </Tooltip>
              </div>
            </div>
          </div>
        </Card>
      </div>
    </div>

    <!-- 数据集视图 -->
    <KbDataset
      v-else
      :kb-id="currentKb?.id || ''"
      :kb-name="currentKb?.name"
      @back="handleBackToList"
    />

    <!-- 详情抽屉 -->
    <DetailDrawer @success="refreshList" />
    <!-- 创建/编辑弹窗 -->
    <FormModal @success="refreshList" />
  </Page>
</template>

<style lang="scss" scoped>
.kb-container {
  --kb-page-bg: hsl(var(--background-deep));
  --kb-panel-bg: hsl(var(--card));
  --kb-panel-shadow: 0 4px 20px rgb(0 0 0 / 5%);
  --kb-title: hsl(var(--foreground));
  --kb-subtitle: hsl(var(--muted-foreground));
  --kb-icon: hsl(var(--primary));
  --kb-input-border: hsl(var(--input));
  --kb-input-focus: hsl(var(--primary));
  --kb-input-shadow: 0 2px 8px rgb(0 0 0 / 2%);
  --kb-card-foreground: hsl(var(--primary-foreground));
  --kb-card-muted: color-mix(in srgb, hsl(var(--primary-foreground)), transparent 20%);
  --kb-card-strong: color-mix(in srgb, hsl(var(--primary-foreground)), transparent 10%);
  --kb-card-tag-bg: color-mix(in srgb, hsl(var(--primary-foreground)), transparent 75%);
  --kb-card-divider: color-mix(in srgb, hsl(var(--primary-foreground)), transparent 85%);
  --kb-card-action-hover: color-mix(in srgb, hsl(var(--primary-foreground)), transparent 80%);
  --kb-card-shadow-hover: 0 20px 40px rgb(0 0 0 / 12%);

  min-height: 100%;
  padding: 24px;
  background: var(--kb-page-bg);
}

:global(.dark) .kb-container {
  --kb-panel-shadow: 0 6px 18px rgb(0 0 0 / 35%);
  --kb-card-shadow-hover: 0 20px 40px rgb(0 0 0 / 35%);
}

.kb-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32px 40px;
  margin-bottom: 32px;
  background: var(--kb-panel-bg);
  border-radius: 24px;
  box-shadow: var(--kb-panel-shadow);
}

.kb-header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kb-title-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
}

.kb-title-icon {
  font-size: 32px;
  color: var(--kb-icon);
}

.kb-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: var(--kb-title);
}

.kb-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--kb-subtitle);
}

.kb-search-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 40px;
}

.kb-search {
  width: 100%;
  max-width: 500px;

  :deep(.ant-input-affix-wrapper) {
    padding: 10px 16px;
    border: 1px solid var(--kb-input-border);
    border-radius: 12px;
    box-shadow: var(--kb-input-shadow);

    &:hover,
    &.ant-input-affix-wrapper-focused {
      border-color: var(--kb-input-focus);
      box-shadow: 0 0 0 2px hsl(var(--primary) / 0.1);
    }

    .ant-input-prefix {
      margin-right: 8px;
      color: var(--kb-subtitle);
    }
  }
}

.kb-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 24px;
}

.kb-card {
  overflow: hidden;
  cursor: pointer;
  border: none;
  border-radius: 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    box-shadow: var(--kb-card-shadow-hover);
    transform: translateY(-6px);
  }
}

.kb-card-content {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 260px;
  padding: 24px;
  color: var(--kb-card-foreground);
}

.kb-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.kb-card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 90px;
  height: 90px;
  background: color-mix(in srgb, hsl(var(--primary-foreground)), transparent 76%);
  border-radius: 24px;
  box-shadow:
    inset 0 0 10px color-mix(in srgb, hsl(var(--primary-foreground)), transparent 65%),
    0 8px 18px rgb(0 0 0 / 14%);
}

.kb-card-badges {
  display: flex;
  gap: 8px;

  :deep(.ant-tag) {
    padding: 4px 12px;
    margin: 0;
    font-size: 12px;
    font-weight: 600;
    color: var(--kb-card-foreground);
    border: none;
    border-radius: 6px;
    backdrop-filter: blur(4px);
  }

  .type-tag {
    background: var(--kb-card-tag-bg);
  }

  .status-success {
    background: hsl(var(--success));
  }

  .status-error {
    background: hsl(var(--destructive));
  }
}

.kb-card-body {
  flex: 1;
}

.kb-card-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: var(--kb-card-foreground);
  letter-spacing: 0.5px;
}

.kb-card-desc {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-line-clamp: 2;
  font-size: 13px;
  line-height: 1.6;
  color: var(--kb-card-muted);
  -webkit-box-orient: vertical;
}

.kb-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
  margin-top: 12px;
  border-top: 1px solid var(--kb-card-divider);
}

.kb-card-info {
  .info-item {
    display: flex;
    gap: 6px;
    align-items: center;
    font-size: 12px;
    color: var(--kb-card-strong);
  }
}

.kb-card-actions {
  display: flex;
  gap: 2px;

  .action-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    color: var(--kb-card-strong);
    border-radius: 8px;
    transition: all 0.2s;

    &:hover {
      color: var(--kb-card-foreground);
      background: var(--kb-card-action-hover);
      transform: scale(1.1);
    }
  }
}

.kb-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.kb-empty {
  padding: 80px 0;
}
</style>
