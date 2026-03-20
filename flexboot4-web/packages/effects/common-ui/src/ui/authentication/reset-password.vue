<script setup lang="ts">
import type { Recordable } from '@vben/types';

import type { VbenFormSchema } from '@vben-core/form-ui';

import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { $t } from '@vben/locales';

import { useVbenForm } from '@vben-core/form-ui';
import { VbenButton } from '@vben-core/shadcn-ui';

import Title from './auth-title.vue';

interface Props {
  /**
   * @zh_CN 邮箱地址（从 URL 或父组件传入）
   */
  email?: string;
  /**
   * @zh_CN 重置令牌（从 URL 或父组件传入）
   */
  token?: string;
  /**
   * @zh_CN 表单 Schema
   */
  formSchema?: VbenFormSchema[];
  /**
   * @zh_CN 是否处于加载处理状态
   */
  loading?: boolean;
  /**
   * @zh_CN 登录路径
   */
  loginPath?: string;
  /**
   * @zh_CN 标题
   */
  title?: string;
  /**
   * @zh_CN 描述
   */
  subTitle?: string;
  /**
   * @zh_CN 按钮文本
   */
  submitButtonText?: string;
}

defineOptions({
  name: 'AuthenticationResetPassword',
});

const props = withDefaults(defineProps<Props>(), {
  email: '',
  token: '',
  formSchema: () => [],
  loading: false,
  loginPath: '/auth/login',
  submitButtonText: '',
  subTitle: '',
  title: '',
});

const emit = defineEmits<{
  submit: [Recordable<any>];
}>();

const [Form, formApi] = useVbenForm(
  reactive({
    commonConfig: {
      hideLabel: true,
      hideRequiredMark: true,
    },
    schema: computed(() => props.formSchema),
    showDefaultActions: false,
  }),
);

const router = useRouter();

async function handleSubmit() {
  const { valid } = await formApi.validate();
  const values = await formApi.getValues();
  if (valid) {
    // 将 token 和 email 一起提交
    emit('submit', {
      ...values,
      email: props.email,
      token: props.token,
    });
  }
}

function goToLogin() {
  router.push(props.loginPath);
}

defineExpose({
  getFormApi: () => formApi,
});
</script>

<template>
  <div>
    <Title>
      <slot name="title">
        {{ title || $t('authentication.resetPassword') }} 🔒
      </slot>
      <template #desc>
        <slot name="subTitle">
          {{ subTitle || $t('authentication.resetPasswordSubtitle') }}
        </slot>
      </template>
    </Title>

    <div v-if="email" class="email-display">
      <span class="i-ant-design:mail-outlined mr-2"></span>
      {{ email }}
    </div>

    <Form />

    <VbenButton
      :class="{
        'cursor-wait': loading,
      }"
      :loading="loading"
      aria-label="reset-password"
      class="mt-2 w-full"
      @click="handleSubmit"
    >
      <slot name="submitButtonText">
        {{ submitButtonText || $t('authentication.resetPasswordSubmit') }}
      </slot>
    </VbenButton>

    <div class="mt-4 text-center text-sm">
      {{ $t('authentication.rememberPassword') }}
      <span class="vben-link text-sm font-normal" @click="goToLogin()">
        {{ $t('authentication.goToLogin') }}
      </span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.email-display {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 16px;
  margin-bottom: 16px;
  font-size: 14px;
  color: #666;
  background: #f5f5f5;
  border-radius: 6px;
}
</style>
