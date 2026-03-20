<script lang="ts" setup>
import type { VbenFormProps } from '#/adapter/form';
import type { SysConfig } from '#/api/devops/sysconfig';

import { computed } from 'vue';

import { useVbenDrawer, useVbenForm } from '@vben/common-ui';

defineOptions({ name: 'DetailDrawer' });

// 配置类型选项
const typeOptions = [
  { value: 'STRING', label: '字符串' },
  { value: 'NUMBER', label: '数字' },
  { value: 'BOOLEAN', label: '布尔值' },
  { value: 'JSON', label: 'JSON' },
  { value: 'ARRAY', label: '数组' },
];

const formOptions: VbenFormProps = {
  collapsed: false,
  showDefaultActions: false,
  commonConfig: {
    componentProps: {
      disabled: true,
    },
  },
  schema: [
    {
      component: 'Input',
      fieldName: 'configKey',
      label: '配置键',
    },
    {
      component: 'Input',
      fieldName: 'configValue',
      label: '配置值',
    },
    {
      component: 'Select',
      fieldName: 'configType',
      label: '配置类型',
      componentProps: {
        options: typeOptions,
      },
    },
    {
      component: 'Input',
      fieldName: 'description',
      label: '描述',
    },
    {
      component: 'RadioGroup',
      fieldName: 'status',
      label: '状态',
      componentProps: {
        options: [
          { value: 1, label: '启用' },
          { value: 0, label: '禁用' },
        ],
      },
    },
    {
      component: 'Input',
      fieldName: 'createTime',
      label: '创建时间',
    },
    {
      component: 'Input',
      fieldName: 'lastModifyTime',
      label: '更新时间',
    },
    {
      component: 'Input',
      fieldName: 'remark',
      label: '备注',
    },
  ],
  wrapperClass: 'grid-cols-1',
};

const [Form, formApi] = useVbenForm(formOptions);

const detailData = computed(() => {
  return drawerApi.getData<SysConfig>() || {};
});

const [Drawer, drawerApi] = useVbenDrawer({
  connectedComponent: undefined,
  destroyOnClose: true,
  placement: 'right',
  showCancelButton: false,
  showConfirmButton: false,
  onOpenChange: async (open) => {
    if (open) {
      await formApi.setValues(detailData.value);
    }
  },
});
</script>

<template>
  <Drawer title="配置详情" class="w-[600px]">
    <Form />
  </Drawer>
</template>
