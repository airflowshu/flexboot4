<script lang="ts" setup>
import type { KnowledgeBase } from '#/api/devops/knowledgebase';

import { ref } from 'vue';

import { useVbenDrawer, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Avatar,
  Button,
  Descriptions,
  DescriptionsItem,
  Divider,
  Empty,
  message,
  Select,
  Spin,
  Tabs,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import {
  getKnowledgeBaseById,
  getKnowledgeBaseFiles,
  getKnowledgeBaseMembers,
  triggerKnowledgeBaseIndex,
} from '#/api/devops/knowledgebase';

import KbMemberModal from './kb-member-modal.vue';

const loading = ref(false);
const kbDetail = ref<KnowledgeBase | null>(null);
const members = ref<any[]>([]);
const files = ref<any[]>([]);
const triggeringIndex = ref(false);

// 模拟模型数据
const modelOptions = [
  { label: 'OpenAI text-embedding-3-small', value: 'text-embedding-3-small' },
  { label: 'OpenAI text-embedding-3-large', value: 'text-embedding-3-large' },
  { label: 'HuggingFace BGE-M3', value: 'bge-m3' },
  { label: 'Cohere Embed v3', value: 'embed-english-v3.0' },
];

const selectedModel = ref<string | undefined>(undefined);

// 知识库类型标签
const typeColors: Record<string, string> = {
  public: 'blue',
  team: 'purple',
  private: 'orange',
};

const typeLabels: Record<string, string> = {
  public: '公开',
  team: '团队',
  private: '私有',
};

// 状态标签颜色
const statusColors: Record<number, string> = {
  0: 'error',
  1: 'success',
};

const statusLabels: Record<number, string> = {
  0: '禁用',
  1: '正常',
};

async function fetchAll(kbId: string) {
  loading.value = true;
  try {
    const detailResp = await getKnowledgeBaseById(kbId);
    kbDetail.value = detailResp.data;
    selectedModel.value = detailResp.data?.embeddingModel; // 初始化选中的模型

    const membersResp = await getKnowledgeBaseMembers(kbId);
    members.value = membersResp.data || [];
    const filesResp = await getKnowledgeBaseFiles(kbId);
    files.value = filesResp.data || [];
  } catch (error) {
    console.error('加载知识库详情失败:', error);
    message.error('加载失败');
  } finally {
    loading.value = false;
  }
}

// 触发索引
async function handleTriggerIndex() {
  if (!props.record?.id) return;

  triggeringIndex.value = true;
  try {
    await triggerKnowledgeBaseIndex(props.record.id);
    message.success('索引任务已触发，请稍后查看状态');
  } catch (error) {
    console.error('触发索引失败:', error);
    message.error('触发失败');
  } finally {
    triggeringIndex.value = false;
  }
}

const activeTab = ref('info');

const [MemberModal, memberModalApi] = useVbenModal({
  connectedComponent: KbMemberModal,
});

function openMemberModal() {
  if (kbDetail.value?.id) {
    memberModalApi.setData({ kbId: kbDetail.value.id });
    memberModalApi.open();
  }
}

function handleMemberChange() {
  if (kbDetail.value?.id) {
    // 刷新成员列表
    getKnowledgeBaseMembers(kbDetail.value.id).then((res) => {
      members.value = res.data || [];
    });
  }
}

const [Drawer, drawerApi] = useVbenDrawer({
  placement: 'right',
  showCancelButton: false,
  showConfirmButton: false,
  destroyOnClose: true,
  onOpenChange: async (open) => {
    if (open) {
      const data = drawerApi.getData<{ record?: KnowledgeBase }>();
      const kbId = data?.record?.id;
      if (kbId) {
        await fetchAll(kbId);
      } else {
        kbDetail.value = null;
        members.value = [];
        files.value = [];
        selectedModel.value = undefined;
      }
    }
  },
});
</script>

<template>
  <Drawer title="知识库详情" class="kb-detail-drawer w-[720px]">
    <Spin :spinning="loading">
      <template v-if="kbDetail">
        <div class="drawer-header">
          <div class="header-icon">
            <IconifyIcon icon="mdi:folder-book-open" :size="48" />
          </div>
          <div class="header-info">
            <h2 class="header-title">{{ kbDetail.name }}</h2>
            <div class="header-tags">
              <Tag :color="typeColors[kbDetail.type]">
                {{ typeLabels[kbDetail.type] }}
              </Tag>
              <Tag :color="statusColors[kbDetail.status]">
                {{ statusLabels[kbDetail.status] }}
              </Tag>
            </div>
          </div>
        </div>

        <div class="drawer-actions">
          <Button :loading="triggeringIndex" @click="handleTriggerIndex">
            <template #icon>
              <IconifyIcon icon="mdi:database-refresh" />
            </template>
            刷新索引
          </Button>
        </div>

        <Divider />

        <Tabs v-model:active-key="activeTab" type="card">
          <Tabs.TabPane key="info" tab="基本信息">
            <div class="flex flex-col gap-4">
              <Descriptions :column="1" bordered size="small">
                <DescriptionsItem label="知识库名称">
                  {{ kbDetail.name }}
                </DescriptionsItem>
                <DescriptionsItem label="描述">
                  {{ kbDetail.description || '暂无描述' }}
                </DescriptionsItem>
                <DescriptionsItem label="类型">
                  <Tag :color="typeColors[kbDetail.type]">
                    {{ typeLabels[kbDetail.type] }}
                  </Tag>
                </DescriptionsItem>
                <DescriptionsItem label="状态">
                  <Tag :color="statusColors[kbDetail.status]">
                    {{ statusLabels[kbDetail.status] }}
                  </Tag>
                </DescriptionsItem>
                <DescriptionsItem label="创建时间">
                  {{ kbDetail.createTime || '-' }}
                </DescriptionsItem>
                <DescriptionsItem label="更新时间">
                  {{ kbDetail.lastModifyTime || '-' }}
                </DescriptionsItem>
                <DescriptionsItem label="备注">
                  {{ kbDetail.remark || '暂无' }}
                </DescriptionsItem>
              </Descriptions>

              <!-- 模型选择区域 -->
              <div class="model-config-card">
                <div class="model-config-title">向量化模型配置</div>
                <Select
                  v-model:value="selectedModel"
                  class="w-full"
                  placeholder="选择向量化模型"
                  :options="modelOptions"
                />
                <div class="model-config-hint">
                  选择用于知识库文档向量化的 Embedding 模型。
                </div>
              </div>
            </div>
          </Tabs.TabPane>

          <Tabs.TabPane key="members" tab="成员管理">
            <div class="mb-4 flex items-center justify-between">
              <span class="text-base font-medium">协作者列表</span>
              <Tooltip title="管理协作者">
                <Button type="text" shape="circle" @click="openMemberModal">
                  <template #icon>
                    <IconifyIcon icon="mdi:cog" class="text-lg text-primary" />
                  </template>
                </Button>
              </Tooltip>
            </div>

            <div v-if="members.length > 0" class="member-list">
              <div
                v-for="member in members"
                :key="member.userId"
                class="member-item"
              >
                <div class="member-avatar">
                  <Avatar>
                    <template #icon>
                      <IconifyIcon icon="mdi:account" />
                    </template>
                  </Avatar>
                </div>
                <div class="member-info">
                  <span class="member-name">{{
                    member.realName || member.username
                  }}</span>
                  <span class="member-role">{{ member.role }}</span>
                </div>
                <span class="member-time">{{ member.joinTime }}</span>
              </div>
            </div>
            <Empty v-else description="暂无成员" />
          </Tabs.TabPane>

          <Tabs.TabPane key="files" tab="关联文件">
            <div v-if="files.length > 0" class="file-list">
              <div v-for="file in files" :key="file.id" class="file-item">
                <div class="file-icon">
                  <IconifyIcon icon="mdi:file-document" :size="24" />
                </div>
                <div class="file-info">
                  <span class="file-name">{{ file.fileName }}</span>
                  <span class="file-meta">
                    {{ file.fileType }} ·
                    {{
                      file.fileSize
                        ? `${(file.fileSize / 1024).toFixed(1)} KB`
                        : '-'
                    }}
                  </span>
                </div>
                <div class="file-status">
                  <Tag :color="file.indexed ? 'success' : 'default'">
                    {{ file.indexed ? '已索引' : '未索引' }}
                  </Tag>
                </div>
              </div>
            </div>
            <Empty v-else description="暂无关联文件" />
          </Tabs.TabPane>
        </Tabs>
      </template>
      <Empty v-else description="未找到知识库信息" />
    </Spin>
    <MemberModal @change="handleMemberChange" />
  </Drawer>
</template>

<style lang="scss" scoped>
.kb-detail-drawer {
  --kb-detail-foreground: hsl(var(--foreground));
  --kb-detail-muted: hsl(var(--muted-foreground));
  --kb-detail-muted-bg: hsl(var(--muted));
  --kb-detail-border: hsl(var(--border));
  --kb-detail-primary: hsl(var(--primary));
  --kb-detail-icon-from: hsl(var(--primary));
  --kb-detail-icon-to: color-mix(in srgb, hsl(var(--primary)), transparent 30%);

  padding: 16px;
}

:global(.dark) .kb-detail-drawer {
  --kb-detail-muted-bg: hsl(var(--accent));
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.drawer-header {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
}

.header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  color: hsl(var(--primary-foreground));
  background: linear-gradient(
    135deg,
    var(--kb-detail-icon-from) 0%,
    var(--kb-detail-icon-to) 100%
  );
  border-radius: 16px;
}

.header-info {
  flex: 1;
}

.header-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: var(--kb-detail-foreground);
}

.header-tags {
  display: flex;
  gap: 8px;
}

.drawer-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.member-item {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  background: var(--kb-detail-muted-bg);
  border-radius: 8px;
}

.member-avatar {
  color: var(--kb-detail-muted);
}

.member-info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.member-name {
  font-weight: 500;
  color: var(--kb-detail-foreground);
}

.member-role {
  font-size: 12px;
  color: var(--kb-detail-muted);
}

.member-time {
  font-size: 12px;
  color: var(--kb-detail-muted);
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px;
  background: var(--kb-detail-muted-bg);
  border-radius: 8px;
}

.file-icon {
  color: var(--kb-detail-primary);
}

.file-info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.file-name {
  font-weight: 500;
  color: var(--kb-detail-foreground);
}

.file-meta {
  font-size: 12px;
  color: var(--kb-detail-muted);
}

.model-config-card {
  margin-top: 8px;
  padding: 16px;
  background: var(--kb-detail-muted-bg);
  border: 1px solid var(--kb-detail-border);
  border-radius: 8px;
}

.model-config-title {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--kb-detail-foreground);
}

.model-config-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--kb-detail-muted);
}
</style>
