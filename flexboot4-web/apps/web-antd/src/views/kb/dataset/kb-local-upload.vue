<script lang="ts" setup>
import { ref } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, message, Progress, Table, Upload } from 'ant-design-vue';

import { uploadFiles } from '#/api/devops/knowledgebase';

interface UploadFile {
  id: string;
  name: string;
  size: number;
  progress: number;
  status: 'error' | 'pending' | 'success' | 'uploading';
  rawFile?: File;
  error?: string;
}

const props = defineProps<{
  kbId: string;
  parentId?: string;
}>();

const emit = defineEmits(['close', 'success']);

const uploading = ref(false);
const selectedFiles = ref<UploadFile[]>([]);
const dropProcessing = ref(false);

// 处理拖拽事件
async function handleDrop(e: DragEvent) {
  e.preventDefault();
  e.stopPropagation();

  if (dropProcessing.value) return;

  const items = e.dataTransfer?.items;
  if (!items) return;

  const files: File[] = [];

  // 递归获取文件夹中的所有文件
  async function traverseFileTree(item: any, path: string = '') {
    return new Promise<void>((resolve) => {
      if (item.isFile) {
        item.file((file: File) => {
          files.push(file);
          resolve();
        });
      } else if (item.isDirectory) {
        const dirReader = item.createReader();
        dirReader.readEntries(async (entries: any[]) => {
          for (const entry of entries) {
            await traverseFileTree(entry, `${path}${item.name}/`);
          }
          resolve();
        });
      } else {
        resolve();
      }
    });
  }

  dropProcessing.value = true;

  // 获取所有文件
  for (const item of items) {
    if (item.kind === 'file') {
      const entry = item.webkitGetAsEntry();
      if (entry) {
        await traverseFileTree(entry);
      }
    }
  }

  if (files.length > 0) {
    // 转换文件为 UploadFile 格式
    const newFiles: UploadFile[] = files.map((file, index) => ({
      id: `file-${Date.now()}-${index}`,
      name: file.name,
      size: file.size,
      progress: 0,
      status: 'pending' as const,
      rawFile: file,
    }));

    // 合并到已选文件，过滤重复
    const existingNames = new Set(selectedFiles.value.map((f) => f.name));
    const uniqueFiles = newFiles.filter((f) => !existingNames.has(f.name));

    if (uniqueFiles.length > 0) {
      selectedFiles.value = [...selectedFiles.value, ...uniqueFiles];
      message.success(`已添加 ${uniqueFiles.length} 个文件`);
    }
  }

  dropProcessing.value = false;
}

// 处理点击选择文件
function handleFileChange(info: any) {
  const files = info.fileList
    .filter((f: any) => f.originFileObj)
    .map((f: any, index: number) => ({
      id: `file-${Date.now()}-${index}`,
      name: f.name,
      size: f.size > 0 ? f.size : f.originFileObj?.size || 0,
      progress: 0,
      status: 'pending' as const,
      rawFile: f.originFileObj,
    }));

  // 合并到已选文件，过滤重复
  const existingNames = new Set(selectedFiles.value.map((f) => f.name));
  const newFiles = files.filter((f: UploadFile) => !existingNames.has(f.name));

  if (newFiles.length > 0) {
    selectedFiles.value = [...selectedFiles.value, ...newFiles];
    message.success(`已添加 ${newFiles.length} 个文件`);
  }
}

// 阻止默认拖拽行为
function handleDragOver(e: DragEvent) {
  e.preventDefault();
  e.stopPropagation();
}

// 移除文件
function removeFile(id: string) {
  selectedFiles.value = selectedFiles.value.filter((f) => f.id !== id);
}

// 获取文件状态文本
function getStatusText(status: string): string {
  const statusMap: Record<string, string> = {
    pending: '待上传',
    uploading: '上传中',
    success: '上传成功',
    error: '上传失败',
  };
  return statusMap[status] || status;
}

// 获取文件状态类名
function getStatusClass(status: string): string {
  const classMap: Record<string, string> = {
    pending: 'status-pending',
    uploading: 'status-uploading',
    success: 'status-success',
    error: 'status-error',
  };
  return classMap[status] || '';
}

// 开始上传
async function handleStartUpload() {
  const filesToUpload = selectedFiles.value.filter(
    (f) => f.status === 'pending' || f.status === 'error',
  );

  if (filesToUpload.length === 0) {
    message.warning('没有需要上传的文件');
    return;
  }

  // 更新所有文件状态为上传中
  filesToUpload.forEach((file) => {
    file.status = 'uploading';
    file.progress = 0;
  });

  uploading.value = true;

  try {
    // 批量上传
    const files = filesToUpload.map((f) => f.rawFile!).filter(Boolean);
    const uploadedIds = await uploadFiles(props.kbId, files, props.parentId);

    // 更新文件状态
    filesToUpload.forEach((file, index) => {
      if (uploadedIds[index]) {
        file.status = 'success';
        file.progress = 100;
        file.error = undefined;
      } else {
        file.status = 'error';
        file.error = '上传失败';
      }
    });

    const successCount = selectedFiles.value.filter(
      (f) => f.status === 'success',
    ).length;
    const errorCount = selectedFiles.value.filter(
      (f) => f.status === 'error',
    ).length;

    if (successCount > 0) {
      message.success(`成功上传 ${successCount} 个文件`);
      if (errorCount > 0) {
        message.warning(`${errorCount} 个文件上传失败`);
      }
      emit('success');
      emit('close');
    } else {
      message.error('上传失败');
    }
  } catch (error: any) {
    console.error('上传失败:', error);
    // 更新所有文件状态为失败
    filesToUpload.forEach((file) => {
      file.status = 'error';
      file.error = error?.message || '上传失败';
    });
    message.error('上传过程中发生错误');
  } finally {
    uploading.value = false;
  }
}

// 关闭弹窗
function handleClose() {
  emit('close');
}
</script>

<template>
  <div class="upload-page-overlay">
    <div class="upload-page-header">
      <div class="header-left">
        <Button type="text" class="exit-btn" @click="handleClose">
          <template #icon>
            <IconifyIcon icon="mdi:close" />
          </template>
        </Button>
        <span class="header-title">本地上传</span>
      </div>
    </div>

    <div class="upload-page-body">
      <!-- 上传区域 -->
      <div
        class="custom-dragger"
        :class="{ 'dragger-active': dropProcessing }"
        @drop="handleDrop"
        @dragover="handleDragOver"
      >
        <Upload
          class="upload-trigger"
          :multiple="true"
          :show-upload-list="false"
          accept=".txt,.docx,.csv,.xlsx,.pdf,.md,.html,.pptx"
          :before-upload="() => false"
          @change="handleFileChange"
        >
          <div class="dragger-inner">
            <div class="icon-wrapper">
              <IconifyIcon
                icon="mdi:cloud-upload-outline"
                class="upload-icon"
              />
            </div>
            <p class="upload-text">点击或拖动文件到此处上传</p>
            <p class="upload-hint">
              支持 .txt, .docx, .csv, .xlsx, .pdf, .md, .html, .pptx 类型文件
            </p>
            <p class="upload-hint">单个文件最大 100 MB</p>
          </div>
        </Upload>
        <div v-if="dropProcessing" class="drop-overlay">
          <IconifyIcon icon="mdi:loading" class="drop-loading-icon spin" />
          <p>正在读取文件夹中的文件...</p>
        </div>
      </div>

      <!-- 文件列表 -->
      <div v-if="selectedFiles.length > 0" class="file-table-wrapper">
        <Table
          :pagination="false"
          :columns="[
            { title: '来源名', dataIndex: 'name', key: 'name' },
            { title: '状态', dataIndex: 'status', key: 'status' },
            { title: '操作', key: 'action', width: 80 },
          ]"
          :data-source="selectedFiles"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div class="flex items-center gap-2">
                <IconifyIcon
                  icon="mdi:file-document-outline"
                  class="upload-status-icon"
                />
                <span>{{ record.name }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'status'">
              <div class="flex items-center gap-2">
                <template v-if="record.status === 'uploading'">
                  <Progress
                    :percent="record.progress"
                    size="small"
                    stroke-color="hsl(var(--success))"
                    style="width: 80px"
                  />
                </template>
                <template v-else>
                  <span :class="getStatusClass(record.status)">
                    {{ getStatusText(record.status) }}
                    <span v-if="record.status === 'error'" class="text-xs">
                      ({{ record.error }})
                    </span>
                  </span>
                </template>
              </div>
            </template>
            <template v-else-if="column.key === 'action'">
              <Button
                type="text"
                size="small"
                danger
                :disabled="record.status === 'uploading'"
                @click="removeFile(record.id)"
              >
                <IconifyIcon icon="mdi:trash-can-outline" />
              </Button>
            </template>
          </template>
        </Table>

        <!-- 上传按钮 -->
        <div class="upload-actions">
          <Button
            type="primary"
            size="large"
            :loading="uploading"
            :disabled="uploading"
            @click="handleStartUpload"
          >
            <template #icon>
              <IconifyIcon icon="mdi:upload" />
            </template>
            开始上传 ({{ selectedFiles.length }} 个文件)
          </Button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.upload-page-overlay {
  --kb-upload-bg: hsl(var(--background-deep));
  --kb-upload-card: hsl(var(--card));
  --kb-upload-border: hsl(var(--border));
  --kb-upload-foreground: hsl(var(--foreground));
  --kb-upload-muted: hsl(var(--muted));
  --kb-upload-muted-foreground: hsl(var(--muted-foreground));
  --kb-upload-primary: hsl(var(--primary));
  --kb-upload-success: hsl(var(--success));
  --kb-upload-danger: hsl(var(--destructive));

  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  background: var(--kb-upload-bg);
}

.upload-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 24px;
  background: var(--kb-upload-card);
  border-bottom: 1px solid var(--kb-upload-border);

  .header-left {
    display: flex;
    gap: 12px;
    align-items: center;

    .exit-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      padding: 0;
      color: var(--kb-upload-muted-foreground);
      background: var(--kb-upload-card);
      border: 1px solid var(--kb-upload-border);
      border-radius: 6px;

      &:hover {
        color: var(--kb-upload-foreground);
        background: var(--kb-upload-muted);
      }
    }

    .header-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--kb-upload-foreground);
    }
  }
}

.upload-page-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  overflow-y: auto;
}

.custom-dragger {
  position: relative;
  width: 100%;
  max-width: 800px;
  background: var(--kb-upload-card);
  border: 1px dashed var(--kb-upload-primary);
  border-radius: 12px;
  transition: all 0.3s;

  &:hover {
    background: var(--kb-upload-muted) !important;
    border-color: var(--kb-upload-primary);
  }

  &.dragger-active {
    border-color: var(--kb-upload-success);
    border-style: solid;
  }

  .upload-trigger {
    width: 100%;

    :deep(.ant-upload-select) {
      width: 100% !important;
      height: auto !important;
      margin: 0 !important;
      background: transparent !important;
      border: none !important;
    }
  }

  .dragger-inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 48px;

    .icon-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 80px;
      height: 80px;
      margin-bottom: 16px;
      background: color-mix(in srgb, hsl(var(--primary)), transparent 88%);
      border-radius: 50%;
    }

    .upload-icon {
      font-size: 40px;
      color: var(--kb-upload-primary);
    }

    .upload-text {
      margin: 12px 0 8px;
      font-size: 16px;
      font-weight: 500;
      color: var(--kb-upload-foreground);
    }

    .upload-hint {
      margin: 2px 0;
      font-size: 12px;
      color: var(--kb-upload-muted-foreground);
    }
  }

  .drop-overlay {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    gap: 12px;
    align-items: center;
    justify-content: center;
    background: color-mix(in srgb, hsl(var(--card)), transparent 5%);
    border-radius: 12px;

    .drop-loading-icon {
      font-size: 32px;
      color: var(--kb-upload-success);
    }

    p {
      font-size: 14px;
      color: var(--kb-upload-muted-foreground);
    }
  }
}

.upload-status-icon {
  font-size: 18px;
  color: var(--kb-upload-primary);
}

.status-pending {
  color: var(--kb-upload-muted-foreground);
}

.status-uploading {
  color: var(--kb-upload-primary);
}

.status-success {
  color: var(--kb-upload-success);
}

.status-error {
  color: var(--kb-upload-danger);
}

.file-table-wrapper {
  width: 100%;
  max-width: 800px;
  margin-top: 24px;
  overflow: hidden;
  background: var(--kb-upload-card);
  box-shadow: 0 1px 3px rgb(0 0 0 / 5%);

  :deep(.ant-table) {
    background: transparent;
  }

  :deep(.ant-table-thead > tr > th) {
    font-size: 13px;
    font-weight: 500;
    color: var(--kb-upload-muted-foreground);
    background: var(--kb-upload-muted);
  }

  :deep(.ant-table-tbody > tr > td) {
    background: var(--kb-upload-card);
    border-bottom: 1px solid var(--kb-upload-border);
  }
}

.upload-actions {
  display: flex;
  justify-content: center;
  padding: 20px;
  border-top: 1px solid var(--kb-upload-border);

  .ant-btn {
    height: 40px;
    padding: 0 32px;
    font-size: 15px;
    border-radius: 8px;
  }
}
</style>
