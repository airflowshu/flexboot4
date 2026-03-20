<script lang="ts" setup>
import type { UserOption } from '#/api/devops/aikey';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Avatar,
  Empty,
  Input,
  List,
  ListItem,
  ListItemMeta,
  Menu,
  Spin,
} from 'ant-design-vue';

import { getUserOptions } from '#/api/devops/aikey';

interface Props {
  initialSelectedIds?: string[];
}

const props = defineProps<Props>();
const emit = defineEmits(['confirm']);

const loading = ref(false);
const allUsers = ref<UserOption[]>([]);
const selectedUsers = ref<UserOption[]>([]);
const searchText = ref('');
const activeMenu = ref(['members']);

// 菜单项
const menuItems = [
  { key: 'members', label: '成员', icon: 'mdi:account' },
  { key: 'dept', label: '部门', icon: 'mdi:file-tree' },
  { key: 'group', label: '群组', icon: 'mdi:account-group' },
];

// 加载用户列表
async function loadUsers() {
  loading.value = true;
  try {
    const res = await getUserOptions();
    allUsers.value = res;
    // 初始化选中状态
    if (props.initialSelectedIds && props.initialSelectedIds.length > 0) {
      selectedUsers.value = allUsers.value.filter((u) =>
        props.initialSelectedIds?.includes(u.id),
      );
    }
  } catch (error) {
    console.error('加载用户失败:', error);
  } finally {
    loading.value = false;
  }
}

// 过滤用户
const filteredUsers = computed(() => {
  if (!searchText.value) return allUsers.value;
  const lowerSearch = searchText.value.toLowerCase();
  return allUsers.value.filter(
    (u) =>
      u.realName?.toLowerCase().includes(lowerSearch) ||
      u.username?.toLowerCase().includes(lowerSearch),
  );
});

// 切换选择
function toggleSelect(user: UserOption) {
  const index = selectedUsers.value.findIndex((u) => u.id === user.id);
  if (index === -1) {
    selectedUsers.value.push(user);
  } else {
    selectedUsers.value.splice(index, 1);
  }
}

// 移除选择
function removeSelect(userId: string) {
  const index = selectedUsers.value.findIndex((u) => u.id === userId);
  if (index !== -1) {
    selectedUsers.value.splice(index, 1);
  }
}

// 判断是否选中
function isSelected(userId: string) {
  return selectedUsers.value.some((u) => u.id === userId);
}

const [Modal, modalApi] = useVbenModal({
  title: '管理协作者',
  fullscreen: false,
  class: 'w-[800px]',
  onConfirm: () => {
    emit('confirm', [...selectedUsers.value]);
    modalApi.close();
  },
  onOpenChange: (isOpen) => {
    if (isOpen) {
      const data = modalApi.getData<{ selectedIds: string[] }>();
      if (data?.selectedIds) {
        // 如果是通过 setData 传入的 initialSelectedIds
        // 需要等待 loadUsers 完成后再设置，或者在 loadUsers 中处理
      }
      loadUsers().then(() => {
        if (data?.selectedIds) {
          selectedUsers.value = allUsers.value.filter((u) =>
            data.selectedIds.includes(u.id),
          );
        }
      });
    } else {
      searchText.value = '';
      selectedUsers.value = [];
    }
  },
});
</script>

<template>
  <Modal>
    <div class="kb-collab-panel flex h-[500px] border-t">
      <!-- 左侧选择区域 -->
      <div class="collab-border-right flex flex-1">
        <!-- 侧边菜单 -->
        <div class="collab-menu w-[120px] pt-2">
          <Menu
            v-model:selected-keys="activeMenu"
            mode="inline"
            class="h-full border-0 bg-transparent"
          >
            <Menu.Item v-for="item in menuItems" :key="item.key">
              <div class="flex items-center gap-2">
                <IconifyIcon :icon="item.icon" />
                <span>{{ item.label }}</span>
              </div>
            </Menu.Item>
          </Menu>
        </div>

        <!-- 列表区域 -->
        <div class="flex flex-1 flex-col p-4">
          <div class="mb-4">
            <Input.Search
              v-model:value="searchText"
              placeholder="搜索成员/部门/群组名称"
              allow-clear
            />
          </div>

          <div class="flex-1 overflow-y-auto">
            <template v-if="activeMenu[0] === 'members'">
              <Spin :spinning="loading">
                <List item-layout="horizontal" :data-source="filteredUsers">
                  <template #renderItem="{ item }">
                    <ListItem
                      class="collab-list-item cursor-pointer rounded px-2 transition-colors"
                      @click="toggleSelect(item)"
                    >
                      <ListItemMeta>
                        <template #title>
                          <span class="font-medium">{{ item.realName }}</span>
                          <span class="collab-muted ml-2 text-xs">{{
                            item.username
                          }}</span>
                        </template>
                        <template #avatar>
                          <Avatar class="bg-primary/10 text-primary">
                            {{
                              item.realName?.charAt(0) ||
                              item.username?.charAt(0)
                            }}
                          </Avatar>
                        </template>
                      </ListItemMeta>
                      <div v-if="isSelected(item.id)">
                        <IconifyIcon
                          icon="mdi:check-circle"
                          class="collab-check-icon"
                        />
                      </div>
                      <div v-else>
                        <div class="collab-check-empty"></div>
                      </div>
                    </ListItem>
                  </template>
                </List>
                <Empty
                  v-if="!loading && filteredUsers.length === 0"
                  description="未找到匹配用户"
                />
              </Spin>
            </template>
            <template v-else>
              <Empty description="暂不支持部门/群组选择" />
            </template>
          </div>
        </div>
      </div>

      <!-- 右侧已选区域 -->
      <div class="collab-selected-panel flex w-[280px] flex-col p-4">
        <div class="mb-4 flex items-center justify-between font-medium">
          <span>已选: {{ selectedUsers.length }}</span>
          <span
            v-if="selectedUsers.length > 0"
            class="collab-clear cursor-pointer text-xs"
            @click="selectedUsers = []"
          >
            清空
          </span>
        </div>

        <div class="flex-1 overflow-y-auto">
          <div class="flex flex-col gap-2">
            <div
              v-for="user in selectedUsers"
              :key="user.id"
              class="collab-selected-item group flex items-center justify-between rounded p-2"
            >
              <div class="flex items-center gap-2 overflow-hidden">
                <Avatar
                  size="small"
                  class="shrink-0 bg-primary/10 text-primary"
                >
                  {{ user.realName?.charAt(0) || user.username?.charAt(0) }}
                </Avatar>
                <div class="flex flex-col overflow-hidden">
                  <span class="truncate text-sm font-medium">{{
                    user.realName
                  }}</span>
                  <span class="collab-muted truncate text-xs">{{
                    user.username
                  }}</span>
                </div>
              </div>
              <IconifyIcon
                icon="mdi:close"
                class="collab-remove-icon"
                @click="removeSelect(user.id)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </Modal>
</template>

<style lang="scss" scoped>
.kb-collab-panel {
  --kb-collab-bg: hsl(var(--card));
  --kb-collab-muted: hsl(var(--muted));
  --kb-collab-border: hsl(var(--border));
  --kb-collab-foreground: hsl(var(--foreground));
  --kb-collab-muted-foreground: hsl(var(--muted-foreground));
  --kb-collab-primary: hsl(var(--primary));
  --kb-collab-danger: hsl(var(--destructive));
}

.kb-collab-panel {
  border-top: 1px solid var(--kb-collab-border);
}

.collab-border-right {
  border-right: 1px solid var(--kb-collab-border);
}

.collab-menu {
  background: var(--kb-collab-muted);
}

.collab-list-item:hover {
  background: var(--kb-collab-muted);
}

.collab-check-icon {
  font-size: 20px;
  color: var(--kb-collab-primary);
}

.collab-check-empty {
  width: 20px;
  height: 20px;
  border: 1px solid var(--kb-collab-border);
  border-radius: 50%;
}

.collab-muted {
  color: var(--kb-collab-muted-foreground);
}

.collab-selected-panel {
  background: var(--kb-collab-bg);
}

.collab-clear {
  color: var(--kb-collab-primary);
}

.collab-selected-item {
  background: var(--kb-collab-muted);
}

.collab-selected-item:hover {
  background: color-mix(in srgb, hsl(var(--muted)), transparent 30%);
}

.collab-remove-icon {
  color: var(--kb-collab-muted-foreground);
  cursor: pointer;

  &:hover {
    color: var(--kb-collab-danger);
  }
}
</style>
