<script setup lang="ts">
import type { SearchRequest } from '#/api/common';
import type { SmsConfig } from '#/api/system/sms';

import { computed, nextTick, reactive, ref, watch } from 'vue';
import type { Component } from 'vue';

import { Page } from '@vben/common-ui';

import {
  ApiOutlined,
  CloudServerOutlined,
  MessageOutlined,
  NotificationOutlined,
  RocketOutlined,
  SafetyCertificateOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';

import { getSmsConfigList, updateSmsConfig } from '#/api/system/sms';

import CreateConfigModal from './components/create-config-modal.vue';
import { SMS_SUPPLIER_PRESETS } from './constants';

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

interface SupplierCardView {
  accent: string;
  accentSoft: string;
  config?: SmsConfig;
  configCount: number;
  description: string;
  emptyTip: string;
  fieldHints: string[];
  hasConfig: boolean;
  icon: Component;
  label: string;
  originalIndex: number;
  supplierType: string;
}

const configs = ref<SmsConfig[]>([]);
const loading = ref(false);
const refreshing = ref(false);
const showCreateModal = ref(false);
const selectedSupplierType = ref('');
const selectedConfig = ref<null | SmsConfig>(null);

const draftMap = reactive<Record<string, SmsConfigDraft>>({});
const savingMap = reactive<Record<string, boolean>>({});
const switchingMap = reactive<Record<string, boolean>>({});
const expandedSet = reactive<Set<string>>(new Set());

const supplierIconMap: Record<string, Component> = {
  aliyun: CloudServerOutlined,
  cloopen: NotificationOutlined,
  huawei: SafetyCertificateOutlined,
  mengwang: RocketOutlined,
  smsbao: MessageOutlined,
  tencent: ThunderboltOutlined,
  yilian: ApiOutlined,
};

const queryParams: SearchRequest = {
  pageNumber: 1,
  pageSize: 200,
  logic: 'AND',
  orders: [
    { column: 'weight', asc: false },
    { column: 'createTime', asc: false },
  ],
};

function getShortConfigId(value?: string) {
  if (!value) return '-';
  return value.length > 14 ? `${value.slice(0, 6)}...${value.slice(-4)}` : value;
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

function ensureDraft(id: string) {
  if (!draftMap[id]) {
    resetDraft(id);
  }
  return draftMap[id]!;
}

async function openCreateModal(supplierType: string, config?: SmsConfig) {
  selectedSupplierType.value = supplierType;
  selectedConfig.value = config ?? null;

  if (showCreateModal.value) {
    return;
  }

  await nextTick();
  showCreateModal.value = true;
}

function isExpanded(id: string) {
  return expandedSet.has(id);
}

function getSortScore(config?: SmsConfig) {
  if (!config) return 0;
  return (
    1000 +
    (config.status === 1 ? 200 : 0) +
    (config.isDefault === 1 ? 60 : 0) +
    (config.weight ?? 0)
  );
}

const supplierCards = computed<SupplierCardView[]>(() => {
  const configGroupMap = new Map<string, SmsConfig[]>();

  for (const config of configs.value) {
    const supplierType = config.supplierType?.toLowerCase();
    if (!supplierType) {
      continue;
    }
    const list = configGroupMap.get(supplierType) ?? [];
    list.push(config);
    configGroupMap.set(supplierType, list);
  }

  return SMS_SUPPLIER_PRESETS.map((preset, index) => {
    const matchedConfigs = configGroupMap.get(preset.supplierType) ?? [];
    const primaryConfig = matchedConfigs[0];
    return {
      ...preset,
      config: primaryConfig,
      configCount: matchedConfigs.length,
      hasConfig: Boolean(primaryConfig),
      icon: supplierIconMap[preset.supplierType] ?? CloudServerOutlined,
      originalIndex: index,
    };
  }).sort((left, right) => {
    const scoreDiff = getSortScore(right.config) - getSortScore(left.config);
    if (scoreDiff !== 0) {
      return scoreDiff;
    }
    return left.originalIndex - right.originalIndex;
  });
});

const configuredCount = computed(
  () => supplierCards.value.filter((item) => item.hasConfig).length,
);

const enabledCount = computed(
  () => configs.value.filter((item) => item.status === 1).length,
);

watch(showCreateModal, (open) => {
  if (!open) {
    selectedSupplierType.value = '';
    selectedConfig.value = null;
  }
});

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
    message.error('切换状态失败，请稍后重试');
  } finally {
    switchingMap[config.id] = false;
  }
}

async function handleSave(config: SmsConfig) {
  const draft = ensureDraft(config.id);
  const extParams = draft.extParams?.trim();

  if (extParams) {
    try {
      JSON.parse(extParams);
    } catch {
      message.error('扩展参数必须是合法的 JSON 格式');
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
    expandedSet.delete(config.id);
    message.success('配置保存成功');
  } catch (error) {
    console.error(error);
    message.error('保存失败，请稍后重试');
  } finally {
    savingMap[config.id] = false;
  }
}

function handleCancel(config: SmsConfig) {
  resetDraft(config.id);
  expandedSet.delete(config.id);
}

function handleCreateSuccess() {
  fetchConfigs(true);
}

fetchConfigs();
</script>

<template>
  <Page auto-content-height class="sms-page">
    <section class="sms-hero">
      <div class="hero-copy">
        <span class="hero-eyebrow">SMS Supplier Types</span>
        <h2 class="sms-title">短信供应商配置</h2>
        <p class="sms-subtitle">
          固定展示 7 家已集成的短信厂商。已接入和未接入卡片保持统一折叠态高度，详细参数通过展开后查看和编辑。
        </p>
        <div class="hero-stats">
          <div class="hero-stat">
            <span class="stat-label">已接入厂商</span>
            <strong class="stat-value">{{ configuredCount }}/{{ supplierCards.length }}</strong>
          </div>
          <div class="hero-stat">
            <span class="stat-label">启用配置</span>
            <strong class="stat-value">{{ enabledCount }}</strong>
          </div>
          <div class="hero-stat">
            <span class="stat-label">配置总数</span>
            <strong class="stat-value">{{ configs.length }}</strong>
          </div>
        </div>
      </div>
    </section>

    <div v-if="loading" class="skeleton-grid">
      <div v-for="item in 7" :key="item" class="skeleton-card">
        <a-skeleton active :paragraph="{ rows: 5 }" />
      </div>
    </div>

    <div v-else class="cards-grid">
      <section
        v-for="card in supplierCards"
        :key="card.supplierType"
        class="supplier-card"
        :class="{
          'is-configured': card.hasConfig,
          'is-expanded': card.config ? isExpanded(card.config.id) : false,
          'is-offline': card.config?.status === 0,
        }"
        :style="{
          '--card-accent': card.accent,
          '--card-accent-soft': card.accentSoft,
        }"
      >
        <div class="card-top">
          <div class="card-brand">
            <div class="brand-icon">
              <component :is="card.icon" />
            </div>
            <div class="brand-content">
              <div class="brand-row">
                <h3 class="brand-name">{{ card.label }}</h3>
                <span
                  :class="[
                    'status-chip',
                    card.hasConfig ? 'is-connected' : 'is-pending',
                  ]"
                >
                  {{ card.hasConfig ? '已接入' : '待接入' }}
                </span>
                <span
                  v-if="card.config?.isDefault === 1"
                  class="status-chip is-default"
                >
                  默认
                </span>
              </div>
              <p class="brand-description">{{ card.description }}</p>
            </div>
          </div>

          <a-tag class="type-tag" color="blue">
            {{ card.config?.supplierTypeStr || card.supplierType }}
          </a-tag>
        </div>

        <div class="summary-pills">
          <span class="summary-pill">配置 {{ card.configCount }}</span>
          <span v-if="card.config" class="summary-pill">权重 {{ card.config.weight ?? '-' }}</span>
          <span
            v-if="card.config"
            :class="[
              'status-chip',
              'summary-status-tag',
              card.config.status === 1 ? 'is-enabled' : 'is-disabled',
            ]"
          >
            {{ card.config.status === 1 ? '已启用' : '未启用' }}
          </span>
          <span v-else class="summary-pill summary-pill--ghost">待配置</span>
        </div>

        <div v-if="card.config" class="overview-shell">
          <div class="overview-main">
            <span class="overview-label">当前主配置</span>
            <strong class="overview-title">
              {{ card.config.configName || `${card.label}配置` }}
            </strong>
          </div>
          <div class="overview-grid">
            <div class="overview-item">
              <span class="overview-item-label">配置 ID</span>
              <strong class="overview-item-value mono">
                {{ getShortConfigId(card.config.configId) }}
              </strong>
            </div>
            <div class="overview-item">
              <span class="overview-item-label">AccessKeyId</span>
              <strong class="overview-item-value">
                {{ card.config.accessKeyId || '-' }}
              </strong>
            </div>
            <div class="overview-item">
              <span class="overview-item-label">短信签名</span>
              <strong class="overview-item-value">
                {{ card.config.signature || '-' }}
              </strong>
            </div>
            <div class="overview-item">
              <span class="overview-item-label">模板 ID</span>
              <strong class="overview-item-value">
                {{ card.config.templateId || '-' }}
              </strong>
            </div>
          </div>
          <p class="overview-tip">
            点击“去配置”打开抽屉维护该厂商配置。
          </p>
        </div>

        <div v-else class="empty-shell">
          <p class="empty-title">暂未读取到 {{ card.label }} 配置</p>
          <p class="empty-tip">{{ card.emptyTip }}</p>
          <div class="field-hints">
            <span v-for="field in card.fieldHints" :key="field" class="field-chip">
              {{ field }}
            </span>
          </div>
        </div>

        <div class="card-actions">
          <div class="action-side">
            <a-switch
              v-if="card.config"
              :checked="card.config.status === 1"
              :loading="switchingMap[card.config.id]"
              size="small"
              checked-children="开"
              un-checked-children="关"
              @change="(checked: boolean) => handleSwitchStatus(checked, card.config!)"
            />
            <span v-else class="empty-action-tip">
              点击“去配置”可直接进入该厂商专属创建流程。
            </span>
          </div>

          <div class="action-buttons">
            <a-button
              type="default"
              size="middle"
              class="config-action-btn"
              @click="openCreateModal(card.supplierType, card.config)"
            >
              去配置
            </a-button>
          </div>
        </div>

        <transition name="expand">
          <div v-if="card.config && isExpanded(card.config.id)" class="card-form">
            <a-divider class="form-divider" />

            <div v-if="card.configCount > 1" class="card-note">
              当前厂商存在 {{ card.configCount }} 条配置，卡片当前展示排序最靠前的一条。
            </div>

            <a-form layout="vertical" size="small">
              <a-row :gutter="16">
                <a-col :span="12">
                  <a-form-item label="配置名称">
                    <a-input
                      v-model:value="draftMap[card.config.id]!.configName"
                      placeholder="例如：阿里云主账号"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="权重">
                    <a-input-number
                      v-model:value="draftMap[card.config.id]!.weight"
                      :min="1"
                      :max="100"
                      class="w-full"
                    />
                  </a-form-item>
                </a-col>
              </a-row>

              <a-row :gutter="16">
                <a-col :span="12">
                  <a-form-item label="AccessKeyId">
                    <a-input v-model:value="draftMap[card.config.id]!.accessKeyId" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="AccessKeySecret">
                    <a-input-password
                      v-model:value="draftMap[card.config.id]!.accessKeySecret"
                    />
                  </a-form-item>
                </a-col>
              </a-row>

              <a-row :gutter="16">
                <a-col :span="12">
                  <a-form-item label="短信签名">
                    <a-input
                      v-model:value="draftMap[card.config.id]!.signature"
                      placeholder="例如：【云途商城】"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="模板 ID">
                    <a-input v-model:value="draftMap[card.config.id]!.templateId" />
                  </a-form-item>
                </a-col>
              </a-row>

              <a-row :gutter="16">
                <a-col :span="12">
                  <a-form-item label="SDK AppId">
                    <a-input
                      v-model:value="draftMap[card.config.id]!.sdkAppId"
                      placeholder="部分厂商需要，例如腾讯云"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="默认配置" class="default-config-item">
                    <a-radio-group
                      v-model:value="draftMap[card.config.id]!.isDefault"
                      class="default-config-group"
                      :options="[
                        { label: '是', value: 1 },
                        { label: '否', value: 0 },
                      ]"
                      option-type="button"
                      button-style="solid"
                      size="small"
                    />
                  </a-form-item>
                </a-col>
              </a-row>

              <a-form-item label="扩展参数 (JSON)">
                <a-textarea
                  v-model:value="draftMap[card.config.id]!.extParams"
                  :auto-size="{ minRows: 2, maxRows: 4 }"
                  placeholder='{"region":"cn-hangzhou"}'
                />
              </a-form-item>

              <a-form-item label="备注">
                <a-textarea
                  v-model:value="draftMap[card.config.id]!.remark"
                  :auto-size="{ minRows: 2, maxRows: 3 }"
                  placeholder="补充说明（可选）"
                />
              </a-form-item>

              <div class="form-actions">
                <a-button @click="handleCancel(card.config)">取消</a-button>
                <a-button
                  type="primary"
                  :loading="savingMap[card.config.id]"
                  @click="handleSave(card.config)"
                >
                  保存修改
                </a-button>
              </div>
            </a-form>
          </div>
        </transition>
      </section>
    </div>

    <CreateConfigModal
      v-model:open="showCreateModal"
      :default-supplier-type="selectedSupplierType"
      :initial-config="selectedConfig"
      @success="handleCreateSuccess"
    />
  </Page>
</template>

<style scoped lang="scss">
.sms-page {
  padding: 8px;
}

.sms-hero {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  justify-content: space-between;
  padding: 28px;
  margin-bottom: 20px;
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgb(22 119 255 / 14%), transparent 34%),
    radial-gradient(circle at left bottom, rgb(82 196 26 / 12%), transparent 30%),
    var(--bg-container);
  border: 1px solid rgb(22 119 255 / 10%);
  border-radius: 24px;
  box-shadow: 0 18px 40px rgb(15 23 42 / 8%);
}

.hero-copy {
  flex: 1;
  min-width: 0;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  margin-bottom: 14px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--primary-color);
  text-transform: uppercase;
  background: rgb(22 119 255 / 10%);
  border-radius: 999px;
}

.sms-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.15;
  color: var(--text-primary);
}

.sms-subtitle {
  max-width: 720px;
  margin: 12px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-secondary);
}

.hero-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.hero-stat {
  min-width: 148px;
  padding: 14px 16px;
  background: rgb(255 255 255 / 66%);
  border: 1px solid rgb(148 163 184 / 14%);
  border-radius: 16px;
  backdrop-filter: blur(8px);
}

.stat-label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}

.header-actions {
  display: flex;
  flex-shrink: 0;
  gap: 10px;
  align-items: center;
}

.skeleton-grid,
.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 18px;
}

.skeleton-card {
  padding: 18px;
  background: var(--bg-container);
  border: 1px solid var(--border-color);
  border-radius: 22px;
}

.supplier-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 360px;
  padding: 22px;
  overflow: hidden;
  background: var(--bg-container);
  border: 1px solid rgb(148 163 184 / 18%);
  border-radius: 24px;
  box-shadow: 0 18px 36px rgb(15 23 42 / 6%);
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    border-color 0.24s ease;
}

.supplier-card::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: '';
  background:
    radial-gradient(circle at top right, var(--card-accent-soft), transparent 34%),
    linear-gradient(180deg, rgb(255 255 255 / 3%), transparent);
}

.supplier-card:hover {
  transform: translateY(-4px);
  border-color: rgb(22 119 255 / 18%);
  box-shadow: 0 24px 48px rgb(15 23 42 / 10%);
}

.supplier-card.is-configured {
  border-color: rgb(22 119 255 / 16%);
}

.supplier-card.is-expanded {
  border-color: var(--card-accent);
  box-shadow: 0 24px 48px rgb(15 23 42 / 12%);
}

.supplier-card.is-offline {
  border-color: rgb(250 173 20 / 24%);
}

.card-top,
.summary-pills,
.overview-shell,
.empty-shell,
.card-actions,
.card-form {
  position: relative;
  z-index: 1;
}

.card-top {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.card-brand {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  min-width: 0;
}

.brand-icon {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  font-size: 20px;
  color: var(--card-accent);
  background: var(--card-accent-soft);
  border: 1px solid rgb(255 255 255 / 50%);
  border-radius: 16px;
}

.brand-content {
  min-width: 0;
}

.brand-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.brand-name {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--text-primary);
}

.brand-description {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
}

.type-tag {
  flex-shrink: 0;
}

.summary-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.summary-pill {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
  background: rgb(15 23 42 / 5%);
  border-radius: 999px;
}

.summary-pill--ghost {
  color: var(--text-secondary);
  background: rgb(148 163 184 / 12%);
}

.status-chip {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  border: 1px solid transparent;
  border-radius: 999px;
}

.status-chip.is-connected {
  color: #1677ff;
  background: rgb(22 119 255 / 10%);
  border-color: rgb(22 119 255 / 18%);
}

.status-chip.is-pending {
  color: rgb(89 89 89);
  background: #fafafa;
  border-color: #d9d9d9;
}

.status-chip.is-default {
  color: #ad6800;
  background: #fff7e6;
  border-color: #ffd591;
}

.summary-status-tag {
  height: 30px;
  padding-inline: 12px;
}

.summary-status-tag.is-enabled {
  color: #389e0d;
  background: #f6ffed;
  border-color: #b7eb8f;
}

.summary-status-tag.is-disabled {
  color: #595959;
  background: #fafafa;
  border-color: #d9d9d9;
}

.overview-shell,
.empty-shell {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 148px;
  padding: 16px;
  background:
    linear-gradient(135deg, rgb(15 23 42 / 3%), transparent),
    rgb(15 23 42 / 2%);
  border: 1px solid rgb(148 163 184 / 12%);
  border-radius: 18px;
}

.overview-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.overview-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.overview-title {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.4;
  color: var(--text-primary);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.overview-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  padding: 10px 12px;
  background: rgb(255 255 255 / 66%);
  border-radius: 14px;
}

.overview-item-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.overview-item-value {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  color: var(--text-primary);
  word-break: break-all;
}

.overview-tip {
  margin: auto 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.empty-shell {
  justify-content: center;
  border-style: dashed;
  border-color: rgb(148 163 184 / 24%);
}

.empty-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.empty-tip {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
}

.field-hints {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.field-chip {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  font-size: 12px;
  color: var(--text-secondary);
  background: rgb(255 255 255 / 66%);
  border-radius: 999px;
}

.card-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid rgb(148 163 184 / 14%);
}

.action-side {
  display: flex;
  align-items: center;
  min-width: 0;
}

.empty-action-tip {
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.action-buttons {
  display: flex;
  gap: 8px;
  align-items: center;
}

.config-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 84px;
  height: 32px;
  padding-inline: 12px;
  font-weight: 600;
  border-color: rgb(22 119 255 / 28%);
  border-radius: 10px;
  color: #1677ff;
  background: linear-gradient(180deg, #f7fbff 0%, #edf4ff 100%);
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 85%);
  transition: all 0.2s ease;
}

.config-action-btn:hover,
.config-action-btn:focus {
  border-color: #69b1ff;
  color: #0958d9;
  background: linear-gradient(180deg, #eef6ff 0%, #e3efff 100%);
}

.config-action-btn:active {
  border-color: #4096ff;
  background: #dbeafe;
}

.card-form {
  padding-top: 4px;
}

.form-divider {
  margin: 0 0 16px;
}

.card-note {
  padding: 12px 14px;
  margin-bottom: 14px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-secondary);
  background: rgb(22 119 255 / 6%);
  border: 1px dashed rgb(22 119 255 / 18%);
  border-radius: 14px;
}

.form-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 12px;
}

.mono {
  font-family:
    ui-monospace,
    SFMono-Regular,
    Menlo,
    Monaco,
    Consolas,
    'Liberation Mono',
    'Courier New',
    monospace;
  font-size: 12px;
}

.expand-enter-active,
.expand-leave-active {
  overflow: hidden;
  transition:
    opacity 0.2s ease,
    max-height 0.3s ease;
  max-height: 900px;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}

:deep(.ant-form-item) {
  margin-bottom: 12px;
}

:deep(.ant-form-item-label > label) {
  font-size: 12px;
  color: var(--text-secondary);
}

.default-config-item {
  margin-bottom: 4px;
}

:deep(.default-config-item .ant-form-item-control-input-content) {
  display: flex;
  align-items: center;
}

:deep(.default-config-group.ant-radio-group) {
  display: inline-flex;
  padding: 3px;
  background: rgb(15 23 42 / 5%);
  border-radius: 999px;
}

:deep(.default-config-group .ant-radio-button-wrapper) {
  height: 28px;
  padding-inline: 14px;
  font-size: 12px;
  line-height: 26px;
  color: var(--text-secondary);
  background: transparent;
  border: none;
  border-radius: 999px;
  box-shadow: none;
}

:deep(.default-config-group .ant-radio-button-wrapper::before) {
  display: none;
}

:deep(
  .default-config-group
    .ant-radio-button-wrapper-checked:not(.ant-radio-button-wrapper-disabled)
) {
  color: #1677ff;
  background: rgb(22 119 255 / 12%);
}

@media (max-width: 960px) {
  .sms-hero {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .sms-page {
    padding: 0;
  }

  .sms-hero {
    padding: 22px 18px;
    border-radius: 20px;
  }

  .cards-grid,
  .skeleton-grid {
    grid-template-columns: 1fr;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .card-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
