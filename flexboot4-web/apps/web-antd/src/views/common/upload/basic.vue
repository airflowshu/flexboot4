<script setup lang="ts">
import { message } from 'ant-design-vue';

import Upload from 'ant-design-vue/es/upload';

const props = defineProps<{
  maxCount?: number;
}>();

const emit = defineEmits<{
  change: [fileList: any[]];
}>();

const handleChange = (info: any) => {
  const { fileList, file } = info;

  // 状态包括：uploading done error removed
  if (file.status === 'uploading') {
    // 上传中
  }
  if (file.status === 'done') {
    message.success(`${file.name} 上传成功`);
    emit('change', fileList);
  } else if (file.status === 'error') {
    message.error(`${file.name} 上传失败`);
  }
};

const handleDrop = (e: DragEvent) => {
  console.log(e.dataTransfer?.files);
};
</script>

<template>
  <div>
    <Upload
      name="file"
      :max-count="props.maxCount"
      multiple
      action="https://www.mocky.io/v2/5cc8019a300000980a055e76"
      @change="handleChange"
      @drop="handleDrop"
    >
      <a-button type="primary">
        <UploadOutlined />
        点击上传
      </a-button>
    </Upload>
    <div class="mt-2 text-gray-500 text-sm">
      支持拖拽上传，可设置 maxCount 限制上传数量
    </div>
  </div>
</template>
