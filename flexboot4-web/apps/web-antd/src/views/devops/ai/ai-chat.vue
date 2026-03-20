<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue';

import {
  Button,
  Dropdown,
  Input,
  Menu,
  MenuItem,
  message,
  Spin,
  Tooltip,
} from 'ant-design-vue';

import { baseRequestClient } from '#/api/request';
import { createSSEConnection } from '#/utils/sse';

interface ChatMessage {
  id: string;
  role: 'ai' | 'user';
  content: string;
  timestamp: number;
  isStreaming?: boolean;
}

interface KnowledgeBase {
  id: string;
  name: string;
}

const messages = ref<ChatMessage[]>([]);
const inputValue = ref('');
const isLoading = ref(false);
const isStreaming = ref(false);
const chatContainerRef = ref<HTMLElement | null>(null);
const inputRef = ref<any>(null);
let abortController: AbortController | null = null;

// 知识库相关
const knowledgeBaseList = ref<KnowledgeBase[]>([]);
const selectedKbId = ref<null | string>(null);
const selectedKbName = ref<string>('通用对话');
const kbDropdownOpen = ref(false);
const fileInputRef = ref<HTMLInputElement | null>(null);

// 获取知识库列表
const fetchKnowledgeBases = async () => {
  try {
    // 使用 baseRequestClient，避免触发全局错误提示
    const res = await baseRequestClient.post('/admin/kb/list', {
      pageNumber: 1,
      pageSize: 100,
    });
    // 处理返回数据：可能是 {code: 0, data: [...]} 或直接 [...]
    const responseData = res.data;
    const list = Array.isArray(responseData?.data)
      ? responseData.data
      : (Array.isArray(responseData)
        ? responseData
        : []);
    if (Array.isArray(list)) {
      knowledgeBaseList.value = list;
    }
  } catch {
    // 静默处理，接口不存在时不显示错误提示
    knowledgeBaseList.value = [];
  }
};

// 选择知识库
const selectKnowledgeBase = (kb: KnowledgeBase) => {
  selectedKbId.value = kb.id;
  selectedKbName.value = kb.name;
  kbDropdownOpen.value = false;
};

// 切换知识库下拉菜单
const toggleKbDropdown = () => {
  if (knowledgeBaseList.value.length > 0) {
    kbDropdownOpen.value = !kbDropdownOpen.value;
  }
};

// 点击上传文件
const handleUploadClick = () => {
  fileInputRef.value?.click();
};

// 处理文件选择
const handleFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = input.files;
  if (files && files.length > 0) {
    const file = files[0];
    message.success(`已选择文件: ${file.name}`);
    // TODO: 实现实际上传逻辑
    console.log('上传文件:', file.name);
  }
  // 清空 input，允许重复选择同一文件
  input.value = '';
};

// 生成唯一ID
const generateId = () =>
  `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`;

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick();
  if (chatContainerRef.value) {
    chatContainerRef.value.scrollTop = chatContainerRef.value.scrollHeight;
  }
};

// 关闭SSE连接
const closeEventSource = () => {
  if (abortController) {
    abortController.abort();
    abortController = null;
  }
  // 重置流式状态
  isStreaming.value = false;
  isLoading.value = false;
};

// 停止流式传输
const stopStreaming = () => {
  closeEventSource();
  // 更新所有流式消息的状态
  messages.value.forEach((msg) => {
    if (msg.isStreaming) {
      msg.isStreaming = false;
    }
  });
  // 清空输入框
  inputValue.value = '';
};

// 处理SSE响应
const handleSSEResponse = (chunk: string, messageId: string) => {
  const msgIndex = messages.value.findIndex(
    (m: ChatMessage) => m.id === messageId,
  );
  if (msgIndex !== -1) {
    const msg = messages.value[msgIndex];
    if (msg) {
      msg.content += chunk;
      scrollToBottom();
    }
  }
};

// 发送消息到后端SSE接口
const sendMessage = async () => {
  // 如果正在流式传输，点击按钮则停止
  if (isStreaming.value) {
    stopStreaming();
    return;
  }

  const trimmedValue = inputValue.value.trim();
  if (!trimmedValue || isLoading.value) return;

  // 不再强制要求选择知识库

  const userMessage: ChatMessage = {
    id: generateId(),
    role: 'user',
    content: trimmedValue,
    timestamp: Date.now(),
  };

  const loadingMessage: ChatMessage = {
    id: generateId(),
    role: 'ai',
    content: '',
    timestamp: Date.now(),
    isStreaming: true,
  };

  messages.value.push(userMessage, loadingMessage);
  inputValue.value = '';
  isLoading.value = true;
  isStreaming.value = true;
  await scrollToBottom();

  closeEventSource();

  // 创建SSE连接（使用 fetch 实现，支持自定义请求头）
  const url = `/api/ai/rag/chat/stream`;
  const aiMessageId = loadingMessage.id;

  abortController = createSSEConnection({
    url,
    body: {
      query: trimmedValue,
      stream: true,
      // kbId 为可选参数，不选择知识库时则不传
      ...(selectedKbId.value ? { kbId: selectedKbId.value } : {}),
    },
    onOpen: () => {
      console.warn('SSE连接已建立');
    },
    onMessage: (data) => {
      try {
        // 处理 OpenAI 格式的 SSE 数据
        if (data.startsWith('{')) {
          try {
            const chunkData = JSON.parse(data);

            // 检查是否是错误响应
            if (chunkData.error) {
              message.error(chunkData.error.message || '请求失败');
              stopStreaming();
              return;
            }

            // 提取流式内容 (OpenAI 格式)
            const content = chunkData?.choices?.[0]?.delta?.content;
            if (content) {
              handleSSEResponse(content, aiMessageId);
            }

            // 检查是否结束
            const finishReason = chunkData?.choices?.[0]?.finish_reason;
            if (finishReason) {
              stopStreaming();
            }
            return;
          } catch {
            // 解析失败，忽略
          }
        }
        // 处理普通文本数据
        if (data) {
          handleSSEResponse(data, aiMessageId);
        }
      } catch (error) {
        console.error('解析SSE数据失败:', error);
      }
    },
    onError: (error) => {
      // 尝试解析后端返回的错误信息
      let errorMessage = 'SSE 连接失败';
      if (error?.message) {
        try {
          const errorData = JSON.parse(error.message);
          errorMessage = errorData.message || errorData.error || errorMessage;
        } catch {
          errorMessage = error.message;
        }
      }
      message.error(errorMessage);
      isLoading.value = false;
      isStreaming.value = false;

      // 更新消息状态
      const msgIndex = messages.value.findIndex(
        (m: ChatMessage) => m.id === aiMessageId,
      );
      if (msgIndex !== -1) {
        const msg = messages.value[msgIndex];
        if (msg) {
          msg.isStreaming = false;
        }
      }
    },
    onComplete: () => {
      isLoading.value = false;
      isStreaming.value = false;
      const msgIndex = messages.value.findIndex(
        (m: ChatMessage) => m.id === aiMessageId,
      );
      if (msgIndex !== -1) {
        const msg = messages.value[msgIndex];
        if (msg) {
          msg.isStreaming = false;
        }
      }
    },
  });
};

// 键盘事件处理
const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    sendMessage();
  }
};

// 清空对话
const clearChat = () => {
  messages.value = [];
};

// 生成图片（保留，未实现）
const handleGenerateImage = () => {
  // TODO: 实现生成图片功能
};

// 组件卸载时关闭连接
onUnmounted(() => {
  closeEventSource();
});

// 初始聚焦输入框
onMounted(async () => {
  await nextTick();
  inputRef.value?.focus();
  // 初始化加载知识库列表
  fetchKnowledgeBases();
});
</script>

<template>
  <div class="ai-chat-container">
    <!-- 头部 -->
    <div class="chat-header">
      <div class="header-title">
        <span class="ai-icon">&#x2728;</span>
        <span>AI 助手</span>
        <!-- 知识库选择（如果有知识库则显示） -->
        <Dropdown
          v-if="knowledgeBaseList.length > 0"
          :open="kbDropdownOpen"
          trigger="click"
        >
          <div
            class="kb-selector"
            :class="{ active: selectedKbId }"
            @click="toggleKbDropdown"
          >
            <span>{{ selectedKbName }}</span>
            <span class="arrow">&#x25BC;</span>
          </div>
          <template #overlay>
            <Menu class="kb-dropdown-menu">
              <MenuItem
                @click="selectKnowledgeBase({ id: '', name: '通用对话' })"
                :class="{ 'kb-item-selected': !selectedKbId }"
              >
                <div class="kb-item-content">
                  <span>通用对话</span>
                  <span v-if="!selectedKbId" class="check-icon">✓</span>
                </div>
              </MenuItem>
              <MenuItem
                v-for="kb in knowledgeBaseList"
                :key="kb.id"
                @click="selectKnowledgeBase(kb)"
                :class="{ 'kb-item-selected': selectedKbId === kb.id }"
              >
                <div class="kb-item-content">
                  <span>{{ kb.name }}</span>
                  <span v-if="selectedKbId === kb.id" class="check-icon">
                    ✓
                  </span>
                </div>
              </MenuItem>
            </Menu>
          </template>
        </Dropdown>
      </div>
      <Button danger size="small" @click="clearChat"> 清空对话 </Button>
    </div>

    <!-- 聊天内容区域 -->
    <div ref="chatContainerRef" class="chat-messages">
      <!-- 欢迎消息（空状态） -->
      <div v-if="messages.length === 0" class="welcome-message">
        <div class="welcome-icon">&#x2728;</div>
        <div class="welcome-title">有什么可以帮您的？</div>
        <div class="welcome-subtitle">输入您的问题，我会尽力帮助您解答</div>
      </div>

      <!-- 消息列表 -->
      <template v-else>
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-item"
          :class="msg.role"
        >
          <!-- 用户头像 -->
          <div class="message-avatar">
            <template v-if="msg.role === 'user'">
              <svg
                viewBox="0 0 24 24"
                width="16"
                height="16"
                fill="currentColor"
              >
                <path
                  d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
                />
              </svg>
            </template>
            <template v-else>
              <div class="ai-avatar">
                <span>&#x2728;</span>
              </div>
            </template>
          </div>

          <!-- 消息内容 -->
          <div class="message-content">
            <div class="message-bubble">
              <Spin v-if="msg.isStreaming" size="small" />
              <span class="message-text">{{ msg.content }}</span>
              <span v-if="msg.isStreaming" class="typing-cursor">|</span>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-wrapper">
      <input
        ref="fileInputRef"
        type="file"
        style="display: none"
        @change="handleFileChange"
      />
      <div class="chat-input-container">
        <!-- 左侧工具栏 - 上传文件 -->
        <div class="input-tools">
          <Tooltip title="上传本地文件">
            <div class="tool-button upload-button" @click="handleUploadClick">
              <svg
                viewBox="0 0 24 24"
                width="20"
                height="20"
                fill="currentColor"
              >
                <path d="M9 16h6v-6h4l-7-7-7 7h4v6zm-4 2h14v2H5v-2z" />
              </svg>
            </div>
          </Tooltip>
        </div>

        <!-- 输入框 -->
        <div class="input-field-wrapper">
          <Input.TextArea
            ref="inputRef"
            v-model:value="inputValue"
            :placeholder="isLoading ? 'AI 正在思考中...' : '输入消息'"
            :disabled="isLoading"
            :rows="1"
            auto-size
            @keydown="handleKeydown"
            class="chat-input-field"
          />
        </div>

        <!-- 右侧工具栏 -->
        <div class="input-actions">
          <Tooltip title="生成图片">
            <div class="tool-button action-button" @click="handleGenerateImage">
              <svg
                viewBox="0 0 24 24"
                width="18"
                height="18"
                fill="currentColor"
              >
                <path
                  d="M21 3H3c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H3V5h18v14zM5 15h14v3H5z"
                />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <path
                  d="M12 10c-2.5 0-4.5 2-4.5 4.5s2 4.5 4.5 4.5 4.5-2 4.5-4.5-2-4.5-4.5-4.5z"
                />
              </svg>
            </div>
          </Tooltip>
          <Tooltip :title="isStreaming ? '停止生成' : '发送'">
            <Button
              class="tool-button send-button"
              :class="{
                disabled: !inputValue.trim() && !isStreaming,
                loading: isLoading || isStreaming,
              }"
              :disabled="!inputValue.trim() && !isStreaming"
              html-type="button"
              @click="sendMessage"
            >
              <!-- 发送图标 -->
              <Spin v-if="isLoading || isStreaming" size="small" />
              <svg
                v-else
                viewBox="0 0 24 24"
                width="18"
                height="18"
                fill="currentColor"
              >
                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
              </svg>
            </Button>
          </Tooltip>
        </div>
      </div>

      <div class="input-disclaimer">AI 可能会产生错误信息，请核实重要内容</div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@keyframes blink {
  0%,
  50% {
    opacity: 1;
  }

  51%,
  100% {
    opacity: 0;
  }
}

.ai-chat-container {
  --chat-bg: hsl(var(--card));
  --chat-header-bg: hsl(var(--card));
  --chat-border: hsl(var(--border));
  --chat-foreground: hsl(var(--foreground));
  --chat-muted-bg: hsl(var(--muted));
  --chat-muted-foreground: hsl(var(--muted-foreground));
  --chat-input-bg: hsl(var(--input-background));
  --chat-input-border: hsl(var(--input));
  --chat-input-placeholder: hsl(var(--input-placeholder));
  --chat-hover-bg: hsl(var(--accent));
  --chat-hover-strong: hsl(var(--accent-hover));
  --chat-kb-bg: hsl(var(--accent));
  --chat-kb-active-bg: color-mix(in srgb, hsl(var(--accent)), transparent 40%);
  --chat-kb-active-fg: hsl(var(--primary));
  --chat-kb-hover-bg: color-mix(in srgb, hsl(var(--accent)), transparent 20%);
  --chat-user-bubble-from: hsl(var(--primary));
  --chat-user-bubble-to: hsl(var(--primary));
  --chat-user-foreground: hsl(var(--primary-foreground));
  --chat-user-shadow: 0 4px 15px color-mix(in srgb, hsl(var(--primary)), transparent 70%);
  --chat-ai-bubble-bg: hsl(var(--muted));
  --chat-ai-foreground: hsl(var(--foreground));
  --chat-ai-avatar-from: hsl(var(--success));
  --chat-ai-avatar-to: hsl(var(--success));
  --chat-send-bg: hsl(var(--primary));
  --chat-send-foreground: hsl(var(--primary-foreground));
  --chat-scrollbar: hsl(var(--accent-dark));

  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--chat-bg);
  border-radius: 16px;
}

:global(.dark) .ai-chat-container {
  --chat-user-shadow: 0 6px 18px rgb(0 0 0 / 35%);
  --chat-ai-bubble-bg: hsl(var(--accent));
  --chat-kb-active-bg: hsl(var(--accent));
  --chat-kb-hover-bg: color-mix(in srgb, hsl(var(--accent)), transparent 30%);
  --chat-scrollbar: hsl(var(--accent));
}

// 头部样式
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: var(--chat-header-bg);
  border-bottom: 1px solid var(--chat-border);

  .header-title {
    display: flex;
    gap: 8px;
    align-items: center;
    font-size: 16px;
    font-weight: 600;
    color: var(--chat-foreground);

    .ai-icon {
      font-size: 20px;
    }

    .current-kb-tag {
      padding: 4px 10px;
      font-size: 12px;
      font-weight: 500;
      color: var(--chat-kb-active-fg);
      background: var(--chat-kb-active-bg);
      border: 1px solid var(--chat-kb-hover-bg);
      border-radius: 12px;
    }

    .kb-selector {
      display: flex;
      gap: 4px;
      align-items: center;
      padding: 4px 10px;
      font-size: 12px;
      font-weight: 500;
      color: var(--chat-muted-foreground);
      cursor: pointer;
      background: var(--chat-kb-bg);
      border-radius: 12px;
      transition: all 0.2s ease;

      &:hover {
        background: var(--chat-kb-hover-bg);
      }

      &.active {
        color: var(--chat-kb-active-fg);
        background: var(--chat-kb-active-bg);
      }

      .arrow {
        margin-left: 2px;
        font-size: 8px;
      }
    }
  }
}

// 聊天消息区域
.chat-messages {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 20px;
  padding: 24px;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--chat-scrollbar);
    border-radius: 3px;
  }
}

// 欢迎消息
.welcome-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px 20px;
  text-align: center;

  .welcome-icon {
    margin-bottom: 20px;
    font-size: 56px;
  }

  .welcome-title {
    margin-bottom: 12px;
    font-size: 24px;
    font-weight: 600;
    color: var(--chat-foreground);
  }

  .welcome-subtitle {
    font-size: 15px;
    color: var(--chat-muted-foreground);
  }
}

// 消息项
.message-item {
  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(10px);
    }

    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  display: flex;
  gap: 16px;
  animation: fadeIn 0.3s ease;
  width: 100%;
  max-width: none;

  &.user {
    flex-direction: row-reverse;
    align-self: flex-end;

    .message-avatar {
      color: var(--chat-user-foreground);
      background: linear-gradient(
        135deg,
        var(--chat-user-bubble-from) 0%,
        var(--chat-user-bubble-to) 100%
      );
    }

    .message-bubble {
      color: var(--chat-user-foreground);
      background: linear-gradient(
        135deg,
        var(--chat-user-bubble-from) 0%,
        var(--chat-user-bubble-to) 100%
      );
      border-radius: 20px 20px 4px;
      box-shadow: var(--chat-user-shadow);
    }
  }

  &.ai {
    align-self: flex-start;

    .message-avatar {
      color: var(--chat-user-foreground);
      background: linear-gradient(
        135deg,
        var(--chat-ai-avatar-from) 0%,
        var(--chat-ai-avatar-to) 100%
      );
    }

    .message-bubble {
      color: var(--chat-ai-foreground);
      background: var(--chat-ai-bubble-bg);
      border-radius: 20px 20px 20px 4px;
      border: 1px solid var(--chat-border);
    }
  }
}

.message-content {
  display: flex;
  flex: 1;
}

.message-item.user .message-content {
  justify-content: flex-end;
}

.message-item.ai .message-content {
  justify-content: flex-start;
}

.message-avatar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  overflow: hidden;
  font-size: 18px;
  border-radius: 50%;
  box-shadow: 0 2px 8px rgb(0 0 0 / 20%);

  .ai-avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    font-size: 20px;
  }
}

.message-bubble {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  min-height: 24px;
  padding: 14px 20px;
  font-size: 15px;
  line-height: 1.6;
  word-break: break-word;
  max-width: 72%;

  .message-text {
    flex: 1;
    white-space: pre-wrap;
  }

  .typing-cursor {
    font-weight: 300;
    color: var(--chat-muted-foreground);
    animation: blink 1s infinite;
  }
}

// 输入区域包装器
.chat-input-wrapper {
  position: relative;
  padding: 16px 24px 20px;
  background: var(--chat-bg);
  border-top: 1px solid var(--chat-border);
}

// 输入容器
.chat-input-container {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  padding: 8px 8px 8px 12px;
  background: var(--chat-input-bg);
  border: 1px solid var(--chat-input-border);
  border-radius: 24px;
  transition: all 0.3s ease;

  &:focus-within {
    border-color: hsl(var(--primary));
    box-shadow: 0 0 0 2px color-mix(in srgb, hsl(var(--primary)), transparent 90%);
  }
}

// 工具按钮
.tool-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  color: var(--chat-muted-foreground);
  cursor: pointer;
  background: transparent;
  border: none;
  border-radius: 50%;
  transition: all 0.2s ease;

  &:hover {
    color: var(--chat-foreground);
    background: var(--chat-hover-bg);
    transform: scale(1.05);
  }

  &.send-button {
    color: var(--chat-send-foreground);
    background: var(--chat-send-bg);

    &:hover {
      box-shadow: 0 4px 15px rgb(0 0 0 / 20%);
    }

    &.disabled,
    &[disabled]:not(.ant-btn-loading) {
      cursor: not-allowed;
      opacity: 0.4;

      &:hover {
        background: var(--chat-send-bg);
        box-shadow: none;
        transform: none;
      }
    }

    // loading 状态保持可点击
    &.ant-btn-loading {
      cursor: pointer !important;

      &:hover {
        box-shadow: 0 4px 15px rgb(0 0 0 / 20%);
      }
    }
  }

  &.action-button {
    margin-right: 4px;
  }

  &.upload-button {
    &:hover {
      color: var(--chat-kb-active-fg);
    }
  }
}

// 输入框包装器
.input-field-wrapper {
  flex: 1;
  min-width: 0;
}

.chat-input-field {
  width: 100%;
  padding: 8px 0;
  font-size: 15px;
  line-height: 1.5;
  color: var(--chat-foreground);
  resize: none;
  background: transparent !important;
  border: none !important;

  &::placeholder {
    color: var(--chat-input-placeholder);
  }

  :deep(.ant-input) {
    color: var(--chat-foreground);
    outline: none !important;
    background: transparent !important;
    border: none !important;
    box-shadow: none !important;

    &::placeholder {
      color: var(--chat-input-placeholder);
    }
  }
}

// 输入免责声明
.input-disclaimer {
  margin-top: 12px;
  font-size: 12px;
  color: var(--chat-muted-foreground);
  text-align: center;
  letter-spacing: 0.3px;
}

// 知识库下拉菜单
.kb-dropdown-menu {
  z-index: 1050;
  min-width: 180px;
  max-height: 280px;
  padding: 4px;
  overflow-y: auto;
  border-radius: 8px;

  .kb-item-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
  }

  .check-icon {
    font-weight: bold;
    color: var(--chat-kb-active-fg);
  }

  .kb-empty {
    padding: 12px;
    font-size: 13px;
    color: var(--chat-muted-foreground);
    text-align: center;
  }

  :deep(.ant-dropdown-menu-item) {
    padding: 10px 12px;
    border-radius: 6px;

    &:hover {
      background: var(--chat-hover-bg);
    }
  }

  .kb-item-selected {
    color: var(--chat-kb-active-fg);
    background: var(--chat-kb-active-bg) !important;
  }
}

@media (max-width: 768px) {
  .chat-messages {
    padding: 16px;
    gap: 16px;
  }

  .message-bubble {
    max-width: 90%;
    padding: 12px 16px;
  }

  .message-avatar {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }

  .chat-input-wrapper {
    padding: 12px 16px 16px;
  }
}
</style>
