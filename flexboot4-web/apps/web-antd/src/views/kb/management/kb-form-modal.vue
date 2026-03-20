<script lang="ts" setup>
import type { UserOption } from '#/api/devops/aikey';
import type { KnowledgeBase } from '#/api/devops/knowledgebase';

import { ref, watch } from 'vue';

import { useVbenDrawer, useVbenForm, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import { Avatar, message, Tooltip } from 'ant-design-vue';

import {
  addKnowledgeBaseMember,
  createKnowledgeBase,
  getKnowledgeBaseMembers,
  removeKnowledgeBaseMember,
  updateKnowledgeBase,
} from '#/api/devops/knowledgebase';

import CollaboratorSelectModal from './collaborator-select-modal.vue';

interface Props {
  [key: string]: any;
}

const props = defineProps<Props>();

const emit = defineEmits(['success']);

const userStore = useUserStore();
const drawerData = ref<{ mode: string; record?: KnowledgeBase }>({
  mode: 'create',
});
const submitLoading = ref(false);
const selectedCollaborators = ref<UserOption[]>([]);
const originalMembers = ref<string[]>([]); // 用于编辑模式对比

// 协作者选择弹窗
const [CollabModal, collabModalApi] = useVbenModal({
  connectedComponent: CollaboratorSelectModal,
});

function openCollabModal() {
  collabModalApi.setData({
    selectedIds: selectedCollaborators.value.map((u) => u.id),
  });
  collabModalApi.open();
}

function handleCollabConfirm(users: UserOption[]) {
  selectedCollaborators.value = users;
}

// 监听协作者变化，自动调整类型
watch(
  () => selectedCollaborators.value.length,
  (count) => {
    // 获取当前表单的类型值
    const currentType = formApi.getValues()?.type;
    // 如果有除自己以外的协作者（简单判断数量大于1），且当前是私有，则自动切换为团队
    // 注意：这里假设初始状态只有自己
    if (count > 1 && currentType === 'private') {
      formApi.setFieldValue('type', 'team');
      message.info('已添加协作者，知识库类型自动切换为"团队"');
    }
  },
);

// 表单定义
const [Form, formApi] = useVbenForm({
  // 表单公共配置
  commonConfig: {
    labelWidth: 100,
  },
  // 隐藏表单按钮，使用抽屉底部按钮
  showDefaultActions: false,
  // 布局
  layout: 'horizontal',
  schema: [
    {
      fieldName: 'name',
      label: '名称',
      component: 'Input',
      componentProps: {
        placeholder: '请输入知识库名称',
        maxlength: 50,
        showCount: true,
      },
      rules: 'required',
    },
    {
      fieldName: 'description',
      label: '描述',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入知识库描述',
        maxlength: 500,
        showCount: true,
        rows: 4,
      },
    },
    {
      fieldName: 'type',
      label: '类型',
      component: 'Select',
      componentProps: {
        placeholder: '请选择知识库类型',
        options: [
          { value: 'private', label: '私有 - 仅自己可见' },
          { value: 'team', label: '团队 - 团队成员可见' },
          { value: 'public', label: '公开 - 所有人可见' },
        ],
      },
      rules: 'required',
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: {
        placeholder: '请输入备注信息',
        rows: 3,
        maxlength: 200,
      },
    },
  ],
  wrapperClass: 'grid-cols-1',
});

const [Drawer, drawerApi] = useVbenDrawer({
  onCancel() {
    formApi.resetForm();
    drawerApi.close();
  },
  onConfirm: async () => {
    try {
      submitLoading.value = true;
      // 验证表单
      await formApi.validate();
      // 获取表单值
      const values = await formApi.getValues();
      const payload = {
        name: values.name,
        description: values.description,
        type: values.type,
        remark: values.remark,
      };

      let kbId = '';

      if (drawerData.value.mode === 'create') {
        const res = await createKnowledgeBase(payload);
        kbId = res.id;
        message.success('创建成功');

        // 创建模式下，添加选中的协作者
        // 过滤掉可能存在的自己（如果后端自动添加创建者）
        // 或者直接全部添加，后端去重
        const memberIds = selectedCollaborators.value.map((u) => u.id);
        if (memberIds.length > 0) {
          await addKnowledgeBaseMember(kbId, memberIds);
        }
      } else if (drawerData.value.record?.id) {
        kbId = drawerData.value.record.id;
        await updateKnowledgeBase(kbId, payload);

        // 编辑模式下，处理成员增删
        const currentIds = selectedCollaborators.value.map((u) => u.id);
        const originalIds = originalMembers.value;

        // 需要添加的：在 current 中但不在 original 中
        const toAdd = currentIds.filter((id) => !originalIds.includes(id));
        // 需要移除的：在 original 中但不在 current 中
        const toRemove = originalIds.filter((id) => !currentIds.includes(id));

        if (toAdd.length > 0) {
          await addKnowledgeBaseMember(kbId, toAdd);
        }

        // 移除成员需要逐个调用或后端支持批量
        // 假设 removeKnowledgeBaseMember 支持单个
        for (const userId of toRemove) {
          // 避免移除自己（如果是所有者）- 简单保护，实际后端也会校验
          if (userId !== userStore.userInfo?.userId) {
            await removeKnowledgeBaseMember(kbId, userId);
          }
        }

        message.success('更新成功');
      }
      await drawerApi.close();
      emit('success');
    } catch (error) {
      console.error('提交失败:', error);
      message.error('操作失败');
    } finally {
      submitLoading.value = false;
    }
  },
  onOpenChange: async (open) => {
    if (open) {
      const data = drawerApi.getData<{
        mode: string;
        record?: KnowledgeBase;
      }>();
      if (data) {
        drawerData.value = data;
      }
      await formApi.resetForm();

      // 初始化协作者数据
      selectedCollaborators.value = [];
      originalMembers.value = [];

      if (drawerData.value.mode === 'edit' && drawerData.value.record) {
        formApi.setValues({
          name: drawerData.value.record.name,
          description: drawerData.value.record.description,
          type: drawerData.value.record.type,
          remark: drawerData.value.record.remark,
        });

        // 加载现有成员
        try {
          const members = await getKnowledgeBaseMembers(
            drawerData.value.record.id,
          );
          if (members.data) {
            selectedCollaborators.value = members.data.map((m) => ({
              id: m.userId,
              username: m.username,
              realName: m.realName,
            }));
            originalMembers.value = members.data.map((m) => m.userId);
          }
        } catch (error) {
          console.error('加载成员失败', error);
        }
      } else {
        // 创建模式，默认选中自己
        if (userStore.userInfo) {
          const selfUser: UserOption = {
            id: userStore.userInfo.userId || userStore.userInfo.id,
            username: userStore.userInfo.username,
            realName: userStore.userInfo.realName,
          };
          selectedCollaborators.value = [selfUser];
        }
        // 默认类型私有
        formApi.setFieldValue('type', 'private');
      }
    }
  },
});
</script>

<template>
  <Drawer
    :title="drawerData.mode === 'create' ? '新建知识库' : '编辑知识库'"
    :confirm-loading="submitLoading"
    class="kb-form-drawer"
  >
    <div class="flex flex-col">
      <Form />

      <!-- 协作者配置区域 -->
      <div class="flex items-start px-5 pb-5">
        <!-- 仿照 Form Item Label 样式，宽度需与 commonConfig.labelWidth 一致 (100px) -->
        <div class="w-[100px] flex-shrink-0 pr-4 pt-[5px] text-right">
          <div class="flex items-center justify-end gap-1">
            <span
              class="collab-label after:relative after:top-[-0.5px] after:ml-0.5 after:content-[':']"
              >协作者</span>
            <Tooltip title="配置协作者">
              <IconifyIcon
                icon="mdi:cog"
                class="collab-gear-icon"
                @click="openCollabModal"
              />
            </Tooltip>
          </div>
        </div>

        <!-- 仿照 Form Item Control 样式 -->
        <div class="flex-1">
          <div
            class="collab-select flex flex-wrap gap-2 min-h-[32px] p-1 rounded transition-colors cursor-pointer"
            @click="openCollabModal"
          >
            <template v-if="selectedCollaborators.length > 0">
              <Tooltip
                v-for="user in selectedCollaborators"
                :key="user.id"
                :title="`${user.realName || user.username}`"
              >
                <Avatar size="small" class="bg-primary/10 text-primary border border-primary/20">
                  {{ (user.realName || user.username)?.charAt(0)?.toUpperCase() }}
                </Avatar>
              </Tooltip>
            </template>
            <div
              v-else
              class="collab-empty flex h-[22px] items-center px-2 text-sm"
            >
              点击添加协作者
            </div>
          </div>
        </div>
      </div>
    </div>

    <CollabModal @confirm="handleCollabConfirm" />
  </Drawer>
</template>

<style lang="scss" scoped>
.kb-form-drawer {
  --kb-form-foreground: hsl(var(--foreground));
  --kb-form-muted: hsl(var(--muted-foreground));
  --kb-form-border: hsl(var(--border));
  --kb-form-card: hsl(var(--card));
  --kb-form-primary: hsl(var(--primary));
}

.collab-label {
  color: var(--kb-form-foreground);
}

.collab-gear-icon {
  cursor: pointer;
  color: var(--kb-form-muted);
  font-size: 16px;
  transition: color 0.2s ease;

  &:hover {
    color: var(--kb-form-primary);
  }
}

.collab-select {
  background: var(--kb-form-card);
  border: 1px solid var(--kb-form-border);

  &:hover {
    border-color: var(--kb-form-primary);
  }
}

.collab-empty {
  color: var(--kb-form-muted);
}
</style>
