<script setup lang="ts">
import type { SearchRequest } from '#/api/common';
import type { SmsConfig } from '#/api/system/sms';

import { computed, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import { Empty, message } from 'ant-design-vue';

import { getSmsConfigList, updateSmsConfig } from '#/api/system/sms';

interface SmsConfigDraft {
  accessKeyId?: string;
  accessKeySecret?: string;
  configName?: string;
  extParams?: string;
  isDefault?: number;
  remark?: string;
  sdkAppId?: string;
  signature?: string;
  templateId?: string;
  weight?: number;
}

const configs = ref<SmsConfig[]>([]);
const loading = ref(false);
const refreshing = ref(false);
const expandedKeys = ref<string[]>([]);
const editingId = ref<null | string>(null);

const draftMap = reactive<Record<string, SmsConfigDraft>>({});
const savingMap = reactive<Record<string, boolean>>({});
const switchingMap = reactive<Record<string, boolean>>({});

const queryParams: SearchRequest = {
  pageNumber: 1,
  pageSize: 200,
  logic: 'AND',
  orders: [
    { column: 'weight', asc: false },
    { column: 'createTime', asc: false },
  ],
};

function maskSecret(value?: string) {
  if (!value) return '-';
  if (value.length <= 8) return `${value.slice(0, 2)}****${value.slice(-2)}`;
  return `${value.slice(0, 4)}****${value.slice(-4)}`;
}

function getDraft(id: string) {
  if (!draftMap[id]) {
    resetDraft(id);
  }
  return draftMap[id];
}

function resetDraft(id: string) {
  const config = configs.value.find((item) => item.id === id);
  if (!config) return;
  draftMap[id] = {
    accessKeyId: config.accessKeyId,
    accessKeySecret: config.accessKeySecret,
    configName: config.configName,
    extParams: config.extParams,
    isDefault: config.isDefault ?? 0,
    remark: config.remark,
    sdkAppId: config.sdkAppId,
    signature: config.signature,
    templateId: config.templateId,
    weight: config.weight,
  };
}

function isCardExpanded(id: string) {
  return expandedKeys.value.includes(id);
}

function toggleExpand(id: string) {
  if (isCardExpanded(id)) {
    expandedKeys.value = expandedKeys.value.filter((key) => key !== id);
    if (editingId.value === id) {
      editingId.value = null;
    }
    return;
  }
  expandedKeys.value.push(id);
}

function handleEdit(config: SmsConfig) {
  resetDraft(config.id);
  editingId.value = config.id;
  if (!isCardExpanded(config.id)) {
    expandedKeys.value.push(config.id);
  }
}

function handleCancel(config: SmsConfig) {
  resetDraft(config.id);
  editingId.value = null;
}

async function fetchConfigs(withRefresh = false) {
  if (withRefresh) {
    refreshing.value = true;
  } else {
    loading.value = true;
  }
  try {
    const list = await getSmsConfigList(queryParams);
    configs.value = list ?? [];
    for (const item of configs.value) {
      resetDraft(item.id);
    }
  } catch (error) {
    console.error(error);
    message.error('加载短信配置失败');
  } finally {
    loading.value = false;
    refreshing.value = false;
  }
}

async function handleSwitchStatus(checked: boolean, config: SmsConfig) {
  const nextStatus = checked ? 1 : 0;
  switchingMap[config.id] = true;
  try {
    await updateSmsConfig(config.id, { ...config, status: nextStatus });
    config.status = nextStatus;
    message.success(nextStatus === 1 ? '已启用该厂商配置' : '已禁用该厂商配置');
  } catch (error) {
    console.error(error);
    message.error('切换状态失败');
  } finally {
    switchingMap[config.id] = false;
  }
}

async function handleSave(config: SmsConfig) {
  const draft = getDraft(config.id);
  const extParams = draft.extParams?.trim();

  if (extParams) {
    try {
      JSON.parse(extParams);
    } catch {
      message.error('扩展参数必须是合法的 JSON');
      return;
    }
  }

  const payload: Partial<SmsConfig> = {
    ...config,
    ...draft,
    extParams,
  };

  savingMap[config.id] = true;
  try {
    await updateSmsConfig(config.id, payload);
    Object.assign(config, payload);
    resetDraft(config.id);
    editingId.value = null;
    message.success('配置保存成功');
  } catch (error) {
    console.error(error);
    message.error('保存失败，请稍后重试');
  } finally {
    savingMap[config.id] = false;
  }
}

const enabledCount = computed(
  () => configs.value.filter((item) => item.status === 1).length,
);

fetchConfigs();
</script>

<template>
  <Page auto-content-height class="sms-page">
    <div class="sms-header">
      <div>
        <h2 class="sms-title">短信厂商配置</h2>
        <p class="sms-subtitle">
          共 {{ configs.length }} 个厂商配置，当前启用 {{ enabledCount }} 个
        </p>
      </div>
      <a-button
        type="primary"
        :loading="refreshing"
        @click="fetchConfigs(true)"
      >
        刷新列表
      </a-button>
    </div>

    <a-skeleton v-if="loading" active :paragraph="{ rows: 6 }" />

    <Empty
      v-else-if="configs.length === 0"
      description="暂无短信厂商配置数据"
    />

    <a-row v-else :gutter="[16, 16]">
      <a-col
        v-for="config in configs"
        :key="config.id"
        :lg="8"
        :md="12"
        :span="24"
      >
        <a-card class="sms-card" :bordered="false">
          <template #title>
            <div class="card-title-row">
              <div class="title-main">
                <span class="title-text">{{
                  config.configName || '未命名配置'
                }}</span>
                <a-tag v-if="config.isDefault === 1" color="gold">默认</a-tag>
              </div>
              <a-tag color="blue">
                {{ config.supplierTypeStr || config.supplierType }}
              </a-tag>
            </div>
          </template>

          <template #extra>
            <a-space>
              <span class="status-label">{{
                config.status === 1 ? '启用中' : '已禁用'
              }}</span>
              <a-switch
                :checked="config.status === 1"
                :loading="switchingMap[config.id]"
                checked-children="开"
                un-checked-children="关"
                @change="
                  (checked: boolean) => handleSwitchStatus(checked, config)
                "
              />
            </a-space>
          </template>

          <div class="meta-grid">
            <div>
              <span class="meta-label">配置ID</span>
              <p class="meta-value mono">{{ config.configId }}</p>
            </div>
            <div>
              <span class="meta-label">权重</span>
              <p class="meta-value">{{ config.weight ?? '-' }}</p>
            </div>
            <div>
              <span class="meta-label">AccessKeyId</span>
              <p class="meta-value">{{ config.accessKeyId || '-' }}</p>
            </div>
            <div>
              <span class="meta-label">AccessKeySecret</span>
              <p class="meta-value mono">
                {{ maskSecret(config.accessKeySecret) }}
              </p>
            </div>
          </div>

          <a-divider class="my-3" />

          <a-collapse
            :active-key="isCardExpanded(config.id) ? ['detail'] : []"
            ghost
            @change="() => toggleExpand(config.id)"
          >
            <a-collapse-panel key="detail" header="详细参数配置">
              <a-form layout="vertical" size="small">
                <a-row :gutter="12">
                  <a-col :span="12">
                    <a-form-item label="配置名称">
                      <a-input
                        v-model:value="getDraft(config.id).configName"
                        :disabled="editingId !== config.id"
                        placeholder="例如：阿里云主账号"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="权重">
                      <a-input-number
                        v-model:value="getDraft(config.id).weight"
                        :disabled="editingId !== config.id"
                        :min="1"
                        class="w-full"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-row :gutter="12">
                  <a-col :span="12">
                    <a-form-item label="AccessKeyId">
                      <a-input
                        v-model:value="getDraft(config.id).accessKeyId"
                        :disabled="editingId !== config.id"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="AccessKeySecret">
                      <a-input-password
                        v-model:value="getDraft(config.id).accessKeySecret"
                        :disabled="editingId !== config.id"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-row :gutter="12">
                  <a-col :span="12">
                    <a-form-item label="短信签名">
                      <a-input
                        v-model:value="getDraft(config.id).signature"
                        :disabled="editingId !== config.id"
                        placeholder="例如：【云途商城】"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="模板ID">
                      <a-input
                        v-model:value="getDraft(config.id).templateId"
                        :disabled="editingId !== config.id"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-row :gutter="12">
                  <a-col :span="12">
                    <a-form-item label="SDK AppId">
                      <a-input
                        v-model:value="getDraft(config.id).sdkAppId"
                        :disabled="editingId !== config.id"
                      />
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="默认配置">
                      <a-radio-group
                        v-model:value="getDraft(config.id).isDefault"
                        :disabled="editingId !== config.id"
                      >
                        <a-radio :value="1">是</a-radio>
                        <a-radio :value="0">否</a-radio>
                      </a-radio-group>
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-form-item label="扩展参数 (JSON)">
                  <a-textarea
                    v-model:value="getDraft(config.id).extParams"
                    :auto-size="{ minRows: 3, maxRows: 6 }"
                    :disabled="editingId !== config.id"
                    placeholder='{"region":"cn-hangzhou"}'
                  />
                </a-form-item>

                <a-form-item label="备注">
                  <a-textarea
                    v-model:value="getDraft(config.id).remark"
                    :auto-size="{ minRows: 2, maxRows: 4 }"
                    :disabled="editingId !== config.id"
                    placeholder="补充说明（可选）"
                  />
                </a-form-item>

                <div class="form-actions">
                  <template v-if="editingId === config.id">
                    <a-button @click="handleCancel(config)">取消</a-button>
                    <a-button
                      type="primary"
                      :loading="savingMap[config.id]"
                      @click="handleSave(config)"
                    >
                      保存修改
                    </a-button>
                  </template>
                  <a-button
                    v-else
                    type="primary"
                    ghost
                    @click="handleEdit(config)"
                  >
                    编辑配置
                  </a-button>
                </div>
              </a-form>
            </a-collapse-panel>
          </a-collapse>
        </a-card>
      </a-col>
    </a-row>
  </Page>
</template>

<style scoped lang="scss">
.sms-page {
  padding: 4px;
}

.sms-header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.sms-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.sms-subtitle {
  margin: 4px 0 0;
  color: rgb(100 116 139);
}

.sms-card {
  border-radius: 14px;
  box-shadow: 0 8px 24px rgb(15 23 42 / 7%);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.sms-card:hover {
  box-shadow: 0 14px 30px rgb(15 23 42 / 10%);
  transform: translateY(-2px);
}

.card-title-row {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
}

.title-main {
  display: flex;
  gap: 8px;
  align-items: center;
}

.title-text {
  font-weight: 600;
}

.status-label {
  font-size: 12px;
  color: rgb(100 116 139);
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.meta-label {
  display: block;
  margin-bottom: 2px;
  font-size: 12px;
  color: rgb(100 116 139);
}

.meta-value {
  margin: 0;
  font-weight: 500;
  color: rgb(15 23 42);
}

.mono {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono',
    'Courier New', monospace;
}

.form-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 8px;
}

:deep(.ant-collapse > .ant-collapse-item > .ant-collapse-header) {
  padding-inline: 0;
}

:deep(.ant-collapse-content > .ant-collapse-content-box) {
  padding-inline: 0;
  padding-bottom: 0;
}
</style>
