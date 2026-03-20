<script lang="ts" setup>
import { ref, reactive, computed, watch } from 'vue';
import { message } from 'ant-design-vue';
import { Steps, Step, Button, Upload, Table, Checkbox, CheckboxGroup, Select, InputNumber, Tooltip, Progress } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

import { uploadFile } from '#/api/devops/knowledgebase';

interface UploadFile {
  id: string;
  name: string;
  size: number;
  progress: number;
  status: 'uploading' | 'success' | 'error';
  rawFile?: File;
  error?: string;
}

const props = defineProps<{
  kbId: string;
  parentId?: string;
}>();

const emit = defineEmits(['close', 'success']);

const currentStep = ref(0);
const loading = ref(false);
const uploading = ref(false);

// Step 1: 文件选择
const selectedFiles = ref<UploadFile[]>([]);

// Step 2: 参数设置
const config = reactive({
  parsing: {
    pdfEnhanced: true,
  },
  processing: {
    mode: 'chunk', // chunk or qa
    chunkCondition: 'length',
    chunkValue: 1000,
    indexing: ['title'],
    paramMode: 'default'
  }
});

// Step 3: 数据预览 (暂用模拟数据)
const previewFiles = ref<UploadFile[]>([]);
const previewContent = ref('');

// Step 4: 确认上传
const finalFiles = computed(() => selectedFiles.value);

const steps = [
  { title: '选择文件' },
  { title: '参数设置' },
  { title: '数据预览' },
  { title: '确认上传' }
];

// 格式化文件大小
function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// 文件选择处理
function handleFileChange(info: any) {
  const files = info.fileList
    .filter((f: any) => f.originFileObj)
    .map((f: any, index: number) => ({
      id: `file-${Date.now()}-${index}`,
      name: f.name,
      size: f.size || f.originFileObj?.size || 0,
      progress: 0,
      status: 'uploading' as const,
      rawFile: f.originFileObj,
    }));

  // 合并已选择的文件
  const existingNames = new Set(selectedFiles.value.map((f) => f.name));
  const newFiles = files.filter((f: UploadFile) => !existingNames.has(f.name));

  if (newFiles.length > 0) {
    selectedFiles.value = [...selectedFiles.value, ...newFiles];
    message.success(`已添加 ${newFiles.length} 个文件`);
  }
}

// 移除文件
function removeFile(id: string) {
  selectedFiles.value = selectedFiles.value.filter((f) => f.id !== id);
}

// 预览步骤变化时，同步文件到预览
watch(currentStep, (step) => {
  if (step === 2) {
    previewFiles.value = selectedFiles.value.map((f) => ({ ...f }));
    previewContent.value = selectedFiles.value.length > 0
      ? `已选择 ${selectedFiles.value.length} 个文件\n\n文件列表：\n${selectedFiles.value.map((f) => `- ${f.name} (${formatFileSize(f.size)})`).join('\n')}`
      : '';
  }
});

function next() {
  if (selectedFiles.value.length === 0) {
    message.warning('请先选择文件');
    return;
  }
  currentStep.value++;
}

function prev() {
  currentStep.value--;
}

function handleClose() {
  emit('close');
}

async function handleStartUpload() {
  if (selectedFiles.value.length === 0) return;

  uploading.value = true;

  try {
    // 上传所有文件
    for (const file of selectedFiles.value) {
      file.status = 'uploading';
      file.progress = 0;

      try {
        await uploadFile(
          {
            kbId: props.kbId,
            parentId: props.parentId,
            bizType: 'kb_dataset',
          },
          file.rawFile!
        );
        file.status = 'success';
        file.progress = 100;
        file.error = undefined;
      } catch (error: any) {
        file.status = 'error';
        file.error = error?.message || '上传失败';
        message.error(`文件 "${file.name}" 上传失败`);
      }
    }

    // 检查是否全部成功
    const successCount = selectedFiles.value.filter((f) => f.status === 'success').length;

    if (successCount > 0) {
      message.success(`成功上传 ${successCount} 个文件`);
      emit('success');
      emit('close');
    }
  } catch (error) {
    console.error('上传失败:', error);
    message.error('上传过程中发生错误');
  } finally {
    uploading.value = false;
  }
}
</script>

<template>
  <div class="import-wizard-overlay">
    <div class="wizard-header">
      <Button type="text" class="exit-btn" @click="handleClose">
        <template #icon><IconifyIcon icon="mdi:arrow-left" /></template>
        退出
      </Button>
      <div class="steps-container">
        <Steps :current="currentStep" size="small" class="custom-steps">
          <Step v-for="item in steps" :key="item.title" :title="item.title" />
        </Steps>
      </div>
    </div>

    <div class="wizard-body">
      <!-- Step 1: 选择文件 -->
      <div v-if="currentStep === 0" class="step-content step-1">
        <Upload.Dragger
          class="custom-dragger"
          :multiple="true"
          :show-upload-list="false"
          accept=".txt,.docx,.csv,.xlsx,.pdf,.md,.html,.pptx"
          :before-upload="() => false"
          @change="handleFileChange"
        >
          <div class="dragger-inner">
            <IconifyIcon icon="mdi:cloud-upload-outline" class="upload-icon" />
            <p class="upload-text">点击或拖动文件到此处上传</p>
            <p class="upload-hint">支持 .txt, .docx, .csv, .xlsx, .pdf, .md, .html, .pptx 类型文件</p>
            <p class="upload-hint">最多支持 15 个文件，单个文件最大 100 MB</p>
          </div>
        </Upload.Dragger>

        <div class="file-table-wrapper mt-6">
          <Table
            :pagination="false"
            :columns="[
              { title: '文件名', dataIndex: 'name', key: 'name' },
              { title: '文件上传进度', dataIndex: 'progress', key: 'progress' },
              { title: '文件大小', dataIndex: 'size', key: 'size' },
              { title: '操作', key: 'action', width: 80 }
            ]"
            :data-source="selectedFiles"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <div class="flex items-center gap-2">
                  <IconifyIcon icon="mdi:file-document-outline" class="wizard-primary-icon" />
                  <span>{{ record.name }}</span>
                </div>
              </template>
              <template v-else-if="column.key === 'progress'">
                <Progress
                  :percent="record.progress"
                  :status="record.status === 'error' ? 'exception' : record.status === 'success' ? 'success' : 'active'"
                  size="small"
                  stroke-color="hsl(var(--success))"
                />
              </template>
              <template v-else-if="column.key === 'size'">
                {{ formatFileSize(record.size) }}
              </template>
              <template v-else-if="column.key === 'action'">
                <Button type="text" size="small" danger @click="removeFile(record.id)">
                  <IconifyIcon icon="mdi:trash-can-outline" />
                </Button>
              </template>
            </template>
          </Table>
        </div>

        <div class="wizard-footer">
          <Button
            type="primary"
            class="next-btn"
            :disabled="selectedFiles.length === 0"
            @click="next"
          >
            共 {{ selectedFiles.length }} 个文件 | 下一步
          </Button>
        </div>
      </div>

      <!-- Step 2: 参数设置 -->
      <div v-if="currentStep === 1" class="step-content step-2">
        <div class="config-section">
          <h3 class="section-title">文件解析设置</h3>
          <div class="config-card active">
            <Checkbox v-model:checked="config.parsing.pdfEnhanced">
              <span class="font-medium ml-2">PDF增强解析</span>
              <Tooltip title="对PDF中的表格、公式等进行增强识别">
                <IconifyIcon icon="mdi:help-circle-outline" class="ml-1 wizard-muted-icon" />
              </Tooltip>
            </Checkbox>
            <Tag color="blue" class="ml-auto">1积分/页</Tag>
          </div>
        </div>

        <div class="config-section mt-8">
          <h3 class="section-title">数据处理方式设置</h3>
          <div class="grid grid-cols-2 gap-4 mb-6">
            <div
              class="mode-card"
              :class="{ active: config.processing.mode === 'chunk' }"
              @click="config.processing.mode = 'chunk'"
            >
              <div class="mode-radio"><div class="radio-inner"></div></div>
              <div class="mode-info">
                <div class="mode-name">
                  分块存储 <IconifyIcon icon="mdi:help-circle-outline" class="wizard-muted-icon" />
                </div>
              </div>
            </div>
            <div
              class="mode-card"
              :class="{ active: config.processing.mode === 'qa' }"
              @click="config.processing.mode = 'qa'"
            >
              <div class="mode-radio"><div class="radio-inner"></div></div>
              <div class="mode-info">
                <div class="mode-name">
                  问答对提取 <IconifyIcon icon="mdi:help-circle-outline" class="wizard-muted-icon" />
                </div>
              </div>
            </div>
          </div>

          <div class="config-item mb-6">
            <div class="item-label">分块条件 <IconifyIcon icon="mdi:help-circle-outline" class="wizard-muted-icon" /></div>
            <div class="flex gap-2">
              <Select v-model:value="config.processing.chunkCondition" style="width: 200px">
                <Select.Option value="length">原文长度大于</Select.Option>
              </Select>
              <InputNumber v-model:value="config.processing.chunkValue" style="width: 120px" />
            </div>
          </div>

          <div class="config-item mb-6">
            <div class="item-label">索引增强</div>
            <CheckboxGroup v-model:value="config.processing.indexing">
              <div class="flex gap-6">
                <Checkbox value="title">
                  将标题加入索引 <IconifyIcon icon="mdi:help-circle-outline" class="wizard-muted-icon" />
                </Checkbox>
                <Checkbox value="auto">
                  自动生成补充索引 <IconifyIcon icon="mdi:help-circle-outline" class="wizard-muted-icon" />
                </Checkbox>
                <Checkbox value="image">
                  图片自动索引 <IconifyIcon icon="mdi:help-circle-outline" class="wizard-muted-icon" />
                </Checkbox>
              </div>
            </CheckboxGroup>
          </div>

          <div class="config-item">
            <div class="item-label">分块处理参数</div>
            <div class="flex flex-col gap-3">
              <div
                class="param-card"
                :class="{ active: config.processing.paramMode === 'default' }"
                @click="config.processing.paramMode = 'default'"
              >
                <div class="mode-radio"><div class="radio-inner"></div></div>
                <div class="param-info">
                  <div class="param-name">默认</div>
                  <div class="param-desc">使用系统默认的参数和规则</div>
                </div>
              </div>
              <div
                class="param-card"
                :class="{ active: config.processing.paramMode === 'custom' }"
                @click="config.processing.paramMode = 'custom'"
              >
                <div class="mode-radio"><div class="radio-inner"></div></div>
                <div class="param-info">
                  <div class="param-name">自定义</div>
                  <div class="param-desc">自定义设置数据处理规则</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="wizard-footer-simple">
          <Button type="primary" class="next-btn-small" @click="next">下一步</Button>
        </div>
      </div>

      <!-- Step 3: 数据预览 -->
      <div v-if="currentStep === 2" class="step-content step-3">
        <div class="preview-layout">
          <div class="preview-sidebar">
            <div class="sidebar-title">文件列表</div>
            <div class="preview-file-list">
              <div
                v-for="file in previewFiles"
                :key="file.id"
                class="preview-file-item"
                :class="{ active: file.active }"
              >
                <IconifyIcon icon="mdi:file-document-outline" class="wizard-primary-icon" />
                <span class="truncate">{{ file.name }}</span>
              </div>
            </div>
          </div>
          <div class="preview-main">
            <div class="preview-header">
              <span class="font-medium">分块预览</span>
              <span class="wizard-muted-hint">共 1 个分块，最多展示 10 个</span>
            </div>
            <div class="preview-content">
              {{ previewContent }}
            </div>
          </div>
        </div>
        <div class="wizard-footer-simple">
          <Button type="primary" class="next-btn-small" @click="next">下一步</Button>
        </div>
      </div>

      <!-- Step 4: 确认上传 -->
      <div v-if="currentStep === 3" class="step-content step-4">
        <div class="file-table-wrapper">
          <Table
            :pagination="false"
            :columns="[
              { title: '来源名', dataIndex: 'name', key: 'name' },
              { title: '状态', dataIndex: 'status', key: 'status' },
              { title: '操作', key: 'action', width: 80 }
            ]"
            :data-source="finalFiles"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <div class="flex items-center gap-2">
                  <IconifyIcon icon="mdi:file-document-outline" class="wizard-primary-icon" />
                  <span>{{ record.name }}</span>
                </div>
              </template>
              <template v-else-if="column.key === 'status'">
                <span
                  :class="{
                    'status-uploading': record.status === 'uploading',
                    'status-success': record.status === 'success',
                    'status-error': record.status === 'error'
                  }"
                >
                  <template v-if="record.status === 'uploading'">上传中</template>
                  <template v-else-if="record.status === 'success'">上传成功</template>
                  <template v-else-if="record.status === 'error'">{{ record.error || '上传失败' }}</template>
                </span>
              </template>
              <template v-else-if="column.key === 'action'">
                <Button
                  type="text"
                  size="small"
                  class="icon-action-btn"
                  :disabled="record.status === 'uploading'"
                  @click="removeFile(record.id)"
                >
                  <IconifyIcon icon="mdi:trash-can-outline" class="wizard-muted-icon" />
                </Button>
              </template>
            </template>
          </Table>
        </div>

        <div class="wizard-footer">
          <Button
            type="primary"
            class="next-btn"
            :loading="uploading"
            :disabled="uploading"
            @click="handleStartUpload"
          >
            共 {{ finalFiles.length }} 个文件 | 开始上传
          </Button>
        </div>
      </div>
    </div>

    <!-- 上一步按钮 (Step 1-3) -->
    <div v-if="currentStep > 0" class="prev-btn-wrapper">
      <Button type="text" @click="prev" class="wizard-prev-btn">
        <template #icon><IconifyIcon icon="mdi:arrow-left" /></template>
        上一步
      </Button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.import-wizard-overlay {
  --kb-wizard-bg: hsl(var(--background-deep));
  --kb-wizard-card: hsl(var(--card));
  --kb-wizard-border: hsl(var(--border));
  --kb-wizard-foreground: hsl(var(--foreground));
  --kb-wizard-muted: hsl(var(--muted));
  --kb-wizard-muted-foreground: hsl(var(--muted-foreground));
  --kb-wizard-primary: hsl(var(--primary));
  --kb-wizard-success: hsl(var(--success));
  --kb-wizard-danger: hsl(var(--destructive));

  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  background: var(--kb-wizard-bg);
}

.wizard-header {
  position: relative;
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 24px;
  background: var(--kb-wizard-card);
  border-bottom: 1px solid var(--kb-wizard-border);

  .exit-btn {
    position: absolute;
    top: 50%;
    left: 24px;
    display: flex;
    gap: 4px;
    align-items: center;
    height: 32px;
    padding: 0 12px;
    color: var(--kb-wizard-muted-foreground);
    background: var(--kb-wizard-card);
    border: 1px solid var(--kb-wizard-border);
    border-radius: 6px;
    transform: translateY(-50%);
  }

  .steps-container {
    flex: 1;
    max-width: 800px;
    padding: 12px 24px;
    margin: 0 auto;
    background: var(--kb-wizard-muted);
    border-radius: 12px;
  }
}

.wizard-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  overflow-y: auto;
}

.step-content {
  width: 100%;
  max-width: 1000px;
  padding: 32px;
  background: var(--kb-wizard-card);
  border-radius: 16px;
  box-shadow: 0 1px 3px rgb(0 0 0 / 5%);
}

.custom-dragger {
  background: var(--kb-wizard-card) !important;
  border: 1px dashed var(--kb-wizard-primary) !important;
  border-radius: 12px !important;

  .dragger-inner {
    padding: 48px 0;

    .upload-icon {
      margin-bottom: 12px;
      font-size: 40px;
      color: var(--kb-wizard-primary);
    }

    .upload-text {
      margin-bottom: 8px;
      font-size: 16px;
      font-weight: 500;
      color: var(--kb-wizard-foreground);
    }

    .upload-hint {
      margin: 2px 0;
      font-size: 12px;
      color: var(--kb-wizard-muted-foreground);
    }
  }
}

.file-table-wrapper {
  width: 100%;
  overflow: hidden;
  background: var(--kb-wizard-muted);
  border-radius: 12px;

  :deep(.ant-table) {
    background: transparent;
  }

  :deep(.ant-table-thead > tr > th) {
    font-size: 12px;
    font-weight: 500;
    color: var(--kb-wizard-muted-foreground);
    background: var(--kb-wizard-muted);
  }

  :deep(.ant-table-tbody > tr > td) {
    background: var(--kb-wizard-card);
    border-bottom: 1px solid var(--kb-wizard-border);
  }
}

.wizard-footer {
  display: flex;
  justify-content: flex-end;
  width: 100%;
  margin-top: 32px;

  .next-btn {
    height: 40px;
    padding: 0 24px;
    font-weight: 500;
    border-radius: 8px;
  }
}

.wizard-footer-simple {
  display: flex;
  justify-content: center;
  width: 100%;
  margin-top: 32px;

  .next-btn-small {
    height: 32px;
    padding: 0 20px;
    border-radius: 6px;
  }
}

// Step 2 Styles
.config-section {
  .section-title {
    padding-left: 8px;
    margin-bottom: 16px;
    font-size: 14px;
    font-weight: 600;
    color: var(--kb-wizard-foreground);
    border-left: 3px solid var(--kb-wizard-primary);
  }
}

.config-card {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: var(--kb-wizard-card);
  border: 1px solid var(--kb-wizard-border);
  border-radius: 8px;

  &.active {
    background: hsl(var(--primary) / 0.08);
    border-color: var(--kb-wizard-primary);
  }
}

.mode-card, .param-card {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 16px;
  cursor: pointer;
  border: 1px solid var(--kb-wizard-border);
  border-radius: 8px;
  transition: all 0.2s;

  &:hover {
    border-color: var(--kb-wizard-primary);
  }

  &.active {
    background: color-mix(in srgb, hsl(var(--primary)), transparent 92%);
    border-color: var(--kb-wizard-primary);

    .mode-radio .radio-inner {
      transform: scale(1);
    }
  }
}

.mode-radio {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  background: var(--kb-wizard-card);
  border: 1px solid var(--kb-wizard-border);
  border-radius: 50%;

  .radio-inner {
    width: 8px;
    height: 8px;
    background: var(--kb-wizard-primary);
    border-radius: 50%;
    transform: scale(0);
    transition: transform 0.2s;
  }
}

.mode-name, .param-name {
  display: flex;
  gap: 4px;
  align-items: center;
  font-weight: 500;
  color: var(--kb-wizard-foreground);
}

.param-desc {
  font-size: 12px;
  color: var(--kb-wizard-muted-foreground);
}

.item-label {
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--kb-wizard-muted-foreground);
}

// Step 3 Styles
.preview-layout {
  display: flex;
  height: 500px;
  overflow: hidden;
  border: 1px solid var(--kb-wizard-border);
  border-radius: 12px;
}

.preview-sidebar {
  display: flex;
  flex-direction: column;
  width: 240px;
  border-right: 1px solid var(--kb-wizard-border);

  .sidebar-title {
    padding: 16px;
    font-weight: 600;
    background: var(--kb-wizard-muted);
    border-bottom: 1px solid var(--kb-wizard-border);
  }

  .preview-file-list {
    flex: 1;
    padding: 8px;
    overflow-y: auto;
  }

  .preview-file-item {
    display: flex;
    gap: 8px;
    align-items: center;
    padding: 8px 12px;
    font-size: 13px;
    color: var(--kb-wizard-muted-foreground);
    cursor: pointer;
    border-radius: 6px;

    &.active {
      color: var(--kb-wizard-primary);
      background: color-mix(in srgb, hsl(var(--primary)), transparent 92%);
      border: 1px solid color-mix(in srgb, hsl(var(--primary)), transparent 70%);
    }
  }
}

.preview-main {
  display: flex;
  flex: 1;
  flex-direction: column;

  .preview-header {
    display: flex;
    align-items: center;
    padding: 16px 24px;
    border-bottom: 1px solid var(--kb-wizard-border);
  }

  .preview-content {
    flex: 1;
    padding: 24px;
    overflow-y: auto;
    font-size: 14px;
    line-height: 1.8;
    color: var(--kb-wizard-foreground);
    white-space: pre-wrap;
  }
}


.wizard-primary-icon {
  font-size: 18px;
  color: var(--kb-wizard-primary);
}

.wizard-muted-icon {
  color: var(--kb-wizard-muted-foreground);
}

.wizard-muted-hint {
  margin-left: auto;
  font-size: 12px;
  color: var(--kb-wizard-muted-foreground);
}

.status-uploading {
  color: var(--kb-wizard-muted-foreground);
}

.status-success {
  color: var(--kb-wizard-success);
}

.status-error {
  color: var(--kb-wizard-danger);
}

.icon-action-btn {
  background: var(--kb-wizard-muted);
  border: 1px solid var(--kb-wizard-border);
  border-radius: 6px;

  &:hover {
    background: color-mix(in srgb, hsl(var(--muted)), transparent 30%);
  }
}

.wizard-prev-btn {
  height: 32px;
  padding: 0 12px;
  background: var(--kb-wizard-card);
  border: 1px solid var(--kb-wizard-border);
  border-radius: 6px;
  box-shadow: 0 1px 2px rgb(0 0 0 / 6%);
}
</style>
