<script lang="ts" setup>
import type { CmsCategory } from '#/api/cms/category';
import type { SearchRequest } from '#/api/common';

import { computed, ref } from 'vue';

import { useVbenDrawer, useVbenForm } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import {
  createCategory,
  getCategoryList,
  updateCategory,
} from '#/api/cms/category';

interface DrawerData {
  mode: 'create' | 'edit';
  record?: CmsCategory;
}

const emit = defineEmits<{
  success: [];
}>();

const mode = ref<'create' | 'edit'>('create');
const currentRecord = ref<CmsCategory | null>(null);
const categoryOptions = ref<Array<{ label: string; value: string }>>([]);

const drawerTitle = computed(() =>
  mode.value === 'create' ? '新增栏目' : '编辑栏目',
);

const [Form, formApi] = useVbenForm({
  showDefaultActions: false,
  schema: [
    {
      component: 'Input',
      fieldName: 'categoryName',
      label: '栏目名称',
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'categoryCode',
      label: '栏目编码',
      rules: 'required',
    },
    {
      component: 'Select',
      fieldName: 'parentId',
      label: '父栏目',
      componentProps: {
        allowClear: true,
        options: [],
        placeholder: '根栏目可留空',
      },
    },
    {
      component: 'Textarea',
      fieldName: 'description',
      label: '栏目描述',
      componentProps: {
        rows: 3,
      },
    },
    {
      component: 'InputNumber',
      fieldName: 'sortOrder',
      label: '排序',
      defaultValue: 0,
      componentProps: {
        min: 0,
      },
    },
    {
      component: 'RadioGroup',
      fieldName: 'status',
      label: '状态',
      defaultValue: 1,
      componentProps: {
        options: [
          { label: '启用', value: 1 },
          { label: '禁用', value: 0 },
        ],
      },
    },
    {
      component: 'Textarea',
      fieldName: 'remark',
      label: '备注',
      componentProps: {
        rows: 2,
      },
    },
  ],
});

async function loadCategoryOptions(excludeId?: string) {
  const params: SearchRequest = {
    pageNumber: 1,
    pageSize: 500,
    orders: [{ column: 'sortOrder', asc: true }],
  };
  const list = await getCategoryList(params);
  categoryOptions.value = list
    .filter((item) => item.id !== excludeId)
    .map((item) => ({
      label: `${item.categoryName} (${item.categoryCode})`,
      value: item.id,
    }));

  formApi.updateSchema([
    {
      fieldName: 'parentId',
      componentProps: {
        allowClear: true,
        options: categoryOptions.value,
        placeholder: '根栏目可留空',
      },
    },
  ]);
}

const [Drawer, drawerApi] = useVbenDrawer({
  onConfirm: async () => {
    try {
      await formApi.validate();
      const values = await formApi.getValues();
      const payload = {
        ...values,
        parentId: values.parentId || undefined,
      };

      if (mode.value === 'create') {
        await createCategory(payload);
        message.success('栏目新增成功');
      } else if (currentRecord.value?.id) {
        await updateCategory(currentRecord.value.id, payload);
        message.success('栏目更新成功');
      }

      emit('success');
      drawerApi.close();
    } catch (error) {
      console.error(error);
    }
  },
  onOpenChange: async (isOpen) => {
    if (!isOpen) return;

    const data = drawerApi.getData<DrawerData>();
    mode.value = data?.mode ?? 'create';
    currentRecord.value = data?.record ?? null;

    await loadCategoryOptions(currentRecord.value?.id);

    if (mode.value === 'edit' && currentRecord.value) {
      formApi.setValues({
        categoryName: currentRecord.value.categoryName,
        categoryCode: currentRecord.value.categoryCode,
        parentId: currentRecord.value.parentId || undefined,
        description: currentRecord.value.description,
        sortOrder: currentRecord.value.sortOrder ?? 0,
        status: currentRecord.value.status ?? 1,
        remark: (currentRecord.value as { remark?: string }).remark,
      });
    } else {
      formApi.resetForm();
      formApi.setValues({
        status: 1,
        sortOrder: 0,
      });
    }
  },
});
</script>

<template>
  <Drawer :title="drawerTitle">
    <Form />
  </Drawer>
</template>
