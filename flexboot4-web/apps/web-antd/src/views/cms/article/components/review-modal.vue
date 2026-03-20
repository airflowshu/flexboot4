<script lang="ts" setup>
import { ref, watch } from 'vue';

import { Modal } from 'ant-design-vue';

interface Props {
  action: 'approve' | 'reject';
  loading?: boolean;
  open: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
});

const emit = defineEmits<{
  cancel: [];
  submit: [comment: string];
}>();

const reviewComment = ref('');

watch(
  () => props.open,
  (value) => {
    if (value) {
      reviewComment.value = '';
    }
  },
);

function handleOk() {
  emit('submit', reviewComment.value);
}

function handleCancel() {
  emit('cancel');
}
</script>

<template>
  <Modal
    :open="open"
    :confirm-loading="loading"
    :title="action === 'approve' ? '审核通过' : '驳回文章'"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-textarea
      v-model:value="reviewComment"
      :rows="4"
      placeholder="请输入审核意见（可选）"
    />
  </Modal>
</template>
