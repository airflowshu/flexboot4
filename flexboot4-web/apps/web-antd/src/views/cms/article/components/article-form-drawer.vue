<script lang="ts" setup>
import type { FormInstance } from 'ant-design-vue';

import type { CmsArticle } from '#/api/cms/article';
import type { CmsCategory } from '#/api/cms/category';
import type { CmsTag } from '#/api/cms/tag';
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

import {
  createArticle,
  getArticleById,
  updateArticle,
} from '#/api/cms/article';
import {
  createArticleTag,
  deleteArticleTag,
  getArticleTagList,
} from '#/api/cms/article-tag';
import { getCategoryList } from '#/api/cms/category';
import { getTagList } from '#/api/cms/tag';
import { uploadCmsFileApi } from '#/api/cms/upload';

interface DrawerData {
  mode: 'create' | 'edit';
  record?: CmsArticle;
}

interface ArticleFormModel {
  author: string;
  categoryId: string;
  content: string;
  coverFileId: string;
  sortOrder: number;
  status: 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'REJECTED';
  summary: string;
  tagIds: string[];
  title: string;
}

const emit = defineEmits<{
  success: [];
}>();

const mode = ref<'create' | 'edit'>('create');
const articleId = ref('');
const categoryOptions = ref<CmsCategory[]>([]);
const tagOptions = ref<CmsTag[]>([]);
const coverPreviewUrl = ref('');
const uploadLoading = ref(false);
const fileInputRef = ref<HTMLInputElement | null>(null);

const model = reactive<ArticleFormModel>({
  author: '',
  categoryId: '',
  content: '',
  coverFileId: '',
  sortOrder: 0,
  status: 'DRAFT',
  summary: '',
  tagIds: [],
  title: '',
});

const formRef = ref<FormInstance>();

function resetModel() {
  model.author = '';
  model.categoryId = '';
  model.content = '';
  model.coverFileId = '';
  model.sortOrder = 0;
  model.status = 'DRAFT';
  model.summary = '';
  model.tagIds = [];
  model.title = '';
  coverPreviewUrl.value = '';
}

async function loadOptions() {
  const listParams: SearchRequest = {
    pageNumber: 1,
    pageSize: 500,
    orders: [{ column: 'sortOrder', asc: true }],
  };
  categoryOptions.value = await getCategoryList(listParams);
  tagOptions.value = await getTagList({
    pageNumber: 1,
    pageSize: 500,
    orders: [{ column: 'tagName', asc: true }],
  });
}

async function loadTagIds(currentArticleId: string) {
  const rels = await getArticleTagList({
    pageNumber: 1,
    pageSize: 500,
    items: [{ field: 'articleId', op: 'eq', val: currentArticleId }],
    logic: 'AND',
  });
  model.tagIds = rels.map((item) => item.tagId);
}

async function syncArticleTags(currentArticleId: string) {
  const rels = await getArticleTagList({
    pageNumber: 1,
    pageSize: 500,
    items: [{ field: 'articleId', op: 'eq', val: currentArticleId }],
    logic: 'AND',
  });

  const currentTagIds = new Set(rels.map((item) => item.tagId));
  const selectedTagIds = new Set(model.tagIds);

  const removeTargets = rels.filter((item) => !selectedTagIds.has(item.tagId));
  for (const item of removeTargets) {
    await deleteArticleTag(item.id);
  }

  for (const tagId of model.tagIds) {
    if (!currentTagIds.has(tagId)) {
      await createArticleTag({ articleId: currentArticleId, tagId });
    }
  }
}

function getFileUrl(fileData: {
  location?: { bucket?: string; endpoint?: string; objectKey?: string };
}) {
  const endpoint = fileData.location?.endpoint;
  const bucket = fileData.location?.bucket;
  const objectKey = fileData.location?.objectKey;
  if (!endpoint || !bucket || !objectKey) return '';
  return `${endpoint}/${bucket}/${objectKey}`;
}

function triggerCoverUpload() {
  fileInputRef.value?.click();
}

async function handleCoverChange(event: Event) {
  const files = (event.target as HTMLInputElement).files;
  if (!files?.length) return;

  const file = files[0] as File;
  try {
    uploadLoading.value = true;
    const fileData = await uploadCmsFileApi(file, {
      bizId: articleId.value || undefined,
      tenantId: '1',
    });
    model.coverFileId = fileData.id || '';
    coverPreviewUrl.value = getFileUrl(fileData);
    message.success('封面上传成功');
  } catch (error) {
    console.error(error);
    message.error('封面上传失败');
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

      const payload = {
        title: model.title,
        categoryId: model.categoryId,
        author: model.author,
        coverFileId: model.coverFileId || undefined,
        summary: model.summary,
        content: model.content,
        status: model.status,
        sortOrder: model.sortOrder,
      };

      if (mode.value === 'create') {
        await createArticle(payload);
        message.success('文章创建成功');
      } else if (articleId.value) {
        await updateArticle(articleId.value, payload);
        await syncArticleTags(articleId.value);
        message.success('文章更新成功');
      }

      emit('success');
      drawerApi.close();
    } catch (error) {
      console.error(error);
    }
  },
  onOpenChange: async (open) => {
    if (!open) return;

    const data = drawerApi.getData<DrawerData>();
    mode.value = data?.mode ?? 'create';
    resetModel();
    await loadOptions();

    if (mode.value === 'edit' && data?.record?.id) {
      articleId.value = data.record.id;
      const detail = await getArticleById(data.record.id);
      model.title = detail.title;
      model.categoryId = detail.categoryId;
      model.author = detail.author || '';
      model.coverFileId = detail.coverFileId || '';
      model.summary = detail.summary || '';
      model.content = detail.content || '';
      model.status = detail.status || 'DRAFT';
      model.sortOrder = detail.sortOrder ?? 0;
      coverPreviewUrl.value = getFileUrl(detail.coverFile || {});
      await loadTagIds(data.record.id);
    } else {
      articleId.value = '';
    }
  },
});
</script>

<template>
  <Drawer
    :title="mode === 'create' ? '创建文章' : '编辑文章'"
    :fullscreen="true"
  >
    <Form ref="formRef" :model="model" layout="vertical">
      <Form.Item
        label="文章标题"
        name="title"
        :rules="[{ required: true, message: '请输入文章标题' }]"
      >
        <Input v-model:value="model.title" placeholder="请输入文章标题" />
      </Form.Item>

      <Form.Item
        label="栏目"
        name="categoryId"
        :rules="[{ required: true, message: '请选择栏目' }]"
      >
        <Select
          v-model:value="model.categoryId"
          placeholder="请选择栏目"
          :options="
            categoryOptions.map((item) => ({
              value: item.id,
              label: item.categoryName,
            }))
          "
        />
      </Form.Item>

      <Form.Item label="作者" name="author">
        <Input v-model:value="model.author" placeholder="请输入作者" />
      </Form.Item>

      <Form.Item label="标签" name="tagIds">
        <Select
          v-model:value="model.tagIds"
          mode="multiple"
          placeholder="可多选标签"
          :options="
            tagOptions.map((item) => ({ value: item.id, label: item.tagName }))
          "
        />
      </Form.Item>

      <Form.Item label="封面文件ID" name="coverFileId">
        <div class="flex items-center gap-2">
          <Input
            v-model:value="model.coverFileId"
            placeholder="请上传封面或手动填写文件ID"
          />
          <input
            ref="fileInputRef"
            type="file"
            class="hidden"
            accept="image/*"
            @change="handleCoverChange"
          />
          <Button :loading="uploadLoading" @click="triggerCoverUpload">
            上传封面
          </Button>
        </div>
        <a v-if="coverPreviewUrl" :href="coverPreviewUrl" target="_blank"
          >预览封面</a
        >
      </Form.Item>

      <Form.Item label="摘要" name="summary">
        <Input.TextArea v-model:value="model.summary" :rows="3" />
      </Form.Item>

      <Form.Item label="内容" name="content">
        <Input.TextArea v-model:value="model.content" :rows="12" />
      </Form.Item>

      <Form.Item label="状态" name="status">
        <Select
          v-model:value="model.status"
          :options="[
            { value: 'DRAFT', label: '草稿' },
            { value: 'PENDING', label: '待审核' },
            { value: 'PUBLISHED', label: '已发布' },
            { value: 'REJECTED', label: '已驳回' },
          ]"
        />
      </Form.Item>

      <Form.Item label="排序" name="sortOrder">
        <InputNumber v-model:value="model.sortOrder" :min="0" />
      </Form.Item>
    </Form>
  </Drawer>
</template>
