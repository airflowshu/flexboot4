<script lang="ts" setup>
import { ref } from 'vue';

import { useVbenDrawer, useVbenForm } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { createDictItem, updateDictItem } from '#/api/system/dict';

const emit = defineEmits(['success']);

const isUpdate = ref(false);
const recordId = ref('');
const typeId = ref('');

const [Form, formApi] = useVbenForm({
  showDefaultActions: false,
  schema: [
    {
      component: 'Input',
      fieldName: 'itemText',
      label: '名称',
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'itemValue',
      label: '数据值',
      rules: 'required',
    },
    {
      component: 'Input',
      fieldName: 'itemCode',
      label: '编码',
      // rules: 'required', // Optional based on json
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

      // Ensure typeId is included
      // Note: Backend might expect typeId or parentId.
      // Based on JSON `parentCode` exists but `typeCode` also exists.
      // We will assume `typeId` is needed for creation if it's a relational DB.
      // Or we send `typeCode` if we have it.
      // For now, we will assume standard CRUD.

      if (isUpdate.value) {
        await updateDictItem(recordId.value, data);
        message.success('修改成功');
      } else {
        await createDictItem({ ...data, typeId: typeId.value });
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
      typeId.value = data?.typeId;

      if (isUpdate.value && data?.record) {
        recordId.value = data.record.id;
        formApi.setValues(data.record);
      } else {
        recordId.value = '';
        formApi.resetForm();
      }
    }
  },
});
</script>

<template>
  <Drawer :title="isUpdate ? '编辑字典项' : '新增字典项'">
    <Form />
  </Drawer>
</template>
