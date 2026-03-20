<script lang="ts" setup>
import type { VxeGridProps } from '#/adapter/vxe-table';
import type { DictItem } from '#/api/system/dict';

import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { Button, message, Modal, Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { deleteDictItem, getDictItemList } from '#/api/system/dict';

import ItemModal from './dict-item-modal.vue';

const gridOptions: VxeGridProps<DictItem> = {
  columns: [
    { field: 'itemText', title: '名称', minWidth: 120 },
    { field: 'itemValue', title: '数据值', width: 100 },
    { field: 'itemCode', title: '编码', width: 100 },
    { field: 'orderNo', title: '排序', width: 80 },
    {
      field: 'status',
      title: '状态',
      width: 80,
      slots: { default: 'status' },
    },
    { field: 'remark', title: '备注', minWidth: 150 },
    {
      field: 'action',
      title: '操作',
      width: 140,
      fixed: 'right',
      slots: { default: 'action' },
    },
  ],
  data: [],
  pagerConfig: {
    enabled: false,
  },
  toolbarConfig: {
    custom: true,
    slots: { tools: 'toolbar-tools' },
  },
  height: 'auto',
};

const [Grid, gridApi] = useVbenVxeGrid({ gridOptions });
const [ItemDrawer, itemDrawerApi] = useVbenDrawer({
  connectedComponent: ItemModal,
});

const currentTypeId = ref<string>('');

async function fetchItems() {
  if (!currentTypeId.value) {
    return;
  }
  try {
    gridApi.setLoading(true);
    const items = await getDictItemList({
      pageNumber: 0,
      pageSize: 0,
      logic: 'AND',
      items: [{ field: 'typeId', op: 'eq', val: currentTypeId.value }],
      orders: [{ column: 'orderNo', asc: true }],
    });
    gridApi.setGridOptions({ data: items });
  } catch (error) {
    console.error('Failed to fetch dict items:', error);
  } finally {
    gridApi.setLoading(false);
  }
}

const [Drawer, drawerApi] = useVbenDrawer({
  showCancelButton: false,
  showConfirmButton: false,
  onOpenChange(isOpen) {
    if (isOpen) {
      const { typeId } = drawerApi.getData<any>() || {};
      if (typeId) {
        currentTypeId.value = typeId;
        fetchItems();
      }
    } else {
      currentTypeId.value = '';
      gridApi.setGridOptions({ data: [] });
    }
  },
});

function handleAdd() {
  itemDrawerApi.setData({ isUpdate: false, typeId: currentTypeId.value });
  itemDrawerApi.open();
}

function handleEdit(row: DictItem) {
  itemDrawerApi.setData({
    isUpdate: true,
    record: row,
    typeId: currentTypeId.value,
  });
  itemDrawerApi.open();
}

function handleDelete(row: DictItem) {
  Modal.confirm({
    title: '确认删除',
    content: `是否确认删除字典项 "${row.itemText}"？`,
    onOk: async () => {
      try {
        await deleteDictItem(row.id);
        message.success('删除成功');
        fetchItems();
      } catch (error) {
        console.error(error);
      }
    },
  });
}

function handleItemSuccess() {
  fetchItems();
}
</script>

<template>
  <Drawer title="字典项管理" class="w-[800px]">
    <div class="flex h-full flex-col">
      <Grid>
        <template #toolbar-tools>
          <Button type="primary" @click="handleAdd">新增字典项</Button>
        </template>

        <template #status="{ row }">
          <Tag v-if="row.status === 1" color="success">启用</Tag>
          <Tag v-else color="error">禁用</Tag>
        </template>

        <template #action="{ row }">
          <Button type="link" size="small" @click="handleEdit(row)">
            编辑
          </Button>
          <Button type="link" danger size="small" @click="handleDelete(row)">
            删除
          </Button>
        </template>
      </Grid>
      <ItemDrawer @success="handleItemSuccess" />
    </div>
  </Drawer>
</template>
