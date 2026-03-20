<script setup lang="ts">
import { ref } from 'vue';

import { Card, Radio } from 'ant-design-vue';

import AvatarUpload from './avatar.vue';
import BasicUpload from './basic.vue';
import CustomButtonUpload from './custom-button.vue';
import DirectoryUpload from './directory.vue';
import DragUpload from './drag.vue';
import ManualUpload from './manual.vue';
import PictureWallUpload from './picture-wall.vue';
import PreviewUpload from './preview.vue';

const uploadType = ref('basic');
</script>

<template>
  <div class="p-4">
    <Card title="上传组件示例" class="mb-4">
      <Radio.Group v-model:value="uploadType" button-style="solid">
        <Radio.Button value="basic">基础使用</Radio.Button>
        <Radio.Button value="drag">拖拽上传</Radio.Button>
        <Radio.Button value="manual">手动上传</Radio.Button>
        <Radio.Button value="picture-wall">图片墙</Radio.Button>
        <Radio.Button value="avatar">头像上传</Radio.Button>
        <Radio.Button value="preview">带预览</Radio.Button>
        <Radio.Button value="custom-button">自定义按钮</Radio.Button>
        <Radio.Button value="directory">目录上传</Radio.Button>
      </Radio.Group>
    </Card>

    <Card title="演示">
      <BasicUpload v-if="uploadType === 'basic'" />
      <DragUpload v-if="uploadType === 'drag'" />
      <ManualUpload v-if="uploadType === 'manual'" />
      <PictureWallUpload v-if="uploadType === 'picture-wall'" />
      <AvatarUpload v-if="uploadType === 'avatar'" />
      <PreviewUpload v-if="uploadType === 'preview'" />
      <CustomButtonUpload v-if="uploadType === 'custom-button'" />
      <DirectoryUpload v-if="uploadType === 'directory'" />
    </Card>
  </div>
</template>
