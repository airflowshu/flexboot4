<script lang="ts" setup>
import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createDept, updateDept } from '#/api/system/dept';
import type { DeptRecord } from '#/api/system/dept';

const emit = defineEmits(['success']);

const isUpdate = ref(false);
const recordId = ref('');

const [Form, formApi] = useVbenForm({
  showDefaultActions: false,
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    labelWidth: 100,
  },
  schema: [
    {
      component: 'Input',
      fieldName: 'deptName',
      label: '部门名称',
      rules: 'required',
    },
    {
      component: 'TreeSelect',
      componentProps: {
        fieldNames: {
          label: 'deptName',
          value: 'id',
          children: 'children',
        },
        treeData: [],
      },
      fieldName: 'parentId',
      label: '上级部门',
    },
    {
      component: 'InputNumber',
      defaultValue: 0,
      fieldName: 'orderNo',
      label: '排序',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '启用', value: 1 },
          { label: '禁用', value: 0 },
        ],
      },
      defaultValue: 1,
      fieldName: 'status',
      label: '状态',
    },
    {
      component: 'Textarea',
      fieldName: 'remark',
      label: '备注',
    },
  ],
});

const [Drawer, drawerApi] = useVbenDrawer({
  onCancel: () => {
    drawerApi.close();
  },
  onConfirm: async () => {
    try {
      await formApi.validate();
      const values = await formApi.getValues();
      drawerApi.setState({ confirmLoading: true });

      if (isUpdate.value) {
        await updateDept(recordId.value, values);
        message.success('修改成功');
      } else {
        await createDept(values);
        message.success('新增成功');
      }

      emit('success');
      drawerApi.close();
    } catch (error) {
      console.error(error);
    } finally {
      drawerApi.setState({ confirmLoading: false });
    }
  },
  onOpenChange: async (isOpen) => {
    if (isOpen) {
      const data = drawerApi.getData<{
        deptTree: DeptRecord[];
        isUpdate: boolean;
        record?: DeptRecord;
      }>();
      isUpdate.value = !!data?.isUpdate;

      if (data?.deptTree) {
        formApi.updateSchema([
          {
            componentProps: {
              treeData: data.deptTree,
            },
            fieldName: 'parentId',
          },
        ]);
      }

      if (isUpdate.value && data?.record) {
        recordId.value = data.record.id;
        await formApi.setValues(data.record);
      } else {
        recordId.value = '';
        await formApi.resetForm();
      }
    }
  },
  title: '部门管理',
});
</script>

<template>
  <Drawer>
    <Form />
  </Drawer>
</template>
