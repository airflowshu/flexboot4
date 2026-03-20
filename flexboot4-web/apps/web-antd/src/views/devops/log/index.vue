<script lang="ts" setup>
import type { VbenFormProps } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SearchRequest } from '#/api/common';

import { ref } from 'vue';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getOperLogPage } from '#/api/system/operlog';
import { $t } from '#/locales';

import DetailDrawer from './detail-drawer.vue';

const [DetailModal, detailApi] = useVbenDrawer({
  connectedComponent: DetailDrawer,
});

// 业务类型翻译
const businessTypeMap: Record<number, string> = {
  0: '其它',
  1: '新增',
  2: '修改',
  3: '删除',
  4: '调用API',
  5: '导出',
  6: '导入',
  7: '查询',
  8: '登录',
  9: '登出',
};

const formOptions: VbenFormProps = {
  collapsed: false,
  schema: [
    {
      component: 'Input',
      fieldName: 'title',
      label: '操作模块',
      componentProps: {
        placeholder: '请输入操作模块',
      },
    },
    {
      component: 'Input',
      fieldName: 'operName',
      label: '操作人员',
      componentProps: {
        placeholder: '请输入操作人员',
      },
    },
    {
      component: 'Select',
      fieldName: 'businessType',
      label: '业务类型',
      componentProps: {
        placeholder: '请选择业务类型',
        options: Object.entries(businessTypeMap).map(([value, label]) => ({
          value: Number(value),
          label,
        })),
        allowClear: true,
      },
    },
    {
      component: 'Select',
      fieldName: 'status',
      label: '操作状态',
      componentProps: {
        placeholder: '请选择状态',
        options: [
          { value: 0, label: '正常' },
          { value: 1, label: '异常' },
        ],
        allowClear: true,
      },
    },
    {
      component: 'RangePicker',
      fieldName: 'operTime',
      label: '操作时间',
      componentProps: {
        format: 'YYYY-MM-DD HH:mm:ss',
        showTime: { format: 'HH:mm:ss' },
      },
    },
  ],
  showCollapseButton: true,
  submitOnChange: true,
  submitOnEnter: true,
  wrapperClass: 'grid-cols-1 md:grid-cols-2 lg:grid-cols-4',
};

const selectedRowId = ref<string>('');

const gridOptions: VxeGridProps = {
  columns: [
    { field: 'title', title: '操作模块', minWidth: 120 },
    {
      field: 'businessType',
      title: '业务类型',
      width: 100,
      slots: { default: 'businessType' },
    },
    {
      field: 'operatorType',
      title: '操作类型',
      width: 100,
      slots: { default: 'operatorType' },
    },
    { field: 'operName', title: '操作人员', width: 100 },
    {
      field: 'requestMethod',
      title: '请求方式',
      width: 100,
      slots: { default: 'requestMethod' },
    },
    { field: 'operUrl', title: '请求地址', minWidth: 180 },
    { field: 'operIp', title: '操作IP', width: 140 },
    { field: 'operLocation', title: '操作地点', width: 120 },
    {
      field: 'status',
      title: '状态',
      width: 80,
      slots: { default: 'status' },
    },
    { field: 'operTime', title: '操作时间', width: 180 },
    { field: 'costTime', title: '耗时(ms)', width: 90 },
  ],
  pagerConfig: {
    enabled: true,
  },
  exportConfig: {
    remote: true,
  },
  proxyConfig: {
    ajax: {
      query: async ({ page }, formValues) => {
        try {
          const params: SearchRequest = {
            pageNumber: page.currentPage,
            pageSize: page.pageSize,
            logic: 'AND',
            orders: [
              {
                column: 'operTime',
                asc: false,
              },
            ],
          };

          const items: SearchRequest['items'] = [];

          // 标题
          if (formValues.title) {
            items.push({
              field: 'title',
              op: 'like',
              val: formValues.title,
            });
          }

          // 操作人员
          if (formValues.operName) {
            items.push({
              field: 'operName',
              op: 'like',
              val: formValues.operName,
            });
          }

          // 业务类型
          if (
            formValues.businessType !== undefined &&
            formValues.businessType !== null
          ) {
            items.push({
              field: 'businessType',
              op: 'eq',
              val: formValues.businessType,
            });
          }

          // 状态
          if (formValues.status !== undefined && formValues.status !== null) {
            items.push({
              field: 'status',
              op: 'eq',
              val: formValues.status,
            });
          }

          // 操作时间范围
          if (
            formValues.operTime &&
            Array.isArray(formValues.operTime) &&
            formValues.operTime.length === 2
          ) {
            const formatTime = (val: any) => {
              if (dayjs.isDayjs(val)) {
                return val.format('YYYY-MM-DD HH:mm:ss');
              }
              if (typeof val === 'string') {
                return val;
              }
              return String(val);
            };
            items.push({
              logic: 'AND',
              children: [
                {
                  field: 'operTime',
                  op: 'gt',
                  val: formatTime(formValues.operTime[0]),
                },
                {
                  field: 'operTime',
                  op: 'lt',
                  val: formatTime(formValues.operTime[1]),
                },
              ],
            });
          }

          if (items.length > 0) {
            params.items = items;
          }

          const resp = await getOperLogPage(params);
          return {
            items: resp.records,
            total: resp.totalRow,
          };
        } catch (error) {
          console.error(error);
          return { items: [], total: 0 };
        }
      },
    },
  },
  toolbarConfig: {
    custom: true,
    export: true,
    refresh: true,
    resizable: true,
    zoom: true,
  },
  height: 'auto',
  rowConfig: {
    isHover: true,
    isCurrent: true,
  },
  rowClassName: ({ row }) => {
    return selectedRowId.value === row.id ? 'row-current-row' : '';
  },
  stripe: true,
};

function handleRowClick(row: any) {
  selectedRowId.value = row.id;
  detailApi.setData(row);
  detailApi.open();
}

const gridEvents = {
  cellClick: ({ row }: any) => {
    handleRowClick(row);
  },
};

const [Grid] = useVbenVxeGrid({
  formOptions,
  gridOptions,
  gridEvents,
} as any);
</script>

<template>
  <Page auto-content-height>
    <Grid :table-title="$t('devops.log.title')">
      <template #businessType="{ row }">
        <Tag color="blue">{{ row.businessTypeStr }}</Tag>
      </template>

      <template #operatorType="{ row }">
        <Tag color="cyan">{{ row.operatorTypeStr }}</Tag>
      </template>

      <template #status="{ row }">
        <Tag v-if="row.status === 0" color="success">正常</Tag>
        <Tag v-else color="error">异常</Tag>
      </template>

      <template #requestMethod="{ row }">
        <Tag v-if="row.requestMethod === 'GET'" color="green">GET</Tag>
        <Tag v-else-if="row.requestMethod === 'POST'" color="blue">POST</Tag>
        <Tag v-else-if="row.requestMethod === 'PUT'" color="orange">PUT</Tag>
        <Tag v-else-if="row.requestMethod === 'DELETE'" color="red">DELETE</Tag>
        <Tag v-else>{{ row.requestMethod }}</Tag>
      </template>
    </Grid>
    <DetailModal />
  </Page>
</template>

<style lang="scss" scoped>
:deep(
  .vxe-table--body-wrapper .vxe-table--body .vxe-body--row.row-current-row
) {
  background-color: #ecfccb !important;
}
</style>
