<script setup lang="ts">
import type { EchartsUIType, ECOption } from '@vben/plugins/echarts';

import { onMounted, onUnmounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

import {
  Card,
  Col,
  Descriptions,
  DescriptionsItem,
  Progress,
  Row,
  Tag,
} from 'ant-design-vue';

import { requestClient } from '#/api/request';

// --- 接口返回数据类型 ---
interface CpuStats {
  usage: number;
  cores: number;
  physicalCores: number;
  frequency: string;
  userUsage: string;
}

interface MemStats {
  usage: number;
  total: string;
  used: string;
  available: string;
}

interface JvmStats {
  usage: number;
  heapUsed: string;
  heapTotal: string;
  nonHeapUsed: string;
  nonHeapTotal: string;
  version: string;
  vendor: string;
}

interface DiskStats {
  path: string;
  fsType: string;
  usage: number;
  total: string;
  used: string;
}

interface ThreadStats {
  active: number;
  peak: number;
  states: {
    blocked: number;
    running: number;
    timedWaiting: number;
    waiting: number;
  };
}

interface MonitorStatsResponse {
  cpu: CpuStats;
  memory: MemStats;
  jvm: JvmStats;
  disks: DiskStats[];
  threads: ThreadStats;
}

// --- 状态数据 ---
const cpuInfo = ref<CpuStats>({
  usage: 0,
  cores: 0,
  physicalCores: 0,
  frequency: '-',
  userUsage: '-',
});

const memInfo = ref<MemStats>({
  usage: 0,
  total: '-',
  used: '-',
  available: '-',
});

const jvmInfo = ref<JvmStats>({
  usage: 0,
  heapUsed: '-',
  heapTotal: '-',
  nonHeapUsed: '-',
  nonHeapTotal: '-',
  version: '-',
  vendor: '-',
});

const diskInfo = ref<DiskStats[]>([]);
const threadInfo = ref<ThreadStats>({
  active: 0,
  peak: 0,
  states: {
    running: 0,
    waiting: 0,
    blocked: 0,
    timedWaiting: 0,
  },
});

// --- 加载状态 ---
const loading = ref(true);

// --- Chart Refs ---
const cpuChartRef = ref<EchartsUIType>();
const memChartRef = ref<EchartsUIType>();
const jvmChartRef = ref<EchartsUIType>();

const { renderEcharts: renderCpu } = useEcharts(cpuChartRef);
const { renderEcharts: renderMem } = useEcharts(memChartRef);
const { renderEcharts: renderJvm } = useEcharts(jvmChartRef);

function getProgressColor(value: number): string {
  if (value >= 80) return '#ef4444';
  if (value >= 60) return '#f97316';
  return '#22c55e';
}

// --- 获取监控数据 ---
async function fetchMonitorStats() {
  try {
    const data = await requestClient.get<MonitorStatsResponse>(
      '/admin/monitor/stats',
    );

    // 更新数据
    cpuInfo.value = data.cpu;
    memInfo.value = data.memory;
    jvmInfo.value = data.jvm;
    diskInfo.value = data.disks;
    threadInfo.value = data.threads;

    // 更新图表，首次加载显示动画，后续刷新不显示动画
    const animation = isFirstLoad;
    await renderCpu(getGaugeOption('CPU', data.cpu.usage, animation));
    await renderMem(getGaugeOption('内存', data.memory.usage, animation));
    await renderJvm(getGaugeOption('JVM', data.jvm.usage, animation));

    // 首次加载后标记为false，后续刷新不再动画
    if (isFirstLoad) {
      isFirstLoad = false;
    }
  } catch (error) {
    console.error('获取监控数据失败:', error);
  } finally {
    loading.value = false;
  }
}

// --- 轮询定时器 ---
let pollTimer: null | ReturnType<typeof setInterval> = null;
let isFirstLoad = true; // 首次加载标志

// --- 颜色配置 ---
function getStatusColor(value: number): string {
  if (value >= 80) return '#ef4444'; // 红色 - 危险
  if (value >= 60) return '#f97316'; // 橙色 - 警告
  return '#22c55e'; // 绿色 - 正常
}

// --- ECharts 配置 ---
function getGaugeOption(
  title: string,
  value: number,
  animation = true,
): ECOption {
  const color = getStatusColor(value);
  return {
    tooltip: {
      formatter: '{a} <br/>{b} : {c}%',
    },
    animation,
    animationDuration: animation ? 1000 : 0,
    series: [
      {
        name: title,
        type: 'gauge',
        detail: {
          formatter: '{value}%',
          fontSize: 20,
          offsetCenter: [0, '70%'],
          valueAnimation: animation,
          color,
        },
        title: {
          offsetCenter: [0, '100%'],
          fontSize: 14,
          color: '#999',
        },
        data: [
          {
            value,
            name: title,
          },
        ],
        progress: {
          show: true,
          width: 12,
          itemStyle: {
            color,
          },
        },
        axisLine: {
          lineStyle: {
            width: 12,
            color: [
              [0.6, '#22c55e'],
              [0.8, '#f97316'],
              [1, '#ef4444'],
            ],
          },
        },
        axisTick: {
          show: false,
        },
        splitLine: {
          length: 5,
          lineStyle: {
            width: 1,
            color: '#999',
          },
        },
        axisLabel: {
          show: false,
          distance: 25,
          color: '#999',
          fontSize: 12,
        },
        pointer: {
          show: true,
          length: '60%',
          width: 4,
          itemStyle: {
            color: color,
          },
        },
        anchor: {
          show: true,
          size: 8,
          itemStyle: {
            color: color,
            borderWidth: 2,
            borderColor: '#fff',
          },
        },
      },
    ],
  } as ECOption;
}

// --- 生命周期 ---
onMounted(() => {
  // 首次加载数据
  fetchMonitorStats();

  // 每5秒轮询刷新
  pollTimer = setInterval(fetchMonitorStats, 5000);
});

// 组件卸载时清除定时器
onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
});
</script>

<template>
  <Page>
    <div class="p-4">
      <Row :gutter="[16, 16]">
        <!-- CPU 监控 -->
        <Col :span="24" :lg="8">
          <Card title="CPU监控" :bordered="false" class="h-full">
            <div class="flex flex-col items-center">
              <EchartsUI ref="cpuChartRef" height="220px" />
              <Descriptions
                v-if="!loading"
                :column="1"
                class="mt-2 w-full"
                size="small"
              >
                <DescriptionsItem label="核心数">
                  <Tag color="blue">{{ cpuInfo.cores }}个</Tag>
                </DescriptionsItem>
                <DescriptionsItem label="物理核心">
                  <Tag color="green">{{ cpuInfo.physicalCores }}个</Tag>
                </DescriptionsItem>
                <DescriptionsItem label="频率">
                  {{ cpuInfo.frequency }}
                </DescriptionsItem>
                <DescriptionsItem label="用户占用">
                  {{ cpuInfo.userUsage }}
                </DescriptionsItem>
              </Descriptions>
            </div>
          </Card>
        </Col>

        <!-- 内存监控 -->
        <Col :span="24" :lg="8">
          <Card title="内存监控" :bordered="false" class="h-full">
            <div class="flex flex-col items-center">
              <EchartsUI ref="memChartRef" height="220px" />
              <Descriptions
                v-if="!loading"
                :column="1"
                class="mt-2 w-full"
                size="small"
              >
                <DescriptionsItem label="总内存">
                  <Tag color="blue">{{ memInfo.total }}</Tag>
                </DescriptionsItem>
                <DescriptionsItem label="已使用">
                  {{ memInfo.used }}
                </DescriptionsItem>
                <DescriptionsItem label="可用">
                  {{ memInfo.available }}
                </DescriptionsItem>
              </Descriptions>
            </div>
          </Card>
        </Col>

        <!-- JVM 监控 -->
        <Col :span="24" :lg="8">
          <Card title="JVM监控" :bordered="false" class="h-full">
            <div class="flex flex-col items-center">
              <EchartsUI ref="jvmChartRef" height="220px" />
              <Descriptions
                v-if="!loading"
                :column="1"
                class="mt-2 w-full"
                size="small"
              >
                <DescriptionsItem label="堆内存">
                  <Tag color="orange">
                    {{ jvmInfo.heapUsed }} / {{ jvmInfo.heapTotal }}
                  </Tag>
                </DescriptionsItem>
                <DescriptionsItem label="非堆内存">
                  {{ jvmInfo.nonHeapUsed }} / {{ jvmInfo.nonHeapTotal }}
                </DescriptionsItem>
                <DescriptionsItem label="Java版本">
                  {{ jvmInfo.version }}
                </DescriptionsItem>
                <DescriptionsItem label="厂商">
                  {{ jvmInfo.vendor }}
                </DescriptionsItem>
              </Descriptions>
            </div>
          </Card>
        </Col>

        <!-- 磁盘监控 -->
        <Col :span="24" :lg="12">
          <Card title="磁盘监控" :bordered="false" class="h-full">
            <div
              v-if="loading"
              class="flex h-40 items-center justify-center text-gray-400"
            >
              加载中...
            </div>
            <div v-else class="flex flex-col gap-6">
              <div v-for="disk in diskInfo" :key="disk.path">
                <div class="mb-1 flex justify-between">
                  <div class="flex items-center gap-2">
                    <span
                      class="font-bold"
                      :class="[getStatusColor(disk.usage)]"
                      >●</span
                    >
                    <span class="font-medium">{{ disk.path }}</span>
                    <Tag color="blue">{{ disk.fsType }}</Tag>
                  </div>
                  <span :class="getStatusColor(disk.usage)">{{ disk.usage }}%</span>
                </div>
                <Progress
                  :percent="disk.usage"
                  :show-info="false"
                  :stroke-color="getProgressColor(disk.usage)"
                />
                <div class="mt-1 flex justify-between text-xs text-gray-500">
                  <span>总空间: {{ disk.total }}</span>
                  <span>已使用: {{ disk.used }}</span>
                </div>
              </div>
            </div>
          </Card>
        </Col>

        <!-- 线程监控 -->
        <Col :span="24" :lg="12">
          <Card title="线程监控" :bordered="false" class="h-full">
            <div
              v-if="loading"
              class="flex h-40 items-center justify-center text-gray-400"
            >
              加载中...
            </div>
            <div v-else class="flex flex-col">
              <div class="mb-8 flex justify-around text-center">
                <div>
                  <div class="mb-1 text-3xl font-bold text-blue-500">
                    {{ threadInfo.active }}
                  </div>
                  <div class="text-gray-500">活动线程</div>
                </div>
                <div>
                  <div class="mb-1 text-3xl font-bold text-green-500">
                    {{ threadInfo.peak }}
                  </div>
                  <div class="text-gray-500">峰值线程</div>
                </div>
              </div>

              <div class="px-4">
                <div class="mb-2 font-medium">线程状态分布</div>
                <div class="flex flex-col gap-3">
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-2">
                      <span class="font-bold text-green-500">●</span>
                      <span>运行线程</span>
                    </div>
                    <Progress
                      :percent="
                        (threadInfo.states.running / threadInfo.active) * 100
                      "
                      :show-info="false"
                      class="w-32"
                      stroke-color="#22c55e"
                      size="small"
                    />
                    <span class="w-8 text-right">{{
                      threadInfo.states.running
                    }}</span>
                  </div>
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-2">
                      <span class="font-bold text-orange-500">●</span>
                      <span>等待线程</span>
                    </div>
                    <Progress
                      :percent="
                        (threadInfo.states.waiting / threadInfo.active) * 100
                      "
                      :show-info="false"
                      class="w-32"
                      stroke-color="#f97316"
                      size="small"
                    />
                    <span class="w-8 text-right">{{
                      threadInfo.states.waiting
                    }}</span>
                  </div>
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-2">
                      <span class="font-bold text-red-500">●</span>
                      <span>阻塞线程</span>
                    </div>
                    <Progress
                      :percent="
                        (threadInfo.states.blocked / threadInfo.active) * 100
                      "
                      :show-info="false"
                      class="w-32"
                      stroke-color="#ef4444"
                      size="small"
                    />
                    <span class="w-8 text-right">{{
                      threadInfo.states.blocked
                    }}</span>
                  </div>
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-2">
                      <span class="font-bold text-blue-500">●</span>
                      <span>定时等待</span>
                    </div>
                    <Progress
                      :percent="
                        (threadInfo.states.timedWaiting / threadInfo.active) *
                        100
                      "
                      :show-info="false"
                      class="w-32"
                      stroke-color="#3b82f6"
                      size="small"
                    />
                    <span class="w-8 text-right">{{
                      threadInfo.states.timedWaiting
                    }}</span>
                  </div>
                </div>
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  </Page>
</template>

<style scoped lang="scss">
:deep(.ant-card-head-title) {
  font-weight: 600;
}
</style>
