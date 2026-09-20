<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="操作描述 / 用户名" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="query.module" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="item in modules" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.success" placeholder="全部" clearable style="width: 120px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
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
        <div>
          <span class="card-title">操作日志</span>
          <el-tag class="tip" type="info" effect="plain" size="small">AOP 异步落库，请求参数已脱敏</el-tag>
        </div>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="110" />
        <el-table-column prop="operation" label="操作" width="150" />
        <el-table-column label="请求" min-width="240">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="methodTagType(row.requestMethod)">{{ row.requestMethod }}</el-tag>
            <span class="uri">{{ row.requestUri }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">
            <span :class="{ 'slow-call': row.durationMs > 500 }">{{ row.durationMs }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="失败原因" min-width="180" show-overflow-tooltip />
      </el-table>

      <Pagination v-model:page="query.pageNum!" v-model:limit="query.pageSize!" :total="total" @change="loadData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination.vue'
import { fetchOperationLogs, type OperationLogQuery } from '@/api/user'
import type { OperationLogRow } from '@/types'

/**
 * 操作日志。
 *
 * 耗时超过 500ms 的请求会高亮，配合后端的慢接口告警日志一起用于定位性能问题。
 */
const loading = ref(false)
const records = ref<OperationLogRow[]>([])
const total = ref(0)

const modules = ['商品管理', '商品分类', '库存管理', '订单管理', '用户管理']

const query = reactive<OperationLogQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  module: undefined,
  success: undefined
})

function methodTagType(method: string): 'primary' | 'success' | 'warning' | 'danger' {
  switch (method) {
    case 'POST':
      return 'primary'
    case 'PUT':
    case 'PATCH':
      return 'warning'
    case 'DELETE':
      return 'danger'
    default:
      return 'success'
  }
}

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const { data } = await fetchOperationLogs({ ...query })
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
  query.module = undefined
  query.success = undefined
  query.pageNum = 1
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.tip {
  margin-left: 10px;
}

.uri {
  margin-left: 8px;
  color: #606266;
}

.slow-call {
  color: #f56c6c;
  font-weight: 700;
}
</style>