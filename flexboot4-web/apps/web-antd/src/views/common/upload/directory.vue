<script setup lang="ts">
import { ref } from 'vue';

import { FolderOpenOutlined, InboxOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import Upload from 'ant-design-vue/es/upload';

const fileList = ref<any[]>([]);

const handleChange = (info: any) => {
  const { fileList: newFileList } = info;

  // 处理新上传的文件
  newFileList.forEach((file: any) => {
    // 提取目录路径
    if (file.originFileObj) {
      // 获取文件的相对路径（如果浏览器支持）
      const path = file.originFileObj.webkitRelativePath || '';
      file.path = path;
    }
  });

  fileList.value = newFileList;

  // 检查是否有上传完成的文件
  const doneFiles = fileList.value.filter((f) => f.status === 'done');
  if (doneFiles.length > 0) {
    message.success(`成功上传 ${doneFiles.length} 个文件`);
  }
};

const beforeUpload = (file: any) => {
  const isLt2M = file.size / 1024 / 1024 < 500;
  if (!isLt2M) {
    message.error('文件大小必须小于 500MB!');
    return false;
  }
  return true; // 允许上传
};
</script>

<template>
  <div>
    <!-- 方式一：目录上传按钮 -->
    <div class="mb-4">
      <h4>方式一：选择目录上传</h4>
      <Upload
        name="file"
        :file-list="fileList"
        directory
        multiple
        action="https://www.mocky.io/v2/5cc8019a300000980a055e76"
        @change="handleChange"
        :before-upload="beforeUpload"
      >
        <a-button type="primary" ghost>
          <FolderOpenOutlined />
          选择目录上传
        </a-button>
      </Upload>
    </div>

    <!-- 方式二：拖拽目录上传 -->
    <div class="mb-4">
      <h4>方式二：拖拽目录上传</h4>
      <div class="directory-drop-zone">
        <Upload
          name="file"
          directory
          multiple
          action="https://www.mocky.io/v2/5cc8019a300000980a055e76"
          @change="handleChange"
          :before-upload="beforeUpload"
        >
          <div class="drop-content">
            <InboxOutlined style="font-size: 48px; color: #1890ff" />
            <p class="mt-2 text-base">拖拽文件夹到此区域</p>
            <p class="text-sm text-gray-500">
              将包含多个文件的文件夹拖拽到此进行上传
            </p>
          </div>
        </Upload>
      </div>
    </div>

    <!-- 上传进度和结果 -->
    <div v-if="fileList.length > 0">
      <h4>上传文件列表：</h4>
      <a-table
        :data-source="fileList"
        :pagination="false"
        size="small"
        :scroll="{ x: 600 }"
      >
        <a-table-column title="文件名" data-index="name" key="name" />
        <a-table-column title="大小" data-index="size" key="size">
          <template #default="{ record }">
            {{ (record.size / 1024).toFixed(2) }} KB
          </template>
        </a-table-column>
        <a-table-column title="状态" data-index="status" key="status">
          <template #default="{ record }">
            <a-tag
              :color="
                record.status === 'done'
                  ? 'green'
                  : record.status === 'error'
                    ? 'red'
                    : 'blue'
              "
            >
              {{
                record.status === 'done'
                  ? '完成'
                  : record.status === 'error'
                    ? '失败'
                    : '上传中'
              }}
            </a-tag>
          </template>
        </a-table-column>
        <a-table-column title="路径" data-index="path" key="path" ellipsis />
      </a-table>
    </div>
  </div>
</template>

<style scoped>
.directory-drop-zone {
  padding: 40px;
  text-align: center;
  background: #fafafa;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  transition: all 0.3s;
}

.directory-drop-zone:hover {
  background: #f0f8ff;
  border-color: #1890ff;
}

.drop-content {
  cursor: pointer;
}
</style>
