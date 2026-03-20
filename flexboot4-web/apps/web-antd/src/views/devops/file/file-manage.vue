<script setup lang="ts">
import type { VxeGridListeners, VxeGridProps } from '#/adapter/vxe-table';
import type { SearchRequest } from '#/api/common';
import type { FileAccessDescriptor, FileObject } from '#/api/core/file';

import { ref } from 'vue';

import { AccessControl } from '@vben/access';
import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Input, message, Modal, Tooltip } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  deleteFiles,
  getFileAccessUrl,
  getFilePage,
  uploadSingleFileApi,
} from '#/api/core/file';

interface FileCategory {
  id: string;
  name: string;
  icon: string;
  exts?: string[];
  mimes?: string[];
}

const categories: FileCategory[] = [
  { id: 'all', name: '全部文件', icon: 'carbon:document' },
  {
    id: 'image',
    name: '图片',
    icon: 'carbon:image',
    exts: ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp'],
  },
  {
    id: 'document',
    name: '文档',
    icon: 'carbon:document-pdf',
    exts: [
      'pdf',
      'doc',
      'docx',
      'xls',
      'xlsx',
      'ppt',
      'pptx',
      'txt',
      'md',
      'csv',
    ],
  },
  {
    id: 'video',
    name: '视频',
    icon: 'carbon:video',
    exts: ['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv'],
  },
  {
    id: 'audio',
    name: '音频',
    icon: 'carbon:music',
    exts: ['mp3', 'wav', 'ogg', 'flac', 'aac'],
  },
  {
    id: 'archive',
    name: '压缩包',
    icon: 'carbon:zip',
    exts: ['zip', 'rar', '7z', 'tar', 'gz', 'bz2'],
  },
  { id: 'other', name: '其他', icon: 'carbon:help' },
];

const activeCategory = ref('all');
const searchKeyword = ref('');
const fileInputRef = ref<HTMLInputElement | null>(null);

const gridOptions: VxeGridProps = {
  columns: [
    { type: 'checkbox', width: 50 },
    {
      field: 'fileName',
      title: '文件名',
      minWidth: 200,
      slots: { default: 'fileName' },
    },
    { field: 'fileExt', title: '类型', width: 80 },
    {
      field: 'fileSize',
      title: '大小',
      width: 100,
      formatter: ({ cellValue }) => {
        if (!cellValue) return '-';
        const units = ['B', 'KB', 'MB', 'GB', 'TB'];
        let size = cellValue;
        let unitIndex = 0;
        while (size >= 1024 && unitIndex < units.length - 1) {
          size /= 1024;
          unitIndex++;
        }
        return `${size.toFixed(2)} ${units[unitIndex]}`;
      },
    },
    {
      field: 'lastModifyTime',
      title: '上传时间',
      width: 180,
      formatter: ({ cellValue }) =>
        cellValue ? dayjs(cellValue).format('YYYY-MM-DD HH:mm:ss') : '-',
    },
    {
      title: '操作',
      width: 150,
      fixed: 'right',
      slots: { default: 'action' },
    },
  ],
  pagerConfig: {
    enabled: true,
  },
  proxyConfig: {
    ajax: {
      query: async ({ page }) => {
        const params: SearchRequest = {
          pageNumber: page.currentPage,
          pageSize: page.pageSize,
          logic: 'AND',
          orders: [{ column: 'createTime', asc: false }],
          items: [],
        };

        if (searchKeyword.value) {
          params.items?.push({
            field: 'fileName',
            op: 'like',
            val: searchKeyword.value,
          });
        }

        if (activeCategory.value !== 'all') {
          const cat = categories.find((c) => c.id === activeCategory.value);
          if (cat?.exts) {
            params.items?.push({
              field: 'fileExt',
              op: 'in',
              val: cat.exts.join(','),
            });
          } else if (activeCategory.value === 'other') {
            // 处理"其他"类型的逻辑：排除掉已知的所有后缀
            const allKnownExts = categories.flatMap((c) => c.exts || []);
            params.items?.push({
              field: 'fileExt',
              op: 'notIn',
              val: allKnownExts.join(','),
            });
          }
        }

        const res = await getFilePage(params);
        return {
          items: res.records,
          total: res.totalRow,
        };
      },
    },
  },
  rowConfig: {
    isHover: true,
  },
};

// KKFileView 预览地址前缀
const PREVIEW_PREFIX = 'http://192.168.11.104:8012/onlinePreview';

// Base64 编码（处理中文和特殊字符）
function base64Encode(str: string): string {
  try {
    return btoa(
      encodeURIComponent(str).replaceAll(/%([0-9A-F]{2})/g, (_, p1) => {
        return String.fromCodePoint(Number.parseInt(p1, 16));
      }),
    );
  } catch {
    return btoa(str);
  }
}

// 处理行点击预览文件
async function handleRowClick(row: FileObject) {
  if (!row.id) return;

  try {
    const data: FileAccessDescriptor = await getFileAccessUrl(row.id);

    if (data.url) {
      // KKFileView 需要 Base64 编码的 URL
      const previewUrl = `${PREVIEW_PREFIX}?url=${base64Encode(data.url)}`;
      window.open(previewUrl, '_blank');
    } else {
      message.error('无法获取文件访问地址');
    }
  } catch (error) {
    console.error('获取预览地址失败:', error);
    message.error('获取预览地址失败');
  }
}

const gridEvents: VxeGridListeners = {
  cellClick: ({ row }) => {
    handleRowClick(row);
  },
};

const [Grid, gridApi] = useVbenVxeGrid({ gridOptions, gridEvents });

function handleCategoryChange(id: string) {
  activeCategory.value = id;
  gridApi.reload();
}

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  searchKeyword.value = '';
  activeCategory.value = 'all';
  gridApi.reload();
}

function getFileIcon(ext: string) {
  const e = ext?.toLowerCase();
  if (categories.find((c) => c.exts?.includes(e))?.id === 'image')
    return 'lucide:image';
  if (['pdf'].includes(e)) return 'lucide:file-text';
  if (['doc', 'docx'].includes(e)) return 'lucide:file-text';
  if (['csv', 'xls', 'xlsx'].includes(e)) return 'lucide:file-spreadsheet';
  if (['ppt', 'pptx'].includes(e)) return 'lucide:presentation';
  if (['7z', 'gz', 'rar', 'tar', 'zip'].includes(e)) return 'lucide:archive';
  if (['avi', 'mov', 'mp4'].includes(e)) return 'lucide:video';
  if (['aac', 'flac', 'mp3', 'ogg', 'wav'].includes(e)) return 'lucide:music';
  return 'lucide:file';
}

function handleDownload(row: FileObject) {
  if (
    !row.location?.endpoint ||
    !row.location?.bucket ||
    !row.location?.objectKey
  ) {
    message.error('文件下载地址无效');
    return;
  }
  const url = `${row.location.endpoint}/${row.location.bucket}/${row.location.objectKey}`;
  window.open(url, '_blank');
}

function handleDelete(row: FileObject) {
  Modal.confirm({
    title: '确认删除',
    content: `是否确认删除文件 "${row.fileName}"？`,
    onOk: async () => {
      try {
        await deleteFiles([row.id!]);
        message.success('删除成功');
        await gridApi.reload();
      } catch (error) {
        console.error(error);
      }
    },
  });
}

function handleBatchDelete() {
  const selected = gridApi.grid?.getCheckboxRecords();
  if (!selected?.length) {
    message.warning('请先选择要删除的文件');
    return;
  }

  const ids = selected.map((item) => item.id!);
  Modal.confirm({
    title: '确认删除',
    content: `是否确认删除选中的 ${ids.length} 个文件？`,
    onOk: async () => {
      try {
        await deleteFiles(ids);
        message.success('删除成功');
        gridApi.reload();
      } catch (error) {
        console.error(error);
      }
    },
  });
}

function triggerUpload() {
  fileInputRef.value?.click();
}

async function handleFileSelect(e: Event) {
  const files = (e.target as HTMLInputElement).files;
  if (!files?.length) return;

  const file = files[0] as File;
  try {
    message.loading({ content: '上传中...', key: 'uploading' });
    await uploadSingleFileApi({ file });
    message.success({ content: '上传成功', key: 'uploading' });
    await gridApi.reload();
  } catch (error) {
    message.error({ content: '上传失败', key: 'uploading' });
    console.error(error);
  } finally {
    if (fileInputRef.value) fileInputRef.value.value = '';
  }
}
</script>

<template>
  <Page auto-content-height>
    <div class="flex h-full gap-4">
      <!-- 左侧分类 -->
      <div
        class="w-64 flex-shrink-0 rounded-[var(--radius)] border border-border bg-card p-4"
      >
        <div class="mb-4 text-lg font-bold">文件分类</div>
        <div class="flex flex-col gap-1">
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="flex cursor-pointer items-center gap-3 rounded-md px-3 py-2 transition-colors hover:bg-accent"
            :class="{ 'bg-accent text-primary': activeCategory === cat.id }"
            @click="handleCategoryChange(cat.id)"
          >
            <IconifyIcon :icon="cat.icon" class="text-xl" />
            <span>{{ cat.name }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧列表 -->
      <div
        class="flex flex-1 flex-col overflow-hidden rounded-[var(--radius)] border border-border bg-card p-4"
      >
        <!-- 搜索与操作 -->
        <div class="mb-4 flex items-center justify-between gap-4">
          <div class="flex items-center gap-2">
            <Input
              v-model:value="searchKeyword"
              placeholder="搜索文件名"
              allow-clear
              class="w-64"
              @press-enter="handleSearch"
            />
            <Button @click="handleReset">
              <template #icon>
                <IconifyIcon icon="ant-design:reload-outlined" />
              </template>
              重置
            </Button>
            <Button type="primary" @click="handleSearch">
              <template #icon>
                <IconifyIcon icon="ant-design:search-outlined" />
              </template>
              搜索
            </Button>
          </div>
          <div class="flex items-center gap-2">
            <AccessControl :codes="['sys:file:upload']" type="code">
              <input
                ref="fileInputRef"
                type="file"
                class="hidden"
                @change="handleFileSelect"
              />
              <Button type="primary" @click="triggerUpload">
                <template #icon>
                  <IconifyIcon icon="ant-design:cloud-upload-outlined" />
                </template>
                上传文件
              </Button>
            </AccessControl>
            <AccessControl :codes="['sys:file:delete']" type="code">
              <Button danger @click="handleBatchDelete">
                <template #icon>
                  <IconifyIcon icon="ant-design:delete-outlined" />
                </template>
                批量删除
              </Button>
            </AccessControl>
          </div>
        </div>

        <!-- 表格 -->
        <div class="flex-1 overflow-hidden">
          <Grid>
            <template #fileName="{ row }">
              <div class="flex items-center gap-3">
                <IconifyIcon
                  :icon="getFileIcon(row.fileExt || '')"
                  class="text-2xl text-primary"
                />
                <span class="truncate font-medium">{{ row.fileName }}</span>
              </div>
            </template>
            <template #action="{ row }">
              <div class="flex items-center gap-2" @click.stop>
                <AccessControl :codes="['sys:file:download']" type="code">
                  <Tooltip title="下载">
                    <Button
                      type="link"
                      size="small"
                      @click="handleDownload(row)"
                    >
                      <template #icon>
                        <IconifyIcon
                          icon="ant-design:download-outlined"
                          class="text-lg"
                        />
                      </template>
                      下载
                    </Button>
                  </Tooltip>
                </AccessControl>
                <AccessControl :codes="['sys:file:delete']" type="code">
                  <Tooltip title="删除">
                    <Button
                      type="link"
                      size="small"
                      danger
                      @click="handleDelete(row)"
                    >
                      <template #icon>
                        <IconifyIcon
                          icon="ant-design:delete-outlined"
                          class="text-lg"
                        />
                      </template>
                      删除
                    </Button>
                  </Tooltip>
                </AccessControl>
              </div>
            </template>
          </Grid>
        </div>
      </div>
    </div>
  </Page>
</template>

<style scoped lang="scss">
:deep(.vxe-grid) {
  height: 100%;
}
</style>
