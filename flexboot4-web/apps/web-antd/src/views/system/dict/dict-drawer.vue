<script lang="ts" setup>
import { ref } from 'vue';

import { useVbenDrawer, useVbenForm } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { createDictType, updateDictType } from '#/api/system/dict';

const emit = defineEmits(['success']);

const isUpdate = ref(false);
const recordId = ref('');

const [Form, formApi] = useVbenForm({
  showDefaultActions: false,
  schema: [
    {
      component: 'Input',
      fieldName: 'name',
      label: '字典名称',
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'code',
      label: '字典编码',
      rules: 'required',
      componentProps: {
        disabled: false, // Will be disabled in update mode if needed
      },
    },
    {
      component: 'InputNumber',
      fieldName: 'orderNo',
      label: '排序',
      defaultValue: 0,
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
    },
  ],
});

const [Drawer, drawerApi] = useVbenDrawer({
  onConfirm: async () => {
    try {
      await formApi.validate();
      const data = await formApi.getValues();

      if (isUpdate.value) {
        await updateDictType(recordId.value, data);
        message.success('修改成功');
      } else {
        await createDictType(data);
        message.success('新增成功');
      }

      emit('success');
      drawerApi.close();
    } catch (error) {
      console.error(error);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<any>();
      isUpdate.value = !!data?.isUpdate;

      if (isUpdate.value && data?.record) {
        recordId.value = data.record.id;
        formApi.setValues(data.record);
        // Disable code editing in update mode if strictly required, but not specified.
        // formApi.updateSchema({ fieldName: 'code', componentProps: { disabled: true } });
      } else {
        recordId.value = '';
        formApi.resetForm();
      }
    }
  },
});
</script>

<template>
  <Drawer :title="isUpdate ? '编辑字典类型' : '新增字典类型'">
    <Form />
  </Drawer>
</template>
