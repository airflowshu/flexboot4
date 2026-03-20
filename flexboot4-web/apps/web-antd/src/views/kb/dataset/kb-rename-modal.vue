<script lang="ts" setup>
import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Input, message } from 'ant-design-vue';

const emit = defineEmits(['confirm']);

const newName = ref('');
const currentFileId = ref('');

const [Modal, modalApi] = useVbenModal({
  title: '重命名',
  class: 'w-[400px]',
  draggable: false,
  closable: false,
  fullscreenButton: false,
  footer: false, // 隐藏默认底部，使用自定义底部
  onOpenChange: (open) => {
    if (open) {
      const data = modalApi.getData<{ id: string; name: string }>();
      if (data) {
        currentFileId.value = data.id;
        newName.value = data.name;
      }
    }
  },
});

function handleCancel() {
  modalApi.close();
}

function handleConfirm() {
  if (!newName.value.trim()) {
    message.warning('请输入名称');
    return;
  }
  emit('confirm', { id: currentFileId.value, name: newName.value });
  modalApi.close();
}
</script>

<template>
  <Modal>
    <template #title>
      <div class="flex w-full items-center justify-between">
        <div class="flex items-center gap-2">
          <IconifyIcon
            icon="mdi:rename-box-outline"
            class="rename-title-icon"
          />
          <span class="text-base font-semibold">重命名</span>
        </div>
        <IconifyIcon
          icon="mdi:close"
          class="rename-close-icon"
          @click="handleCancel"
        />
      </div>
    </template>

    <div class="kb-rename-panel p-6">
      <Input
        v-model:value="newName"
        class="custom-rename-input"
        @press-enter="handleConfirm"
      />

      <div class="mt-8 flex justify-end gap-3">
        <Button class="custom-btn-cancel" @click="handleCancel">关闭</Button>
        <Button
          type="primary"
          class="custom-btn-confirm"
          @click="handleConfirm"
        >
          确认
        </Button>
      </div>
    </div>
  </Modal>
</template>

<style lang="scss" scoped>
.kb-rename-panel {
  --kb-rename-border: hsl(var(--input));
  --kb-rename-primary: hsl(var(--primary));
  --kb-rename-foreground: hsl(var(--foreground));
  --kb-rename-muted: hsl(var(--muted-foreground));
}

.rename-title-icon {
  font-size: 20px;
  color: var(--kb-rename-primary);
}

.rename-close-icon {
  font-size: 20px;
  color: var(--kb-rename-muted);
  cursor: pointer;

  &:hover {
    color: var(--kb-rename-foreground);
  }
}

.custom-rename-input {
  height: 44px;
  padding: 0 12px;
  font-size: 14px;
  border: 1px solid var(--kb-rename-border);
  border-radius: 8px;

  &:focus,
  &:hover {
    border-color: var(--kb-rename-primary);
    box-shadow: 0 0 0 2px hsl(var(--primary) / 0.1);
  }
}

.custom-btn-cancel {
  height: 36px;
  padding: 0 20px;
  font-weight: 500;
  color: var(--kb-rename-foreground);
  border: 1px solid var(--kb-rename-border);
  border-radius: 6px;

  &:hover {
    color: var(--kb-rename-primary);
    border-color: var(--kb-rename-primary);
  }
}

.custom-btn-confirm {
  height: 36px;
  padding: 0 20px;
  font-weight: 500;
  background: var(--kb-rename-primary);
  border-radius: 6px;

  &:hover {
    background: hsl(var(--primary) / 0.9);
  }
}
</style>
