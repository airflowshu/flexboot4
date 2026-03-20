<script lang="ts" setup>
import type { CmsTag } from '#/api/cms/tag';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm, z } from '#/adapter/form';
import { createTag, updateTag } from '#/api/cms/tag';

interface DrawerData {
  mode: 'create' | 'edit';
  record?: CmsTag;
}

const emit = defineEmits<{ success: [] }>();

const mode = ref<'create' | 'edit'>('create');
const currentRecord = ref<CmsTag | null>(null);
const drawerTitle = computed(() =>
  mode.value === 'create' ? '新增标签' : '编辑标签',
);

const DEFAULT_TAG_COLOR = '#1677ff';

const [Form, formApi] = useVbenForm({
  showDefaultActions: false,
  schema: [
    {
      component: 'Input',
      fieldName: 'tagName',
      label: '标签名称',
      rules: 'required',
    },
    {
      component: 'ColorPicker',
      fieldName: 'tagColor',
      label: '标签颜色',
      defaultValue: DEFAULT_TAG_COLOR,
      rules: z
        .string()
        .min(1, '请选择标签颜色')
        .max(20, '标签颜色长度不能超过20个字符'),
      componentProps: {
        format: 'hex',
        showText: true,
      },
    },
    {
      component: 'Textarea',
      fieldName: 'remark',
      label: '备注',
      componentProps: {
        rows: 3,
      },
    },
  ],
});

const [Drawer, drawerApi] = useVbenDrawer({
  onConfirm: async () => {
    try {
      await formApi.validate();
      const values = await formApi.getValues();

      if (mode.value === 'create') {
        await createTag(values);
        message.success('标签新增成功');
      } else if (currentRecord.value?.id) {
        await updateTag(currentRecord.value.id, values);
        message.success('标签更新成功');
      }

      emit('success');
      drawerApi.close();
    } catch (error) {
      console.error(error);
    }
  },
  onOpenChange: (isOpen) => {
    if (!isOpen) {
      return;
    }

    const data = drawerApi.getData<DrawerData>();
    mode.value = data?.mode ?? 'create';
    currentRecord.value = data?.record ?? null;

    if (mode.value === 'edit' && currentRecord.value) {
      formApi.setValues({
        tagName: currentRecord.value.tagName,
        tagColor: currentRecord.value.tagColor || DEFAULT_TAG_COLOR,
      });
    } else {
      formApi.resetForm();
      formApi.setValues({ tagColor: DEFAULT_TAG_COLOR });
    }
  },
});
</script>

<template>
  <Drawer :title="drawerTitle">
    <Form />
  </Drawer>
</template>
