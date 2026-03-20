<script setup lang="ts">
import { ref } from 'vue';

import {
  DeleteOutlined,
  DownloadOutlined,
  EyeOutlined,
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import Upload from 'ant-design-vue/es/upload';

const fileList = ref<any[]>([]);
const previewVisible = ref(false);
const previewImage = ref('');

const handleCancel = () => {
  previewVisible.value = false;
};

const handlePreview = (file: any) => {
  previewImage.value = file.url || file.thumbUrl || file.response?.url || '';
  previewVisible.value = true;
};

const handleChange = (info: any) => {
  const { fileList: newFileList } = info;
  fileList.value = newFileList;

  if (info.file.status === 'done') {
    message.success(`${info.file.name} 上传成功`);
  } else if (info.file.status === 'error') {
    message.error(`${info.file.name} 上传失败`);
  }
};

const handleRemove = (file: any) => {
  const index = fileList.value.indexOf(file);
  fileList.value.splice(index, 1);
};

const downloadFile = (file: any) => {
  const url = file.url || file.response?.url;
  if (url) {
    const link = document.createElement('a');
    link.href = url;
    link.download = file.name;
    link.click();
  }
};
</script>

<template>
  <div>
    <Upload
      v-model:file-list="fileList"
      :max-count="10"
      multiple
      action="https://www.mocky.io/v2/5cc8019a300000980a055e76"
      list-type="picture"
      @change="handleChange"
      @preview="handlePreview"
      @remove="handleRemove"
    >
      <a-button type="primary">
        <UploadOutlined />
        点击上传（带预览）
      </a-button>
    </Upload>

    <!-- 预览弹窗 -->
    <a-modal
      :open="previewVisible"
      title="图片预览"
      :footer="null"
      @cancel="handleCancel"
    >
      <img
        v-if="previewImage"
        style="width: 100%"
        :src="previewImage"
        alt="预览"
      />
    </a-modal>

    <!-- 自定义文件列表操作 -->
    <div v-if="fileList.length > 0" class="mt-4">
      <h4>已上传文件：</h4>
      <div v-for="file in fileList" :key="file.uid" class="file-item">
        <span>{{ file.name }}</span>
        <div class="file-actions">
          <a-button type="link" size="small" @click="handlePreview(file)">
            <EyeOutlined />
          </a-button>
          <a-button type="link" size="small" @click="downloadFile(file)">
            <DownloadOutlined />
          </a-button>
          <a-button type="link" size="small" danger @click="handleRemove(file)">
            <DeleteOutlined />
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.file-actions {
  display: flex;
  gap: 8px;
}
</style>
