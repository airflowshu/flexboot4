<script lang="ts" setup>
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { SearchRequest } from '#/api/common';
import type { DeptRecord } from '#/api/system/dept';

import { Page, useVbenDrawer } from '@vben/common-ui';

import { Button, message, Modal, Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteDept, getDeptList } from '#/api/system/dept';

import DeptDrawer from './dept-drawer.vue';

defineOptions({ name: 'SystemDept' });
const [Drawer, drawerApi] = useVbenDrawer({
  connectedComponent: DeptDrawer,
});

const gridOptions: VxeGridProps<DeptRecord> = {
  columns: [
    { field: 'deptName', width: 380, title: '部门名称', treeNode: true },
    { field: 'orderNo', title: '排序', width: 80 },
    { field: 'remark', title: '备注', minWidth: 220 },
    {
      field: 'status',
      title: '状态',
      width: 80,
      slots: { default: 'status' },
    },
    {
      field: 'action',
      fixed: 'right',
      title: '操作',
      width: 150,
      slots: { default: 'action' },
    },
  ],
  pagerConfig: {
    enabled: false,
  },
  proxyConfig: {
    ajax: {
      query: async ({ sorts }) => {
        try {
          const params: SearchRequest = {
            pageNumber: 1,
            pageSize: 1000,
            orders: sorts.map((item) => ({
              asc: item.order === 'asc',
              column: item.field,
            })),
          };
          const data = await getDeptList(params);
          // 转换 parentId 为 '0' 的节点为根节点，防止 vxe-table 无法识别
          // 并且包装为 items 结构，以匹配 adapter/vxe-table.ts 中的全局 proxyConfig.response.list = 'items' 设置
          return {
            items: data.map((item) => {
              if (item.parentId === '0') {
                return { ...item, parentId: null };
              }
              return item;
            }),
          };
        } catch (error) {
          console.error(error);
          return [];
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
  treeConfig: {
    parentField: 'parentId',
    rowField: 'id',
    transform: true,
    expandAll: true,
  },
  stripe: true,
};

const [Grid, gridApi] = useVbenVxeGrid({ gridOptions });

const expandAll = () => {
  gridApi.grid?.setAllTreeExpand(true);
};

const collapseAll = () => {
  gridApi.grid?.setAllTreeExpand(false);
};

function handleAdd() {
  const records = gridApi.grid?.getFullData() || [];
  drawerApi.setData({ deptTree: records, isUpdate: false });
  drawerApi.open();
}

function handleEdit(row: DeptRecord) {
  const records = gridApi.grid?.getFullData() || [];
  drawerApi.setData({ deptTree: records, isUpdate: true, record: row });
  drawerApi.open();
}

function handleDelete(row: DeptRecord) {
  Modal.confirm({
    title: '确认删除',
    content: `是否确认删除部门 "${row.deptName}"？`,
    onOk: async () => {
      try {
        await deleteDept(row.id);
        message.success('删除成功');
        await gridApi.reload();
      } catch (error) {
        console.error(error);
      }
    },
  });
}

function handleSuccess() {
  gridApi.reload();
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="部门列表">
      <template #toolbar-tools>
        <Button class="mr-2" type="primary" @click="handleAdd">
          新增部门
        </Button>
        <Button class="mr-2" @click="expandAll"> 展开全部 </Button>
        <Button @click="collapseAll"> 折叠全部 </Button>
      </template>

      <template #status="{ row }">
        <Tag v-if="row.status === 1" color="success">启用</Tag>
        <Tag v-else color="error">禁用</Tag>
      </template>

      <template #action="{ row }">
        <Button size="small" type="link" @click="handleEdit(row)">编辑</Button>
        <Button danger size="small" type="link" @click="handleDelete(row)">
          删除
        </Button>
      </template>
    </Grid>
    <Drawer @success="handleSuccess" />
  </Page>
</template>

<!--
//根本原因在于全局的 vxe-table 适配器配置（ adapter/vxe-table.ts ）中定义了响应解析规则 response: { list: 'items' } 。
这意味着表格组件在接收到数据时，会尝试去读取返回对象的 items 属性（即 response.items ）。
之前的代码直接返回了数组 data ，组件尝试读取 data.items 得到的是 undefined ，所以表格无法渲染任何数据。

此外，为了防止树形结构因根节点 parentId 为 "0" 而无法被识别（通常组件默认根节点父ID为 null ），我也做了兼容处理。

1. 数据包装 ：将返回值从 data 修改为 { items: data } ，以匹配全局配置。
2. 根节点修正 ：遍历数据，将 parentId === '0' 的项修改为 parentId: null ，确保树形结构能正确构建。-->
