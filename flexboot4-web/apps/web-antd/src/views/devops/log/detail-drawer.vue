<script lang="ts" setup>
import { computed } from 'vue';

import { JsonViewer, useVbenDrawer } from '@vben/common-ui';

import { Card, Empty } from 'ant-design-vue';

interface OperLogDetail {
  operParam?: Record<string, any>;
  jsonResult?: Record<string, any>;
  [key: string]: any;
}

const detailData = computed(() => {
  return drawerApi.getData<OperLogDetail>() || {};
});

// 获取实际的请求参数，如果最外层有args则取args内部数据
const operParamValue = computed(() => {
  const { operParam } = detailData.value;
  if (!operParam) return null;
  // 如果最外层有 args 字段，则只渲染 args 内部
  if ('args' in operParam && operParam.args) {
    return operParam.args;
  }
  return operParam;
});

// 获取实际的返回结果，如果最外层有args则取args内部数据
const jsonResultValue = computed(() => {
  const { jsonResult } = detailData.value;
  if (!jsonResult) return null;
  // 如果最外层有 args 字段，则只渲染 args 内部
  if ('args' in jsonResult && jsonResult.args) {
    return jsonResult.args;
  }
  return jsonResult;
});

// 错误信息
const errorMsgValue = computed(() => {
  const { errorMsg } = detailData.value;
  if (errorMsg && errorMsg.trim()) {
    return errorMsg;
  }
  return null;
});

// 扩展参数
const extParamsValue = computed(() => {
  const { extParams } = detailData.value;
  if (extParams && Object.keys(extParams).length > 0) {
    return extParams;
  }
  return null;
});

// 终端信息
interface TerminalInfo {
  osName?: string;
  agentName?: string;
  osVersion?: string;
  deviceName?: string;
  deviceBrand?: string;
  agentVersion?: string;
}

const terminalValue = computed(() => {
  const { terminal } = detailData.value;
  if (terminal && typeof terminal === 'object') {
    return terminal as TerminalInfo;
  }
  return null;
});

// 判断设备类型
const terminalType = computed(() => {
  const { deviceBrand, deviceName, osName } = terminalValue.value || {};
  const lowerBrand = (deviceBrand || '').toLowerCase();
  const lowerName = (deviceName || '').toLowerCase();
  const lowerOs = (osName || '').toLowerCase();

  // 常见手机品牌
  const mobileBrands = [
    'apple',
    'samsung',
    'xiaomi',
    'huawei',
    'oppo',
    'vivo',
    'oneplus',
    'realme',
    'iqoo',
    'google',
    'nokia',
    'motorola',
    'sony',
    'lg',
  ];

  // 常见平板关键词
  const padKeywords = ['ipad', 'tablet', 'pad', 'tab'];

  // 判断是否为手机
  if (
    mobileBrands.some((brand) => lowerBrand.includes(brand)) ||
    (lowerOs.includes('android') &&
      !padKeywords.some((k) => lowerName.includes(k)))
  ) {
    return 'mobile';
  }

  // 判断是否为平板
  if (
    padKeywords.some((k) => lowerName.includes(k)) ||
    lowerName.includes('tab') ||
    lowerBrand.includes('ipad') ||
    lowerBrand.includes('tablet')
  ) {
    return 'pad';
  }

  return 'pc';
});

// PC图标SVG
const pcIconSvg = `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2" ry="2" /><line x1="8" y1="21" x2="16" y2="21" /><line x1="12" y1="17" x2="12" y2="21" /></svg>`;

// 平板图标SVG
const padIconSvg = `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="2" width="16" height="20" rx="2" ry="2" /><line x1="12" y1="18" x2="12.01" y2="18" /></svg>`;

// 手机图标SVG
const mobileIconSvg = `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="5" y="2" width="14" height="20" rx="2" ry="2" /><line x1="12" y1="18" x2="12.01" y2="18" /></svg>`;

// 根据类型获取图标
const terminalIconSvg = computed(() => {
  switch (terminalType.value) {
    case 'mobile': {
      return mobileIconSvg;
    }
    case 'pad': {
      return padIconSvg;
    }
    default: {
      return pcIconSvg;
    }
  }
});

const [Drawer, drawerApi] = useVbenDrawer({
  connectedComponent: undefined,
  destroyOnClose: true,
  placement: 'right',
  showCancelButton: false,
  showConfirmButton: false,
  width: '600px',
} as any);
</script>

<template>
  <Drawer title="操作详情" class="w-[600px]">
    <div class="flex flex-col gap-4">
      <!-- 请求参数 -->
      <Card title="请求参数" size="small">
        <JsonViewer
          v-if="operParamValue && Object.keys(operParamValue).length > 0"
          :value="operParamValue"
          copyable
          :expand-depth="2"
          boxed
        />
        <Empty v-else description="无请求参数" />
      </Card>

      <!-- 返回结果 -->
      <Card
        v-if="jsonResultValue && Object.keys(jsonResultValue).length > 0"
        title="返回结果"
        size="small"
      >
        <JsonViewer
          :value="jsonResultValue"
          copyable
          :expand-depth="2"
          boxed
        />
      </Card>

      <!-- 错误信息 -->
      <Card v-if="errorMsgValue" title="错误信息" size="small">
        <div class="error-message text-red-600">{{ errorMsgValue }}</div>
      </Card>

      <!-- 扩展参数 -->
      <Card v-if="extParamsValue" title="扩展参数" size="small">
        <JsonViewer :value="extParamsValue" copyable :expand-depth="2" boxed />
      </Card>

      <!-- 终端信息 -->
      <Card v-if="terminalValue" title="终端信息" size="small">
        <div class="terminal-card">
          <div class="terminal-header">
            <div
              class="terminal-icon"
              :class="`icon-${terminalType}`"
              v-html="terminalIconSvg"
            ></div>
            <div class="terminal-title">
              <span class="device-name">
                {{ terminalValue.agentName }}
                {{ terminalValue.agentVersion }}
              </span>
              <span class="device-brand">
                {{ terminalValue.deviceBrand }}
                {{ terminalValue.deviceName }}
              </span>
            </div>
          </div>
          <div class="terminal-info">
            <div class="info-item">
              <span class="label">操作系统</span>
              <span class="value"
                >{{ terminalValue.osName }} {{ terminalValue.osVersion }}</span
              >
            </div>
          </div>
        </div>
      </Card>
    </div>
  </Drawer>
</template>

<style lang="scss" scoped>
.terminal-card {
  .terminal-header {
    display: flex;
    gap: 12px;
    align-items: center;
    padding-bottom: 12px;
    margin-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;

    .terminal-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 48px;
      height: 48px;
      color: white;
      border-radius: 12px;

      &.icon-mobile {
        background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
      }

      &.icon-pad {
        background: linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%);
      }

      &.icon-pc {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }
    }

    .terminal-title {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .device-name {
        font-size: 16px;
        font-weight: 600;
        color: #1f2937;
      }

      .device-brand {
        font-size: 13px;
        color: #6b7280;
      }
    }
  }

  .terminal-info {
    .info-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 0;

      .label {
        font-size: 14px;
        color: #6b7280;
      }

      .value {
        font-size: 14px;
        font-weight: 500;
        color: #1f2937;
      }
    }
  }
}

.error-message {
  max-height: 300px;
  overflow-y: auto;
  word-break: break-all;
  overflow-wrap: break-word;
  white-space: pre-wrap;
}
</style>
