<script lang="ts" setup>
import type { VbenFormProps } from '#/adapter/form';

import { ref } from 'vue';

import { useVbenDrawer, useVbenForm, z } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { createConfig } from '#/api/devops/sysconfig';

interface Emits {
  (e: 'success'): void;
}

const emit = defineEmits<Emits>();

const loading = ref(false);
const currentConfigType = ref<string>('STRING');

// 配置类型选项
const typeOptions = [
  { value: 'STRING', label: '字符串' },
  { value: 'NUMBER', label: '数字' },
  { value: 'BOOLEAN', label: '布尔值' },
  { value: 'JSON', label: 'JSON' },
  { value: 'ARRAY', label: '数组' },
];

// 根据配置类型获取校验规则
function getValueRules(type: string) {
  switch (type) {
    case 'ARRAY': {
      return z
        .string()
        .min(1, '请输入数组')
        .refine(
          (val) => {
            try {
              const parsed = JSON.parse(val);
              return Array.isArray(parsed);
            } catch {
              return false;
            }
          },
          { message: '请输入有效的JSON数组格式' },
        );
    }
    case 'BOOLEAN': {
      return z
        .string()
        .min(1, '请输入布尔值')
        .regex(/^(true|false|0|1)$/i, '请输入 true、false、0 或 1');
    }
    case 'JSON': {
      return z
        .string()
        .min(1, '请输入JSON')
        .refine(
          (val) => {
            try {
              JSON.parse(val);
              return true;
            } catch {
              return false;
            }
          },
          { message: '请输入有效的JSON格式' },
        );
    }
    case 'NUMBER': {
      return z
        .string()
        .min(1, '请输入数字')
        .regex(/^-?\d+(\.\d+)?$/, '请输入有效的数字');
    }
    default: {
      return z.string().min(1, '请输入配置值');
    }
  }
}

// 根据配置类型获取 placeholder
function getValuePlaceholder(type: string) {
  switch (type) {
    case 'ARRAY': {
      return '请输入JSON数组格式，如：["a", "b", "c"]';
    }
    case 'BOOLEAN': {
      return '请输入 true、false、0 或 1';
    }
    case 'JSON': {
      return '请输入JSON格式，如：{"key": "value"}';
    }
    case 'NUMBER': {
      return '请输入数字，如：100、-5.5';
    }
    default: {
      return '请输入配置值';
    }
  }
}

// 根据配置类型更新配置值字段的校验规则
function updateValueRules(type: string) {
  currentConfigType.value = type;
  const rules = getValueRules(type);
  formApi.updateSchema([
    {
      fieldName: 'configValue',
      rules,
      componentProps: {
        placeholder: getValuePlaceholder(type),
      },
    },
  ]);
}

const formOptions: VbenFormProps = {
  collapsed: false,
  showDefaultActions: false,
  schema: [
    {
      component: 'Input',
      fieldName: 'configKey',
      label: '配置键',
      rules: z.string().min(1, '请输入配置键').max(100, '配置键最多100个字符'),
      componentProps: {
        placeholder: '请输入配置键，如 system.name',
      },
    },
    {
      component: 'Select',
      fieldName: 'configType',
      label: '配置类型',
      rules: z.string().min(1, '请选择配置类型'),
      componentProps: {
        placeholder: '请选择配置类型',
        options: typeOptions,
        onChange: (value: string) => updateValueRules(value),
      },
    },
    {
      component: 'Input',
      fieldName: 'configValue',
      label: '配置值',
      rules: z.string().min(1, '请输入配置值'),
      componentProps: {
        placeholder: '请输入配置值',
      },
    },
    {
      component: 'Input',
      fieldName: 'description',
      label: '描述',
      componentProps: {
        placeholder: '请输入配置描述',
      },
    },
    {
      component: 'RadioGroup',
      fieldName: 'status',
      label: '状态',
      rules: z.union([z.literal(0), z.literal(1)]),
      componentProps: {
        options: [
          { value: 1, label: '启用' },
          { value: 0, label: '禁用' },
        ],
      },
    },
    {
      component: 'Input',
      fieldName: 'remark',
      label: '备注',
      componentProps: {
        placeholder: '请输入备注',
      },
    },
  ],
  wrapperClass: 'grid-cols-1',
};

const [Form, formApi] = useVbenForm(formOptions);

// 手动校验配置值格式
async function validateConfigValue(
  value: string,
  type: string,
): Promise<boolean> {
  if (!value) return false;

  switch (type) {
    case 'BOOLEAN': {
      return /^(?:true|false|0|1)$/i.test(value);
    }
    case 'NUMBER': {
      return /^-?\d+(?:\.\d+)?$/.test(value);
    }
    case 'JSON': {
      try {
        JSON.parse(value);
        return true;
      } catch {
        return false;
      }
    }
    case 'ARRAY': {
      try {
        const parsed = JSON.parse(value);
        return Array.isArray(parsed);
      } catch {
        return false;
      }
    }
    default: {
      return true;
    }
  }
}

const [Drawer, drawerApi] = useVbenDrawer({
  onCancel() {
    drawerApi.close();
  },
  onConfirm: async () => {
    try {
      loading.value = true;
      const data = await formApi.getValues();

      // 手动校验配置值格式
      const isValid = await validateConfigValue(
        data.configValue,
        data.configType,
      );
      if (!isValid) {
        message.error(
          `配置值格式不正确，请检查是否为有效的 ${data.configType} 格式`,
        );
        return;
      }

      await formApi.validate();
      await createConfig(data);
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
      await formApi.resetForm();
      currentConfigType.value = 'STRING';
      // 重置后需要重新设置默认值
      await formApi.setFieldValue('status', 1);
    }
  },
});
</script>

<template>
  <Drawer title="新增配置" :loading="loading">
    <Form />
  </Drawer>
</template>
