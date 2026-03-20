<script setup lang="ts">
import { InboxOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import Upload from 'ant-design-vue/es/upload';

const handleChange = (info: any) => {
  const { fileList } = info;
  const status = fileList[fileList.length - 1]?.status;
  if (status === 'done') {
    message.success(`${fileList[fileList.length - 1].name} 上传成功`);
  } else if (status === 'error') {
    message.error(`${fileList[fileList.length - 1].name} 上传失败`);
  }
};

const beforeUpload = (file: any) => {
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) {
    message.error('文件大小必须小于 2MB!');
  }
  return false; // 阻止自动上传，改为手动控制
};
</script>

<template>
  <Upload
    name="file"
    multiple
    directory
    action="https://www.mocky.io/v2/5cc8019a300000980a055e76"
    @change="handleChange"
    :before-upload="beforeUpload"
  >
    <a-button type="primary" ghost>
      <InboxOutlined />
      点击或拖拽到此上传
    </a-button>
  </Upload>

  <div
    class="mt-4 rounded-lg border p-8 text-center transition-colors hover:border-blue-500 hover:bg-blue-50"
  >
    <Upload
      name="file"
      drag
      multiple
      action="https://www.mocky.io/v2/5cc8019a300000980a055e76"
      @change="handleChange"
    >
      <p class="ant-upload-drag-icon">
        <InboxOutlined />
      </p>
      <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
      <p class="ant-upload-hint">
        支持单个或批量上传。严禁上传公司数据或其他保密文件。
      </p>
    </Upload>
  </div>
</template>
