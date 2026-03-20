<script lang="ts" setup>
import type { SysVersionLog } from '#/api/system/versionlog';

import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { message } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenForm, z } from '#/adapter/form';
import { createVersionLog, updateVersionLog } from '#/api/system/versionlog';

const emit = defineEmits<{
  (e: 'success'): void;
}>();

const isUpdate = ref(false);
const recordId = ref<null | string>(null);

function splitLines(value: null | string | undefined) {
  if (!value) {
    return [];
  }
  return value
    .split('\n')
    .map((item) => item.trim())
    .filter((item) => !!item);
}

const [Form, formApi] = useVbenForm({
  showDefaultActions: false,
  commonConfig: {
    colon: true,
    componentProps: {
      class: 'w-full',
    },
  },
  handleSubmit: onSubmit,
  layout: 'vertical',
  // 禁用表单内部的按钮，只使用抽屉底部的按钮
  schema: [
    {
      component: 'Input',
      componentProps: {
        placeholder: '请输入版本号 (e.g. 1.0.0)',
      },
      fieldName: 'versionNo',
      label: '版本号',
      rules: z.string().min(1, '请输入版本号'),
    },
    {
      component: 'DatePicker',
      componentProps: {
        format: 'YYYY-MM-DD HH:mm:ss',
        placeholder: '请选择发布日期',
        showTime: true,
        valueFormat: 'YYYY-MM-DDTHH:mm:ss',
      },
      fieldName: 'releaseDate',
      label: '发布日期',
      rules: z.string().min(1, '请选择发布日期'),
    },
    {
      component: 'Select',
      componentProps: {
        options: [
          { label: 'Major (重大更新)', value: 'major' },
          { label: 'Minor (次要更新)', value: 'minor' },
          { label: 'Patch (补丁修复)', value: 'patch' },
        ],
        placeholder: '请选择更新类型',
      },
      fieldName: 'type',
      label: '更新类型',
      rules: z.string().min(1, '请选择更新类型'),
    },
    {
      component: 'Input',
      componentProps: {
        placeholder: '请输入更新标题',
      },
      fieldName: 'title',
      label: '标题',
      rules: z.string().min(1, '请输入标题'),
    },
    {
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入更新描述',
        rows: 3,
      },
      fieldName: 'description',
      label: '描述',
    },
    {
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入新特性，每行一条',
        rows: 4,
      },
      fieldName: 'features',
      label: '新特性 (每行一条)',
    },
    {
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入修复的问题，每行一条',
        rows: 4,
      },
      fieldName: 'fixes',
      label: '修复问题 (每行一条)',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '草稿', value: 0 },
          { label: '已发布', value: 1 },
        ],
      },
      defaultValue: 1,
      fieldName: 'status',
      label: '状态',
    },
  ],
});

const [Drawer, drawerApi] = useVbenDrawer({
  onCancel() {
    drawerApi.close();
  },
  onConfirm: async () => {
    await formApi.validate();
    await formApi.submitForm();
  },
  onOpenChange(isOpen: boolean) {
    if (isOpen) {
      const { record } = drawerApi.getData<{ record: SysVersionLog }>() || {};
      if (record) {
        isUpdate.value = true;
        recordId.value = record.id || null;
        drawerApi.setState({ title: '编辑版本' });

        // Transform arrays to strings for Textarea
        const featuresStr = Array.isArray(record.features)
          ? record.features.join('\n')
          : record.features || '';
        const fixesStr = Array.isArray(record.fixes)
          ? record.fixes.join('\n')
          : record.fixes || '';

        formApi.setValues({
          ...record,
          releaseDate: record.releaseDate
            ? dayjs(record.releaseDate).format('YYYY-MM-DDTHH:mm:ss')
            : undefined,
          features: featuresStr,
          fixes: fixesStr,
        });
      } else {
        isUpdate.value = false;
        recordId.value = null;
        drawerApi.setState({ title: '新增版本' });
        formApi.resetForm();
        // Set default values if needed
        formApi.setValues({
          status: 1,
          type: 'minor',
          releaseDate: dayjs().format('YYYY-MM-DDTHH:mm:ss'),
        });
      }
    }
  },
});

async function onSubmit(values: Record<string, any>) {
  try {
    drawerApi.setState({ confirmLoading: true });

    const payload = {
      ...values,
      features: splitLines(values.features),
      fixes: splitLines(values.fixes),
    };

    if (isUpdate.value && recordId.value) {
      await updateVersionLog(recordId.value, payload);
      message.success('更新成功');
    } else {
      await createVersionLog(payload);
      message.success('新增成功');
    }
    emit('success');
    await drawerApi.close();
  } catch (error) {
    console.error(error);
  } finally {
    drawerApi.setState({ confirmLoading: false });
  }
}
</script>

<template>
  <Drawer>
    <Form />
  </Drawer>
</template>
