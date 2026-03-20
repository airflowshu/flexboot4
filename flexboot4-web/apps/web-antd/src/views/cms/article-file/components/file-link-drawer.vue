<script lang="ts" setup>
import type { FormInstance } from 'ant-design-vue';

import type { CmsArticle } from '#/api/cms/article';
import type { SearchRequest } from '#/api/common';

import { reactive, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import {
  Button,
  Form,
  Input,
  InputNumber,
  message,
  Select,
} from 'ant-design-vue';

import { getArticleList } from '#/api/cms/article';
import { createArticleFile } from '#/api/cms/article-file';
import { uploadCmsFileApi } from '#/api/cms/upload';

const emit = defineEmits<{
  success: [];
}>();

const articleOptions = ref<CmsArticle[]>([]);
const fileInputRef = ref<HTMLInputElement | null>(null);
const uploadLoading = ref(false);
const formRef = ref<FormInstance>();

const model = reactive({
  articleId: '',
  fileId: '',
  sortOrder: 0,
});

async function loadArticles() {
  const params: SearchRequest = {
    pageNumber: 1,
    pageSize: 500,
    orders: [{ column: 'lastModifyTime', asc: false }],
  };
  articleOptions.value = await getArticleList(params);
}

function resetModel() {
  model.articleId = '';
  model.fileId = '';
  model.sortOrder = 0;
}

function triggerUpload() {
  fileInputRef.value?.click();
}

async function handleFileChange(event: Event) {
  const files = (event.target as HTMLInputElement).files;
  if (!files?.length) return;

  try {
    uploadLoading.value = true;
    const file = files[0] as File;
    const result = await uploadCmsFileApi(file, {
      bizId: model.articleId || undefined,
      tenantId: '1',
    });
    model.fileId = result.id || '';
    message.success('附件上传成功');
  } catch (error) {
    console.error(error);
    message.error('附件上传失败');
  } finally {
    uploadLoading.value = false;
    if (fileInputRef.value) {
      fileInputRef.value.value = '';
    }
  }
}

const [Drawer, drawerApi] = useVbenDrawer({
  onConfirm: async () => {
    try {
      await formRef.value?.validate();
      await createArticleFile({
        articleId: model.articleId,
        fileId: model.fileId,
        sortOrder: model.sortOrder,
      });
      message.success('附件关联成功');
      emit('success');
      drawerApi.close();
    } catch (error) {
      console.error(error);
    }
  },
  onOpenChange: async (open) => {
    if (!open) return;
    resetModel();
    await loadArticles();
  },
});
</script>

<template>
  <Drawer title="关联附件">
    <Form ref="formRef" layout="vertical" :model="model">
      <Form.Item
        label="文章"
        name="articleId"
        :rules="[{ required: true, message: '请选择文章' }]"
      >
        <Select
          v-model:value="model.articleId"
          placeholder="请选择文章"
          :options="
            articleOptions.map((item) => ({
              value: item.id,
              label: item.title,
            }))
          "
        />
      </Form.Item>

      <Form.Item
        label="文件ID"
        name="fileId"
        :rules="[{ required: true, message: '请上传文件或填写文件ID' }]"
      >
        <div class="flex items-center gap-2">
          <Input v-model:value="model.fileId" placeholder="文件ID" />
          <input
            ref="fileInputRef"
            type="file"
            class="hidden"
            @change="handleFileChange"
          />
          <Button :loading="uploadLoading" @click="triggerUpload">
            上传文件
          </Button>
        </div>
      </Form.Item>

      <Form.Item label="排序" name="sortOrder">
        <InputNumber v-model:value="model.sortOrder" :min="0" />
      </Form.Item>
    </Form>
  </Drawer>
</template>
