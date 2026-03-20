<script setup lang="ts">
import { h, ref } from 'vue';

import {
  CameraOutlined,
  CloudUploadOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';

import Upload from 'ant-design-vue/es/upload';

const fileList = ref<any[]>([]);

const handleChange = (info: any) => {
  const { fileList: newFileList } = info;
  fileList.value = newFileList;

  if (info.file.status === 'done') {
    message.success(`${info.file.name} 上传成功`);
  } else if (info.file.status === 'error') {
    message.error(`${info.file.name} 上传失败`);
  }
};

// 自定义上传按钮组件
const UploadButton = (props: { loading: boolean }) => {
  return h(
    'div',
    {
      class: 'custom-upload-btn',
    },
    [
      props.loading
        ? h(CloudUploadOutlined, { spin: true, style: { fontSize: '24px' } })
        : h(CameraOutlined, { style: { fontSize: '24px' } }),
    ],
  );
};
</script>

<template>
  <div>
    <!-- 方式一：使用 a-button 作为上传按钮 -->
    <div class="mb-4">
      <h4>方式一：使用按钮组件</h4>
      <Upload
        name="file"
        :file-list="fileList"
        multiple
        action="https://www.mocky.io/v2/5cc8019a300000980a055e76"
        @change="handleChange"
      >
        <a-button type="primary">
          <UploadOutlined />
          自定义按钮上传
        </a-button>
      </Upload>
    </div>

    <!-- 方式二：使用自定义图标按钮 -->
    <div class="mb-4">
      <h4>方式二：使用图标按钮</h4>
      <Upload
        name="file"
        :show-upload-list="false"
        multiple
        action="https://www.mocky.io/v2/5cc8019a300000980a055e76"
        @change="handleChange"
      >
        <a-button shape="circle" size="large">
          <CameraOutlined />
        </a-button>
        <a-button class="ml-2">
          <CloudUploadOutlined />
          上传
        </a-button>
      </Upload>
    </div>

    <!-- 方式三：完全自定义上传区域 -->
    <div>
      <h4>方式三：自定义上传区域</h4>
      <Upload
        name="file"
        :show-upload-list="false"
        multiple
        action="https://www.mocky.io/v2/5cc8019a300000980a055e76"
        @change="handleChange"
      >
        <div
          class="custom-upload-area"
        >
          <CloudUploadOutlined style="font-size: 48px; color: #1890ff" />
          <p class="mt-2">点击或拖拽文件到此区域上传</p>
        </div>
      </Upload>
    </div>

    <!-- 已上传文件列表 -->
    <div v-if="fileList.length > 0" class="mt-4">
      <h4>已上传文件：</h4>
      <a-list :data-source="fileList" size="small">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :title="item.name">
              <template #avatar>
                <a-avatar :src="item.thumbUrl || item.url" />
              </template>
            </a-list-item-meta>
            <template #actions>
              <span :class="item.status === 'uploading' ? 'text-blue-500' : ''">
                {{ item.status === 'uploading' ? '上传中...' : item.status }}
              </span>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </div>
  </div>
</template>

<style scoped>
.custom-upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  max-width: 400px;
  height: 200px;
  text-align: center;
  cursor: pointer;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  transition: all 0.3s;
}

.custom-upload-area:hover {
  background: #f0f8ff;
  border-color: #1890ff;
}
</style>
