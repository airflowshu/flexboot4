<script lang="ts" setup>
import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Input, message } from 'ant-design-vue';

const emit = defineEmits(['confirm']);

const folderName = ref('');

const [Modal, modalApi] = useVbenModal({
  title: '创建文件夹',
  class: 'w-[400px]',
  onConfirm: () => {
    if (!folderName.value.trim()) {
      message.warning('请输入文件夹名称');
      return;
    }
    emit('confirm', folderName.value);
    modalApi.close();
  },
  onOpenChange: (open) => {
    if (open) {
      folderName.value = '';
    }
  },
});

defineExpose(modalApi);
</script>

<template>
  <Modal>
    <template #title>
      <div class="flex items-center gap-2">
        <IconifyIcon icon="mdi:folder" class="folder-title-icon" />
        <span class="text-base font-semibold">创建文件夹</span>
      </div>
    </template>

    <div class="kb-folder-panel p-6">
      <Input
        v-model:value="folderName"
        placeholder="输入文件夹名称"
        class="custom-folder-input"
        @press-enter="modalApi.onConfirm()"
      />
    </div>
  </Modal>
</template>

<style lang="scss" scoped>
.kb-folder-panel {
  --kb-folder-border: hsl(var(--input));
  --kb-folder-focus: hsl(var(--primary));
  --kb-folder-focus-shadow: hsl(var(--primary) / 0.1);
  --kb-folder-icon: hsl(var(--warning));
}

.folder-title-icon {
  font-size: 20px;
  color: var(--kb-folder-icon);
}

.custom-folder-input {
  height: 40px;
  border: 1px solid var(--kb-folder-border);
  border-radius: 8px;

  &:focus,
  &:hover {
    border-color: var(--kb-folder-focus);
    box-shadow: 0 0 0 2px var(--kb-folder-focus-shadow);
  }
}
</style>
