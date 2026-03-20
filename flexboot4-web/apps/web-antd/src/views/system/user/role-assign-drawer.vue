<script lang="ts" setup>
import type { RoleRecord } from '#/api/system/user';

import { ref } from 'vue';

import { useVbenDrawer, useVbenForm, z } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { assignUserRole, getRoleList } from '#/api/system/user';

const emit = defineEmits(['success']);

const recordId = ref('');
const recordName = ref('');
const recordUsername = ref('');

// 角色列表
const roleListData = ref<any[]>([]);

// 获取角色列表
const loadRoleList = async () => {
  try {
    const roles = await getRoleList({ pageNumber: 1, pageSize: 1000 });
    roleListData.value = roles.map((role) => ({
      label: role.roleName,
      value: role.id,
    }));
  } catch (error) {
    console.error('Failed to load role list', error);
  }
};

const [Form, formApi] = useVbenForm({
  showDefaultActions: false,
  commonConfig: {
    componentProps: {
      class: 'w-full',
    },
    labelClass: 'w-1/4',
  },
  layout: 'horizontal',
  schema: [
    {
      component: 'Select',
      fieldName: 'roleIds',
      label: '分配角色',
      rules: 'required',
      componentProps: {
        mode: 'multiple',
        options: roleListData,
        placeholder: '请选择角色',
        maxTagCount: 3,
        showSearch: true,
      },
    },
  ],
  wrapperClass: 'grid-cols-1',
});

const [Drawer, drawerApi] = useVbenDrawer({
  onConfirm: async () => {
    try {
      await formApi.validate();
      const data = await formApi.getValues();

      const roleIds = data.roleIds || [];

      await assignUserRole(recordId.value, roleIds);
      message.success('角色分配成功');

      emit('success');
      drawerApi.close();
    } catch (error) {
      console.error(error);
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      loadRoleList();

      const data = drawerApi.getData<any>();
      recordId.value = data?.id || '';
      recordName.value = data?.realName || '';
      recordUsername.value = data?.username || '';

      // 设置已选中的角色
      const currentRoleIds = data?.roles?.map((r: RoleRecord) => r.id) || [];
      formApi.setValues({
        roleIds: currentRoleIds,
      });
    }
  },
});
</script>

<template>
  <Drawer :title="`关联角色 - ${recordName}`">
    <div class="role-assign-container">
      <!-- 用户信息卡片 -->
      <div class="user-info-card">
        <div class="user-avatar">
          <span class="i-ant-design:user-outlined text-2xl"></span>
        </div>
        <div class="user-details">
          <div class="user-name">{{ recordName || '-' }}</div>
          <div class="user-username">
            <span class="i-ant-design:user-outlined mr-1"></span>
            {{ recordUsername }}
          </div>
        </div>
      </div>

      <!-- 提示信息 -->
      <div class="role-tip">
        <span class="i-ant-design:info-circle-outlined mr-2"></span>
        <span>为用户分配角色，分配后该用户将拥有所选角色的权限</span>
      </div>

      <!-- 表单 -->
      <div class="form-wrapper">
        <Form />
      </div>
    </div>
  </Drawer>
</template>

<style lang="scss" scoped>
.role-assign-container {
  padding: 4px 0;
}

.user-info-card {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 16px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #f6f8fc 0%, #f0f4fa 100%);
  border: 1px solid #e8ecf3;
  border-radius: 8px;

  .user-avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 48px;
    color: #fff;
    background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
    border-radius: 50%;
  }

  .user-details {
    flex: 1;

    .user-name {
      margin-bottom: 4px;
      font-size: 16px;
      font-weight: 600;
      color: #1a1a1a;
    }

    .user-username {
      font-size: 13px;
      color: #666;
    }
  }
}

.role-tip {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #1890ff;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 6px;
}

.form-wrapper {
  padding: 8px 0;
}
</style>
