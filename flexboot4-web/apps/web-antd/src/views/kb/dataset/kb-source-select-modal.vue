<script lang="ts" setup>
import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

const emit = defineEmits(['confirm']);

const selectedSource = ref('local');

const sources = [
  {
    id: 'local',
    title: '本地文件',
    description: '上传 PDF、TXT、DOCX 等格式的文件',
    icon: 'mdi:file-document-outline',
  },
  {
    id: 'web',
    title: '网页链接',
    description: '读取静态网页内容作为数据集',
    icon: 'mdi:link-variant',
  },
  {
    id: 'custom',
    title: '自定义文本',
    description: '手动输入一段文本作为数据集',
    icon: 'mdi:text-box-plus-outline',
  },
];

const [Modal, modalApi] = useVbenModal({
  title: '选择来源',
  class: 'w-[600px]',
  onConfirm: () => {
    emit('confirm', selectedSource.value);
    modalApi.close();
  },
});

defineExpose(modalApi);
</script>

<template>
  <Modal>
    <template #title>
      <div class="flex items-center gap-2">
        <IconifyIcon
          icon="mdi:check-circle-outline"
          class="source-title-icon"
        />
        <span>选择来源</span>
      </div>
    </template>

    <div class="kb-source-panel flex flex-col gap-4 p-6">
      <div
        v-for="source in sources"
        :key="source.id"
        class="source-card"
        :class="{ active: selectedSource === source.id }"
        @click="selectedSource = source.id"
      >
        <div class="source-radio">
          <div class="radio-inner"></div>
        </div>
        <div class="source-info">
          <div class="source-title">{{ source.title }}</div>
          <div class="source-desc">{{ source.description }}</div>
        </div>
      </div>
    </div>
  </Modal>
</template>

<style lang="scss" scoped>
.kb-source-panel {
  --kb-source-card: hsl(var(--card));
  --kb-source-border: hsl(var(--border));
  --kb-source-foreground: hsl(var(--foreground));
  --kb-source-muted: hsl(var(--muted-foreground));
  --kb-source-hover: hsl(var(--accent));
  --kb-source-active: hsl(var(--primary) / 0.08);
  --kb-source-primary: hsl(var(--primary));
}

.source-title-icon {
  font-size: 20px;
  color: var(--kb-source-primary);
}

.source-card {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 16px 20px;
  cursor: pointer;
  background: var(--kb-source-card);
  border: 1px solid var(--kb-source-border);
  border-radius: 12px;
  transition: all 0.2s ease;

  &:hover {
    background: var(--kb-source-hover);
    border-color: var(--kb-source-primary);
  }

  &.active {
    background: var(--kb-source-active);
    border-color: var(--kb-source-primary);
    box-shadow: 0 0 0 1px hsl(var(--primary) / 0.4);

    .source-radio {
      border-color: var(--kb-source-primary);

      .radio-inner {
        transform: scale(1);
      }
    }
  }
}

.source-radio {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border: 2px solid var(--kb-source-border);
  border-radius: 50%;
  transition: all 0.2s ease;

  .radio-inner {
    width: 8px;
    height: 8px;
    background: var(--kb-source-primary);
    border-radius: 50%;
    transform: scale(0);
    transition: transform 0.2s ease;
  }
}

.source-info {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .source-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--kb-source-foreground);
  }

  .source-desc {
    font-size: 13px;
    color: var(--kb-source-muted);
  }
}
</style>
