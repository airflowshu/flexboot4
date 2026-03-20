<script setup lang="ts">
import { ref } from 'vue';

import { UploadOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import Upload from 'ant-design-vue/es/upload';

const fileList = ref<any[]>([]);
const uploading = ref(false);

const handleUpload = () => {
  const formData = new FormData();
  fileList.value.forEach((file: any) => {
    formData.append('files[]', file);
  });
  uploading.value = true;

  // 模拟上传请求
  setTimeout(() => {
    uploading.value = false;
    message.success('上传成功');
    fileList.value = [];
  }, 1500);
};

const handleChange = (info: any) => {
  fileList.value = [...info.fileList];

  // 过滤掉已删除的文件
  fileList.value = fileList.value.filter((file: any) => {
    if (file.response) {
      return file.status !== 'error';
    }
    return true;
  });
};

const beforeUpload = (file: any) => {
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) {
    message.error('文件大小必须小于 2MB!');
  }
  return false; // 阻止自动上传
};
</script>

<template>
  <div>
    <Upload
      name="file"
      :file-list="fileList"
      :before-upload="beforeUpload"
      :max-count="5"
      multiple
      @change="handleChange"
      action="https://www.mocky.io/v2/5cc8019a300000980a055e76"
    >
      <a-button type="primary" ghost>
        <UploadOutlined />
        选择文件
      </a-button>
    </Upload>

    <a-button
      type="primary"
      :loading="uploading"
      :disabled="fileList.length === 0"
      class="mt-4"
      @click="handleUpload"
    >
      {{ uploading ? '上传中...' : '开始上传' }}
    </a-button>

    <div class="mt-4 text-gray-500">
      <p>已选择 {{ fileList.length }} 个文件</p>
    </div>
  </div>
</template>
