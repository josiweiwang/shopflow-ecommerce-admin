<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="用户名 / 昵称 / 手机号" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.roleId" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="table-toolbar">
        <span class="card-title">用户列表</span>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32">{{ (row.nickname || row.username).slice(0, 1) }}</el-avatar>
              <div>
                <div class="user-name">{{ row.nickname || row.username }}</div>
                <div class="user-account">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="roleNames" label="角色" width="160">
          <template #default="{ row }">
            <el-tag v-for="name in (row.roleNames || '').split('、').filter(Boolean)" :key="name" size="small" effect="plain" class="role-tag">
              {{ name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :disabled="!canUpdate || row.id === userStore.userInfo?.id"
              :before-change="() => toggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最后登录" width="180">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canAssignRole" link type="primary" @click="openRoleDialog(row)">分配角色</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.pageNum!" v-model:limit="query.pageSize!" :total="total" @change="loadData" />
    </el-card>

    <el-dialog v-model="roleDialogVisible" :title="`分配角色 - ${currentUser?.nickname || currentUser?.username || ''}`" width="420px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="角色">
          <el-select v-model="selectedRoleIds" multiple class="full-width" placeholder="请选择角色">
            <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id">
              <span>{{ role.roleName }}</span>
              <span class="role-desc">{{ role.description }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="角色变更后权限缓存立即失效，被授权用户的下一个请求即生效"
        />
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRoles">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination.vue'
import { assignUserRoles, fetchRoleOptions, fetchUserPage, updateUserStatus } from '@/api/user'
import { useUserStore } from '@/stores/user'
import type { RoleOption, UserQuery, UserRow } from '@/types'
import { formatDateTime } from '@/utils/format'

/**
 * 用户管理。
 *
 * 两个细节：
 * 1. 禁止管理员修改自己的状态，避免把自己锁在系统外；
 * 2. 角色变更与禁用都会让后端清除权限缓存，因此变更"立刻"生效，不用等 30 分钟缓存过期。
 */
const userStore = useUserStore()
const canUpdate = computed(() => userStore.hasPermission('user:update'))
const canAssignRole = computed(() => userStore.hasPermission('user:assign-role'))

const loading = ref(false)
const submitting = ref(false)
const records = ref<UserRow[]>([])
const total = ref(0)
const roles = ref<RoleOption[]>([])

const query = reactive<UserQuery>({ pageNum: 1, pageSize: 10, keyword: '', status: undefined, roleId: undefined })

const roleDialogVisible = ref(false)
const currentUser = ref<UserRow | null>(null)
const selectedRoleIds = ref<string[]>([])

async function loadRoles(): Promise<void> {
  const { data } = await fetchRoleOptions()
  roles.value = data
}

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const { data } = await fetchUserPage({ ...query })
    records.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  query.pageNum = 1
  loadData()
}

function handleReset(): void {
  query.keyword = ''
  query.status = undefined
  query.roleId = undefined
  query.pageNum = 1
  loadData()
}

async function toggleStatus(row: UserRow): Promise<boolean> {
  const targetStatus = row.status === 1 ? 0 : 1
  try {
    await updateUserStatus(row.id, targetStatus)
    ElMessage.success(targetStatus === 1 ? '用户已启用' : '用户已禁用，其令牌立即失效')
    await loadData()
    return true
  } catch {
    return false
  }
}

function openRoleDialog(row: UserRow): void {
  currentUser.value = row
  const matched = roles.value.filter((role) => (row.roleNames || '').includes(role.roleName))
  selectedRoleIds.value = matched.map((role) => role.id)
  roleDialogVisible.value = true
}

async function submitRoles(): Promise<void> {
  if (!currentUser.value || selectedRoleIds.value.length === 0) {
    ElMessage.warning('请至少选择一个角色')
    return
  }
  submitting.value = true
  try {
    await assignUserRoles(currentUser.value.id, selectedRoleIds.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadData()])
})
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-name {
  font-weight: 500;
}

.user-account {
  color: #909399;
  font-size: 12px;
}

.role-tag {
  margin-right: 4px;
}

.full-width {
  width: 100%;
}

.role-desc {
  float: right;
  color: #909399;
  font-size: 12px;
}
</style>