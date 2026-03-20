<script setup lang="ts">
import { ref } from 'vue';

import { PlusOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import Upload from 'ant-design-vue/es/upload';

const fileList = ref<any[]>([]);

const handleChange = (info: any) => {
  const { fileList: newFileList, file } = info;

  if (file.status === 'done') {
    message.success(`${file.name} 上传成功`);
  } else if (file.status === 'error') {
    message.error(`${file.name} 上传失败`);
  }

  // 限制上传数量
  if (newFileList.length > 9) {
    message.warning('最多只能上传 9 张图片');
    fileList.value = newFileList.slice(0, 9);
    return;
  }
  fileList.value = newFileList;
};

const handlePreview = (file: any) => {
  // 预览逻辑
  window.open(file.url || file.response?.url, '_blank');
};

const handleRemove = (file: any) => {
  const index = fileList.value.indexOf(file);
  const newFileList = [...fileList.value];
  newFileList.splice(index, 1);
  fileList.value = newFileList;
};

const beforeUpload = (file: any) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
  if (!isJpgOrPng) {
    message.error('只能上传 JPG/PNG 格式的图片!');
    return false;
  }
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) {
    message.error('图片大小必须小于 2MB!');
    return false;
  }
  return false; // 阻止自动上传
};
</script>

<template>
  <div>
    <div class="upload-container">
      <Upload
        v-model:file-list="fileList"
        list-type="picture-card"
        :before-upload="beforeUpload"
        :max-count="9"
        multiple
        action="https://www.mocky.io/v2/5cc8019a300000980a055e76"
        @change="handleChange"
        @preview="handlePreview"
        @remove="handleRemove"
      >
        <div v-if="fileList.length < 9">
          <PlusOutlined />
          <div class="mt-2">上传图片</div>
        </div>
      </Upload>
    </div>

    <div class="mt-4">
      <a-button
        type="primary"
        :disabled="fileList.length === 0"
        @click="() => console.log('提交图片:', fileList)"
      >
        提交
      </a-button>
    </div>

    <!-- 图片预览弹窗 -->
    <a-modal :open="false" title="预览" :footer="null">
      <img style="width: 100%" src="" alt="预览" />
    </a-modal>
  </div>
</template>

<style scoped>
.upload-container {
  display: inline-block;
}
</style>
