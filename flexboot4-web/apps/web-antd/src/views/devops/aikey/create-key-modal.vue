<script lang="ts" setup>
import type { UserOption } from '#/api/devops/aikey';

import { ref, shallowRef } from 'vue';

import { useVbenDrawer, useVbenForm, z } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { createApiKey, getUserOptions } from '#/api/devops/aikey';

defineOptions({ name: 'CreateKeyModal' });

const emit = defineEmits(['success']);

const userOptions = shallowRef<UserOption[]>([]);
const loading = ref(false);

// 加载用户列表
async function loadUserOptions() {
  try {
    const resp = await getUserOptions();
    userOptions.value = resp.map((user) => ({
      id: user.id,
      username: user.username,
      realName: user.realName,
    }));
  } catch (error) {
    console.error('加载用户列表失败', error);
    message.error('加载用户列表失败');
  }
}

const [Form, formApi] = useVbenForm({
  showDefaultActions: false,
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    labelClass: 'w-1/4',
  },
  layout: 'horizontal',
  schema: [
    {
      component: 'Select',
      fieldName: 'userId',
      label: '所属用户',
      rules: 'required',
      componentProps: {
        placeholder: '请选择所属用户',
        options: userOptions,
        showSearch: true,
        optionFilterProp: 'realName',
        fieldNames: { label: 'realName', value: 'id' },
        filterOption: (
          input: string,
          option: { realName: string; username: string },
        ) => {
          const realName = option.realName?.toLowerCase() || '';
          const username = option.username?.toLowerCase() || '';
          const searchInput = input.toLowerCase();
          return (
            realName.includes(searchInput) || username.includes(searchInput)
          );
        },
        onFocus: loadUserOptions,
      },
    },
    {
      component: 'Input',
      fieldName: 'keyName',
      label: 'Key名称',
      rules: z.string().min(1, '请输入Key名称').max(50, 'Key名称最多50个字符'),
    },
    {
      component: 'InputNumber',
      fieldName: 'quote',
      label: '配额',
      rules: 'required',
      componentProps: {
        placeholder: '请输入配额',
        min: 0,
        max: 99_999_999,
        addonAfter: '次',
      },
    },
    {
      component: 'Input',
      fieldName: 'modelScope',
      label: '模型范围',
      rules: 'required',
      componentProps: {
        placeholder: '请输入模型范围，如: * 或 gpt-4,gpt-3.5-turbo',
      },
    },
    {
      component: 'DatePicker',
      fieldName: 'expiresAt',
      label: '过期时间',
      componentProps: {
        placeholder: '可选，不填则永不过期',
        showTime: true,
        format: 'YYYY-MM-DD HH:mm:ss',
        valueFormat: 'YYYY-MM-DDTHH:mm:ss',
        style: { width: '100%' },
      },
    },
    {
      component: 'Textarea',
      fieldName: 'remark',
      label: '备注',
      componentProps: {
        placeholder: '可选',
        rows: 3,
        maxlength: 200,
        showCount: true,
      },
    },
  ],
  wrapperClass: 'grid-cols-1',
});

const [Drawer, drawerApi] = useVbenDrawer({
  onCancel() {
    drawerApi.close();
  },
  onConfirm: async () => {
    try {
      loading.value = true;
      await formApi.validate();
      const data = await formApi.getValues();
      await createApiKey(data);
      message.success('创建成功');
      await drawerApi.close();
      emit('success');
    } catch (error) {
      console.error(error);
      message.error('创建失败');
    } finally {
      loading.value = false;
    }
  },
  onOpenChange: async (open) => {
    if (open) {
      // 打开时重置表单并加载用户列表
      await formApi.resetForm();
      await loadUserOptions();
    }
  },
});
</script>

<template>
  <Drawer title="创建API Key" :loading="loading">
    <Form />
  </Drawer>
</template>
