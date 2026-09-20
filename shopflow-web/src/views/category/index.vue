<template>
  <div class="page-container">
    <el-card shadow="never">
      <div class="table-toolbar">
        <div>
          <span class="card-title">商品分类</span>
          <el-tag class="tip" type="info" effect="plain" size="small">两级分类树，整棵树缓存在 Redis</el-tag>
        </div>
        <div>
          <el-button type="primary" :icon="Plus" @click="openCreate(null)">新增一级分类</el-button>
          <el-button :icon="Refresh" @click="loadTree">刷新</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="tree"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="分类名称" min-width="220" />
        <el-table-column prop="level" label="层级" width="90">
          <template #default="{ row }">
            <el-tag :type="row.level === 1 ? 'primary' : 'info'" size="small" effect="plain">
              {{ row.level === 1 ? '一级' : '二级' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.level === 1" link type="primary" @click="openCreate(row)">新增子分类</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级分类" prop="parentId">
          <el-select v-model="form.parentId" class="full-width" :disabled="parentDisabled">
            <el-option label="一级分类（无上级）" value="0" />
            <el-option
              v-for="item in firstLevelOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  createCategory,
  deleteCategory,
  fetchCategoryTree,
  updateCategory,
  type CategoryPayload
} from '@/api/category'
import type { CategoryNode } from '@/types'

/**
 * 分类管理。
 *
 * 删除分类有两条业务约束，后端已经校验并返回明确错误码，
 * 前端这里也做一次提示，让用户不用等接口报错就知道原因：
 * 1. 分类下存在子分类不能删；
 * 2. 分类下存在商品不能删。
 */
const loading = ref(false)
const tree = ref<CategoryNode[]>([])
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<CategoryPayload>({
  parentId: '0',
  name: '',
  sort: 0,
  status: 1
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  parentId: [{ required: true, message: '请选择上级分类', trigger: 'change' }]
}

const firstLevelOptions = computed(() => tree.value.filter((node) => node.level === 1))

const parentDisabled = computed(() => editingId.value !== null && form.parentId === '0')

const dialogTitle = computed(() => (editingId.value ? '编辑分类' : '新增分类'))

async function loadTree(): Promise<void> {
  loading.value = true
  try {
    const { data } = await fetchCategoryTree()
    tree.value = data
  } finally {
    loading.value = false
  }
}

function openCreate(parent: CategoryNode | null): void {
  editingId.value = null
  form.parentId = parent ? parent.id : '0'
  form.name = ''
  form.sort = 0
  form.status = 1
  dialogVisible.value = true
}

function openEdit(row: CategoryNode): void {
  editingId.value = row.id
  form.parentId = row.parentId
  form.name = row.name
  form.sort = row.sort
  form.status = row.status
  dialogVisible.value = true
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateCategory(editingId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createCategory({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await loadTree()
  } finally {
    submitting.value = false
  }
}

function handleDelete(row: CategoryNode): void {
  if (row.children && row.children.length > 0) {
    ElMessage.warning('该分类下存在子分类，无法删除')
    return
  }
  ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteCategory(row.id)
      ElMessage.success('删除成功')
      await loadTree()
    })
    .catch(() => undefined)
}

onMounted(loadTree)
</script>

<style scoped>
.tip {
  margin-left: 10px;
}

.full-width {
  width: 100%;
}
</style>