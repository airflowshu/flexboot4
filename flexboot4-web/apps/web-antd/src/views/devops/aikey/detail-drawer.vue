<script lang="ts" setup>
import type { AiApiKey } from '#/api/devops/aikey';

import { computed } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { Card, Descriptions, Tag } from 'ant-design-vue';

import { getApiKeyById } from '#/api/devops/aikey';

defineOptions({ name: 'DetailDrawer' });

const detailData = computed(() => {
  return drawerApi.getData<AiApiKey>() || {};
});

const [Drawer, drawerApi] = useVbenDrawer({
  connectedComponent: undefined,
  destroyOnClose: true,
  placement: 'right',
  showCancelButton: false,
  showConfirmButton: false,
  onOpenChange: async (open) => {
    if (open) {
      const id = detailData.value.id;
      if (id) {
        try {
          const data = await getApiKeyById(id);
          drawerApi.setData(data);
        } catch (error) {
          console.error(error);
        }
      }
    }
  },
});

// 格式化时间
function formatTime(time?: string) {
  if (!time) return '-';
  return time.replace('T', ' ').slice(0, 19);
}

</script>

<template>
  <Drawer title="API Key详情" class="w-[600px]">
    <div class="flex flex-col gap-4">
      <!-- 基本信息 -->
      <Card title="基本信息" size="small">
        <Descriptions :column="1" bordered>
          <Descriptions.Item label="Key名称">
            {{ detailData.keyName || '-' }}
          </Descriptions.Item>
          <Descriptions.Item label="API Key">
            <code class="rounded bg-gray-100 px-2 py-1 text-sm">
              {{ detailData.apiKey || '-' }}
            </code>
          </Descriptions.Item>
          <Descriptions.Item label="模型范围">
            {{ detailData.modelScope || '-' }}
          </Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag v-if="detailData.status === 1" color="success">启用</Tag>
            <Tag v-else color="error">禁用</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="备注">
            {{ detailData.remark || '-' }}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <!-- 使用统计 -->
      <Card title="使用统计" size="small">
        <Descriptions :column="1" bordered>
          <Descriptions.Item label="配额">
            <Tag color="blue">{{ detailData.quote || 0 }}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="已使用">
            <Tag color="green">{{ detailData.used || 0 }}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="使用率">
            <Tag
              :color="
                detailData.quote && detailData.used
                  ? detailData.used / detailData.quote > 0.9
                    ? 'red'
                    : 'orange'
                  : 'default'
              "
            >
              {{
                detailData.quote && detailData.used
                  ? `${Math.round((detailData.used / detailData.quote) * 100)}%`
                  : '-'
              }}
            </Tag>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <!-- 时间信息 -->
      <Card title="时间信息" size="small">
        <Descriptions :column="1" bordered>
          <Descriptions.Item label="过期时间">
            <span
              :class="{
                'text-red-500':
                  detailData.expiresAt &&
                  new Date(detailData.expiresAt) < new Date(),
              }"
            >
              {{ formatTime(detailData.expiresAt) }}
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="最后使用时间">
            {{ formatTime(detailData.lastUsedTime) }}
          </Descriptions.Item>
          <Descriptions.Item label="创建时间">
            {{ formatTime(detailData.createTime) }}
          </Descriptions.Item>
          <Descriptions.Item label="更新时间">
            {{ formatTime(detailData.lastModifyTime) }}
          </Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  </Drawer>
</template>
