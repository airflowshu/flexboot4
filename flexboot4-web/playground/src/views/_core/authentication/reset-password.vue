<script lang="ts" setup>
import type { VbenFormSchema } from '@vben/common-ui';
import type { Recordable } from '@vben/types';

import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { AuthenticationResetPassword, z } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { message } from 'ant-design-vue';

import { resetPasswordApi } from '#/api/core/auth';

defineOptions({ name: 'ResetPassword' });

const route = useRoute();
const router = useRouter();

const loading = ref(false);

// 从 URL 获取 token 和 email 参数
const token = computed(() => (route.query.token as string) || '');
const email = computed(() => (route.query.email as string) || '');

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      component: 'VbenInput',
      componentProps: {
        placeholder: $t('authentication.newPasswordTip'),
        type: 'password',
      },
      fieldName: 'newPassword',
      label: $t('authentication.newPassword'),
      rules: z
        .string()
        .min(1, { message: $t('authentication.newPasswordTip') })
        .min(8, { message: '密码长度至少8位' }),
    },
    {
      component: 'VbenInput',
      componentProps: {
        placeholder: $t('authentication.confirmNewPasswordTip'),
        type: 'password',
      },
      fieldName: 'confirmPassword',
      label: $t('authentication.confirmNewPassword'),
      rules: z
        .string()
        .min(1, { message: $t('authentication.confirmNewPasswordTip') }),
    },
  ];
});

async function handleSubmit(value: Recordable<any>) {
  if (!token.value) {
    message.error('无效的重置链接');
    return;
  }

  // 校验两次密码是否一致
  if (value.newPassword !== value.confirmPassword) {
    message.error('两次输入的密码不一致');
    return;
  }

  loading.value = true;
  try {
    await resetPasswordApi({
      token: token.value,
      email: email.value || value.email || '',
      newPassword: value.newPassword,
    });
    message.success($t('authentication.passwordResetSuccess'));
    // 跳转到登录页
    router.push('/auth/login');
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <AuthenticationResetPassword
    :email="email"
    :form-schema="formSchema"
    :loading="loading"
    :token="token"
    @submit="handleSubmit"
  />
</template>
