<script setup lang="ts">
import type { TablePaginationConfig } from 'ant-design-vue';

import type { SearchRequest } from '#/api/common';
import type { SysVersionLog } from '#/api/system/versionlog';

import { computed, h, onMounted, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  FireOutlined,
  InfoCircleOutlined,
  PlusOutlined,
  RocketOutlined,
  SearchOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue';
import {
  Button,
  Drawer,
  Input,
  message,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Timeline,
  TimelineItem,
  Typography,
} from 'ant-design-vue';

import {
  deleteVersionLog,
  getVersionLogList,
  getVersionLogPage,
} from '#/api/system/versionlog';

import LogDrawer from './log-drawer.vue';

const SelectOption = Select.Option;

const { Title, Paragraph, Text } = Typography;

// 抽屉控制
const isDrawerVisible = ref(false);
const [VersionLogDrawer, logDrawerApi] = useVbenDrawer({
  connectedComponent: LogDrawer,
});

// 查询参数
const searchText = ref('');
const filterType = ref<string | undefined>(undefined);

const tableData = ref<SysVersionLog[]>([]);
const timelineLogs = ref<SysVersionLog[]>([]);
const loading = ref(false);
const pagination = ref<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`,
});

// 表格列定义
const columns: any = [
  {
    title: '版本号',
    dataIndex: 'versionNo',
    key: 'versionNo',
    width: 120,
  },
  {
    title: '发布日期',
    dataIndex: 'releaseDate',
    key: 'releaseDate',
    width: 180,
  },
  {
    title: '类型',
    dataIndex: 'typeStr',
    key: 'type',
    width: 120,
  },
  {
    title: '标题',
    dataIndex: 'title',
    key: 'title',
    ellipsis: true,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80,
  },
  {
    title: '操作',
    key: 'action',
    width: 140,
    fixed: 'right',
  },
];

// 过滤后的数据（用于时间线展示）
const displayLogs = computed(() => {
  const getTimeValue = (value?: string) => {
    return value ? new Date(value).getTime() : 0;
  };
  return [...timelineLogs.value].toSorted(
    (a, b) => getTimeValue(b.releaseDate) - getTimeValue(a.releaseDate),
  );
});

// 获取类型对应的颜色
const getTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    major: 'red',
    minor: 'orange',
    patch: 'green',
  };
  return colors[type] || 'default';
};

// 获取类型对应的标签文本
const getTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    major: '重大更新',
    minor: '功能优化',
    patch: '问题修复',
  };
  return labels[type] || '其他';
};

// 获取类型对应的图标
const getTypeIcon = (type: string) => {
  const icons: Record<string, any> = {
    major: WarningOutlined,
    minor: InfoCircleOutlined,
    patch: CheckCircleOutlined,
  };
  return icons[type] || InfoCircleOutlined;
};

const formatTime = (time?: string) => {
  if (!time) return '-';
  return time.replace('T', ' ').slice(0, 19);
};

const normalizeList = (items?: string[]) => {
  if (!items) return [];
  return items.map((item) => item.trim()).filter(Boolean);
};

// 打开抽屉
const openDrawer = () => {
  isDrawerVisible.value = true;
};

// 打开新增弹窗
const openAddModal = () => {
  logDrawerApi.setData({}).open();
};

// 打开编辑弹窗
const openEditModal = (record: SysVersionLog) => {
  logDrawerApi.setData({ record }).open();
};

const handleSuccess = () => {
  fetchTableData();
  fetchTimelineLogs();
};

// 删除确认
const handleDelete = async (id: string) => {
  try {
    await deleteVersionLog(id);
    message.success('版本删除成功');
    await fetchTableData();
    await fetchTimelineLogs();
  } catch (error) {
    console.error(error);
  }
};

const fetchTableData = async () => {
  loading.value = true;
  try {
    const params: SearchRequest = {
      pageNumber: pagination.value.current || 1,
      pageSize: pagination.value.pageSize || 10,
      logic: 'AND',
      orders: [{ column: 'releaseDate', asc: false }],
    };

    if (searchText.value) {
      params.keyword = searchText.value;
      params.searchFields = ['versionNo', 'title'];
    }

    const items: SearchRequest['items'] = [];
    if (filterType.value) {
      items.push({ field: 'type', op: 'eq', val: filterType.value });
    }
    if (items.length > 0) {
      params.items = items;
    }

    const resp = await getVersionLogPage(params);
    tableData.value = resp.records || [];
    pagination.value = {
      ...pagination.value,
      total: resp.totalRow || 0,
      current: resp.pageNumber || pagination.value.current,
      pageSize: resp.pageSize || pagination.value.pageSize,
    };
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const fetchTimelineLogs = async () => {
  try {
    const params: SearchRequest = {
      pageNumber: 1,
      pageSize: 200,
      logic: 'AND',
      items: [
        {
          field: 'status',
          op: 'eq',
          val: 1,
        },
      ],
      orders: [{ column: 'releaseDate', asc: false }],
    };
    const resp = await getVersionLogList(params);
    timelineLogs.value = resp || [];
  } catch (error) {
    console.error(error);
  }
};

const handleTableChange = (page: TablePaginationConfig) => {
  pagination.value = {
    ...pagination.value,
    current: page.current || 1,
    pageSize: page.pageSize || 10,
  };
  fetchTableData();
};

const handleSearch = () => {
  pagination.value = {
    ...pagination.value,
    current: 1,
  };
  fetchTableData();
};

onMounted(() => {
  fetchTableData();
  fetchTimelineLogs();
});
</script>

<template>
  <div class="version-log-page">
    <!-- 背景特效 -->
    <div class="bg-effects">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="grid-overlay"></div>
    </div>

    <!-- 头部区域 -->
    <div class="header-section">
      <div class="header-content">
        <div class="header-top">
          <div class="title-wrapper">
            <RocketOutlined class="title-icon" />
            <Title :level="1" class="main-title">系统版本日志</Title>
          </div>
          <Button type="primary" size="large" @click="openDrawer">
            <template #icon>
              <EditOutlined />
            </template>
            管理版本日志
          </Button>
        </div>
        <Paragraph class="subtitle">
          记录系统每一次迭代与进化，持续为您提供更优质的服务体验
        </Paragraph>
      </div>
    </div>

    <!-- 时间线区域 -->
    <div class="timeline-section">
      <div class="timeline-container">
        <Timeline mode="left">
          <TimelineItem
            v-for="(log, index) in displayLogs"
            :key="log.id"
            :dot="
              index === 0
                ? h('div', { class: 'custom-dot new-dot' }, [
                    h(FireOutlined, { class: 'dot-icon' }),
                  ])
                : undefined
            "
          >
            <div class="version-card">
              <!-- 卡片头部 -->
              <div class="card-header">
                <div class="version-info">
                  <span class="version-number">{{ log.versionNo }}</span>
                  <Tag :color="getTypeColor(log.type || '')">
                    {{ log.typeStr || getTypeLabel(log.type || '') }}
                  </Tag>
                  <Tag v-if="index === 0" color="magenta" class="new-tag">
                    <FireOutlined /> 最新
                  </Tag>
                </div>
                <div class="version-date">
                  <ClockCircleOutlined />
                  {{ formatTime(log.releaseDate) }}
                </div>
              </div>

              <!-- 卡片内容 -->
              <div class="card-body">
                <h3 class="card-title">{{ log.title }}</h3>
                <p class="card-desc">{{ log.description }}</p>

                <!-- 新增功能 -->
                <div
                  v-if="normalizeList(log.features).length > 0"
                  class="features-section"
                >
                  <h4 class="section-title"><RocketOutlined /> 新增功能</h4>
                  <ul class="item-list">
                    <li
                      v-for="(feature, idx) in normalizeList(log.features)"
                      :key="idx"
                    >
                      <span class="bullet">▸</span>
                      {{ feature }}
                    </li>
                  </ul>
                </div>

                <!-- 问题修复 -->
                <div
                  v-if="normalizeList(log.fixes).length > 0"
                  class="fixes-section"
                >
                  <h4 class="section-title fix"><FireOutlined /> 问题修复</h4>
                  <ul class="item-list">
                    <li
                      v-for="(fix, idx) in normalizeList(log.fixes)"
                      :key="idx"
                    >
                      <span class="bullet fix-bullet">✓</span>
                      {{ fix }}
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </TimelineItem>
        </Timeline>
      </div>
    </div>

    <!-- 管理抽屉 -->
    <Drawer
      v-model:open="isDrawerVisible"
      title="版本日志管理"
      placement="right"
      :width="900"
      class="version-drawer"
    >
      <!-- 工具栏 -->
      <div class="drawer-toolbar">
        <Input
          v-model:value="searchText"
          placeholder="搜索版本号或标题..."
          class="search-input"
          allow-clear
          @press-enter="handleSearch"
          @change="handleSearch"
        >
          <template #prefix>
            <SearchOutlined />
          </template>
        </Input>
        <Select
          v-model:value="filterType"
          placeholder="筛选类型"
          class="type-select"
          allow-clear
          style="width: 140px"
          @change="handleSearch"
        >
          <SelectOption value="major">
            <Tag color="red">重大更新</Tag>
          </SelectOption>
          <SelectOption value="minor">
            <Tag color="orange">功能优化</Tag>
          </SelectOption>
          <SelectOption value="patch">
            <Tag color="green">问题修复</Tag>
          </SelectOption>
        </Select>
        <Button type="primary" @click="openAddModal">
          <template #icon>
            <PlusOutlined />
          </template>
          新增版本
        </Button>
      </div>

      <!-- 数据表格 -->
      <Table
        :columns="columns"
        :data-source="tableData"
        :pagination="pagination"
        :loading="loading"
        :scroll="{ x: 800 }"
        row-key="id"
        @change="handleTableChange"
      >
        <!-- 类型列 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <Tag :color="getTypeColor(record.type || '')" class="type-tag">
              <component :is="getTypeIcon(record.type || '')" />
              {{ record.typeStr || getTypeLabel(record.type || '') }}
            </Tag>
          </template>

          <template v-if="column.key === 'releaseDate'">
            <Text>{{ formatTime(record.releaseDate) }}</Text>
          </template>

          <!-- 标题列 -->
          <template v-if="column.key === 'title'">
            <Text>{{ record.title }}</Text>
          </template>

          <!-- 状态列 -->
          <template v-if="column.key === 'status'">
            <Tag v-if="record.status === 1" color="green">已发布</Tag>
            <Tag v-else color="default">草稿</Tag>
          </template>

          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <Space class="action-buttons">
              <Button
                type="link"
                size="small"
                @click="openEditModal(record as SysVersionLog)"
                class="action-btn edit"
              >
                <template #icon>
                  <EditOutlined />
                </template>
                编辑
              </Button>
              <Popconfirm
                title="确定要删除此版本吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record.id)"
              >
                <Button
                  type="link"
                  size="small"
                  danger
                  class="action-btn delete"
                >
                  <template #icon>
                    <DeleteOutlined />
                  </template>
                  删除
                </Button>
              </Popconfirm>
            </Space>
          </template>
        </template>
      </Table>

      <VersionLogDrawer @success="handleSuccess" />
    </Drawer>
  </div>
</template>

<style scoped lang="scss">
@keyframes float {
  0%,
  100% {
    transform: translate(0, 0);
  }

  50% {
    transform: translate(40px, 40px);
  }
}

@keyframes fire-pulse {
  0%,
  100% {
    box-shadow: 0 0 25px rgb(245 34 45 / 70%);
    transform: scale(1);
  }

  50% {
    box-shadow: 0 0 40px rgb(245 34 45 / 90%);
    transform: scale(1.1);
  }
}

@keyframes glow {
  0%,
  100% {
    box-shadow: 0 0 5px rgb(233 30 99 / 50%);
  }

  50% {
    box-shadow: 0 0 20px rgb(233 30 99 / 80%);
  }
}

// 响应式
@media (max-width: 768px) {
  .version-log-page {
    padding: 24px 16px;
  }

  .header-content {
    .header-top {
      flex-direction: column;
      align-items: stretch;
    }

    .title-wrapper {
      justify-content: center;
    }

    .subtitle {
      padding-left: 0;
      text-align: center;
    }
  }

  .main-title {
    font-size: 28px !important;
  }

  .title-icon {
    font-size: 36px !important;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .drawer-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input,
  .type-select {
    width: 100% !important;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }
}

.version-log-page {
  position: relative;
  min-height: 100vh;
  padding: 40px 24px;
  overflow: hidden;
}

// 背景特效
.bg-effects {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;

  .gradient-orb {
    position: absolute;
    border-radius: 50%;
    opacity: 0.3;
    filter: blur(100px);
  }

  .orb-1 {
    top: -150px;
    left: -150px;
    width: 500px;
    height: 500px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    animation: float 10s ease-in-out infinite;
  }

  .orb-2 {
    right: -100px;
    bottom: -100px;
    width: 400px;
    height: 400px;
    background: linear-gradient(135deg, #0ea5e9 0%, #06b6d4 100%);
    animation: float 12s ease-in-out infinite reverse;
  }

  .grid-overlay {
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgb(255 255 255 / 2%) 1px, transparent 1px),
      linear-gradient(90deg, rgb(255 255 255 / 2%) 1px, transparent 1px);
    background-size: 60px 60px;
    mask-image: radial-gradient(ellipse at center, black 0%, transparent 70%);
  }
}

// 头部区域
.header-section {
  position: relative;
  z-index: 1;
  max-width: 1000px;
  padding: 0 16px;
  margin: 0 auto 48px;
}

.header-content {
  @keyframes pulse-glow {
    0%,
    100% {
      filter: drop-shadow(0 0 20px rgb(14 165 233 / 60%));
      transform: scale(1);
    }

    50% {
      filter: drop-shadow(0 0 40px rgb(14 165 233 / 90%));
      transform: scale(1.05);
    }
  }

  .header-top {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 16px;
  }

  .title-wrapper {
    display: flex;
    gap: 16px;
    align-items: center;
  }

  .title-icon {
    font-size: 48px;
    color: #0ea5e9;
    filter: drop-shadow(0 0 20px rgb(14 165 233 / 60%));
    animation: pulse-glow 3s ease-in-out infinite;
  }

  .main-title {
    margin: 0 !important;
    font-size: 40px !important;
    font-weight: 700;
    letter-spacing: 2px;
    background: linear-gradient(135deg, #fff 0%, #94a3b8 100%);
    background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .subtitle {
    padding-left: 64px;
    margin: 0 !important;
    font-size: 16px;
    color: rgb(255 255 255 / 60%);
  }
}

// 时间线区域
.timeline-section {
  position: relative;
  z-index: 1;
  max-width: 1000px;
  padding: 0 16px;
  margin: 0 auto;
}

.timeline-container {
  :deep(.ant-timeline-item-tail) {
    background: linear-gradient(
      180deg,
      rgb(14 165 233 / 60%) 0%,
      rgb(102 126 234 / 60%) 100%
    );
    border-left: 2px solid;
    border-image: linear-gradient(
        180deg,
        rgb(14 165 233 / 100%) 0%,
        rgb(102 126 234 / 100%) 100%
      )
      1;
  }

  :deep(.ant-timeline-item-head) {
    width: 18px;
    height: 18px;
    background: linear-gradient(135deg, #0ea5e9 0%, #06b6d4 100%);
    border: 3px solid rgb(255 255 255 / 30%);
    box-shadow: 0 0 20px rgb(14 165 233 / 80%);
  }
}

.custom-dot {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #f5222d 0%, #fa8c16 100%);
  border-radius: 50%;
  box-shadow: 0 0 25px rgb(245 34 45 / 70%);
  animation: fire-pulse 2s ease-in-out infinite;

  .dot-icon {
    font-size: 14px;
    color: #fff;
  }
}

.version-card {
  overflow: hidden;
  background: rgb(255 255 255 / 4%);
  border: 1px solid rgb(255 255 255 / 10%);
  border-radius: 20px;
  backdrop-filter: blur(30px);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    border-color: rgb(14 165 233 / 50%);
    box-shadow:
      0 25px 50px rgb(0 0 0 / 40%),
      0 0 40px rgb(14 165 233 / 15%);
    transform: translateY(-6px);
  }
}

.card-header {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid rgb(255 255 255 / 8%);
}

.version-info {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.version-number {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 26px;
  font-weight: 700;
  background: linear-gradient(135deg, #0ea5e9 0%, #06b6d4 100%);
  background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.new-tag {
  animation: glow 2s ease-in-out infinite;

  :deep(.anticon) {
    margin-right: 4px;
  }
}

.version-date {
  display: flex;
  gap: 6px;
  align-items: center;
  font-size: 14px;
  color: rgb(255 255 255 / 50%);

  :deep(.anticon) {
    font-size: 14px;
  }
}

.card-body {
  padding: 24px;
}

.card-title {
  margin: 0 0 10px;
  font-size: 22px;
  font-weight: 600;
  color: #fff;
}

.card-desc {
  margin: 0 0 24px;
  font-size: 15px;
  line-height: 1.6;
  color: rgb(255 255 255 / 60%);
}

.section-title {
  display: flex;
  gap: 8px;
  align-items: center;
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: #0ea5e9;

  &.fix {
    color: #52c41a;
  }

  :deep(.anticon) {
    font-size: 16px;
  }
}

.features-section,
.fixes-section {
  margin-top: 20px;
}

.item-list {
  padding: 0;
  margin: 0;
  list-style: none;
}

.item-list li {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 8px 0;
  font-size: 14px;
  line-height: 1.6;
  color: rgb(255 255 255 / 80%);
}

.bullet {
  padding-top: 3px;
  font-size: 12px;
  font-weight: bold;
  color: #0ea5e9;
}

.fix-bullet {
  color: #52c41a;
}

// 抽屉工具栏
.drawer-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  padding-bottom: 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid rgb(255 255 255 / 10%);
}

.search-input {
  width: 280px;
}

.type-select {
  flex-shrink: 0;
}

.type-tag {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}

.normal-text {
  color: rgb(255 255 255 / 30%);
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn {
  height: auto;
  padding: 0;
  font-size: 13px;

  &.edit {
    color: #0ea5e9;

    &:hover {
      color: #06b6d4;
    }
  }

  &.delete {
    &:hover {
      color: #ff4d4f;
    }
  }
}
</style>

<style lang="scss">
// ============================================
// 白色主题适配 (全局样式覆盖)
// ============================================
html:not(.dark),
html[data-theme='light'],
.light-theme,
body.light {
  .version-log-page {
    background-color: #f0f2f5;

    .bg-effects {
      opacity: 0;
    }

    .header-content {
      .header-top .ant-btn-primary {
        background: #1677ff;
        border-color: #1677ff;
        box-shadow: 0 2px 0 rgb(5 145 255 / 10%);

        &:hover {
          background: #4096ff;
          border-color: #4096ff;
        }
      }
    }

    .title-icon {
      color: #1677ff;
      filter: none;
      animation: none;
    }

    .main-title {
      color: #1f1f1f;
      text-shadow: none;
      background: none;
      -webkit-text-fill-color: #1f1f1f;
    }

    .subtitle {
      font-weight: 500;
      color: #4b5563;
    }

    .timeline-container {
      .ant-timeline-item-tail {
        background: #e5e7eb;
        border-left: 2px solid #e5e7eb;
        border-image: none;
      }

      .ant-timeline-item-head {
        background: #fff;
        border: 3px solid #1677ff;
        box-shadow: 0 0 0 4px rgb(22 119 255 / 20%);
      }
    }

    .custom-dot {
      background: #fff;
      border: 2px solid #ff4d4f;
      box-shadow: 0 0 0 4px rgb(255 77 79 / 20%);
      animation: none;

      .dot-icon {
        color: #ff4d4f;
      }
    }

    .version-card {
      background: #fff;
      border: 1px solid #e5e7eb;
      box-shadow: 0 1px 2px 0 rgb(0 0 0 / 5%);

      &:hover {
        border-color: #1677ff;
        box-shadow:
          0 10px 15px -3px rgb(0 0 0 / 10%),
          0 4px 6px -2px rgb(0 0 0 / 5%);
        transform: translateY(-2px);
      }
    }

    .card-header {
      background: #fafafa;
      border-bottom: 1px solid #f3f4f6;
    }

    .version-number {
      color: #1677ff;
      background: none;
      -webkit-text-fill-color: #1677ff;
    }

    .version-date {
      color: #6b7280;

      .anticon {
        color: #9ca3af;
      }
    }

    .card-title {
      font-weight: 700;
      color: #111827;
    }

    .card-desc {
      color: #4b5563;
    }

    .section-title {
      color: #1677ff;

      &.fix {
        color: #52c41a;
      }
    }

    .item-list li {
      color: #374151;
    }

    .bullet {
      color: #1677ff;
    }

    .fix-bullet {
      color: #52c41a;
    }

    // 抽屉内表格
    .drawer-toolbar {
      border-bottom-color: #f0f0f0;
    }

    .search-input {
      .ant-input {
        color: #333;
        background: #fff;

        &::placeholder {
          color: #999;
        }
      }

      .ant-input-prefix {
        color: #999;
      }
    }

    .type-select {
      .ant-select-selector {
        background: #fff !important;
        border-color: #d9d9d9 !important;
      }
    }

    .version-table {
      .ant-table-thead > tr > th {
        color: #333;
        background: #fafafa !important;
        border-bottom-color: #f0f0f0;
      }

      .ant-table-tbody > tr > td {
        color: #333;
        border-bottom-color: #f5f5f5;
      }

      .ant-table-tbody > tr:hover > td {
        background: #f0f7ff !important;
      }
    }

    .title-text {
      color: #1a1a1a;
    }

    .normal-text {
      color: #999;
    }

    .action-btn {
      &.edit {
        color: #1677ff;

        &:hover {
          color: #4096ff;
        }
      }
    }
  }
}
</style>
