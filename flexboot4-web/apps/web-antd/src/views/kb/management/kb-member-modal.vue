<script lang="ts" setup>
import type { UserOption } from '#/api/devops/aikey';
import type { KbMember } from '#/api/devops/knowledgebase';

import { ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Avatar,
  Button,
  List,
  ListItem,
  ListItemMeta,
  message,
  Popconfirm,
  Select,
  Spin,
} from 'ant-design-vue';

import { getUserOptions } from '#/api/devops/aikey';
import {
  addKnowledgeBaseMember,
  getKnowledgeBaseMembers,
  removeKnowledgeBaseMember,
} from '#/api/devops/knowledgebase';

const emit = defineEmits(['change']);

const kbId = ref<string>('');
const members = ref<KbMember[]>([]);
const loading = ref(false);
const userOptions = ref<UserOption[]>([]);
const selectedUsers = ref<string[]>([]);
const adding = ref(false);

// 加载成员列表
async function loadMembers() {
  if (!kbId.value) return;
  loading.value = true;
  try {
    const res = await getKnowledgeBaseMembers(kbId.value);
    members.value = res.data || [];
  } catch (error) {
    console.error('加载成员失败:', error);
    message.error('加载成员失败');
  } finally {
    loading.value = false;
  }
}

// 加载用户选项（用于添加成员）
async function loadUserOptions() {
  try {
    const res = await getUserOptions();
    userOptions.value = res;
  } catch (error) {
    console.error('加载用户列表失败:', error);
  }
}

// 添加成员
async function handleAddMember() {
  if (selectedUsers.value.length === 0) return;

  adding.value = true;
  try {
    await addKnowledgeBaseMember(kbId.value, selectedUsers.value);
    message.success('添加成功');
    selectedUsers.value = [];
    await loadMembers();
    emit('change');
  } catch (error) {
    console.error('添加成员失败:', error);
    message.error('添加成员失败');
  } finally {
    adding.value = false;
  }
}

// 移除成员
async function handleRemoveMember(userId: string) {
  try {
    await removeKnowledgeBaseMember(kbId.value, userId);
    message.success('移除成功');
    await loadMembers();
    emit('change');
  } catch (error) {
    console.error('移除成员失败:', error);
    message.error('移除成员失败');
  }
}

const [Modal, modalApi] = useVbenModal({
  title: '协作者管理',
  draggable: true,
  onOpenChange: (isOpen) => {
    if (isOpen) {
      const data = modalApi.getData<{ kbId: string }>();
      if (data?.kbId) {
        kbId.value = data.kbId;
        loadMembers();
        loadUserOptions();
      }
    } else {
      members.value = [];
      selectedUsers.value = [];
    }
  },
});
</script>

<template>
  <Modal class="kb-member-modal w-[600px]">
    <div class="p-4">
      <!-- 添加成员区域 -->
      <div class="mb-6 flex gap-2">
        <Select
          v-model:value="selectedUsers"
          mode="multiple"
          placeholder="搜索并选择用户添加"
          style="flex: 1"
          :options="userOptions"
          :field-names="{ label: 'realName', value: 'id' }"
          option-filter-prop="realName"
          allow-clear
        />
        <Button type="primary" :loading="adding" @click="handleAddMember">
          添加成员
        </Button>
      </div>

      <!-- 成员列表 -->
      <Spin :spinning="loading">
        <List item-layout="horizontal" :data-source="members">
          <template #renderItem="{ item }">
            <ListItem>
              <template #actions>
                <Popconfirm
                  title="确定要移除该成员吗？"
                  @confirm="handleRemoveMember(item.userId)"
                >
                  <Button type="link" danger size="small">移除</Button>
                </Popconfirm>
              </template>
              <ListItemMeta :description="item.role">
                <template #title>
                  <span class="font-medium">{{
                    item.realName || item.username
                  }}</span>
                </template>
                <template #avatar>
                  <Avatar>
                    <template #icon>
                      <IconifyIcon icon="mdi:account" />
                    </template>
                  </Avatar>
                </template>
              </ListItemMeta>
              <div class="member-time">加入时间: {{ item.joinTime }}</div>
            </ListItem>
          </template>
        </List>
      </Spin>
    </div>
  </Modal>
</template>

<style lang="scss" scoped>
.kb-member-modal {
  --kb-member-muted: hsl(var(--muted-foreground));
}

.member-time {
  font-size: 12px;
  color: var(--kb-member-muted);
}
</style>
