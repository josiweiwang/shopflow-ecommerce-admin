<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="商品ID">
          <el-input v-model="query.productId" placeholder="按商品ID筛选" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="低库存">
          <el-switch v-model="query.lowStockOnly" @change="handleSearch" />
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
          <span class="card-title">库存列表</span>
          <el-tag class="tip" type="info" effect="plain" size="small">可用库存由 Redis 缓存 + 数据库条件更新共同保证</el-tag>
        </div>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column label="商品" min-width="240">
          <template #default="{ row }">
            <div class="product-name">{{ row.productName || '（商品已删除）' }}</div>
            <div class="product-sku">SKU：{{ row.sku || '-' }}　ID：{{ row.productId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="可用库存" width="110">
          <template #default="{ row }">
            <span :class="{ 'low-stock': row.lowStock }">{{ row.availableStock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lockedStock" label="锁定库存" width="100" />
        <el-table-column prop="totalStock" label="总库存" width="100" />
        <el-table-column prop="warnStock" label="预警值" width="90" />
        <el-table-column prop="version" label="版本号" width="90" />
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canUpdate" link type="primary" @click="openInbound(row)">入库</el-button>
            <el-button v-if="canUpdate" link type="warning" @click="openAdjust(row)">盘点调整</el-button>
            <el-button link type="info" @click="openLogs(row)">流水</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.pageNum!" v-model:limit="query.pageSize!" :total="total" @change="loadData" />
    </el-card>

    <el-dialog v-model="inboundVisible" title="商品入库" width="420px" destroy-on-close>
      <el-form :model="inboundForm" label-width="80px">
        <el-form-item label="商品">
          <span>{{ currentProductName }}</span>
        </el-form-item>
        <el-form-item label="入库数量" required>
          <el-input-number v-model="inboundForm.quantity" :min="1" :max="1000000" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inboundForm.remark" placeholder="选填" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inboundVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitInbound">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adjustVisible" title="库存盘点调整" width="420px" destroy-on-close>
      <el-form :model="adjustForm" label-width="80px">
        <el-form-item label="商品">
          <span>{{ currentProductName }}</span>
        </el-form-item>
        <el-form-item label="调整数量" required>
          <el-input-number v-model="adjustForm.delta" :min="-100000" :max="100000" />
          <div class="form-hint">正数增加可用库存，负数减少（如报损 -3）</div>
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="adjustForm.remark" placeholder="例如：盘点差异、报损" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAdjust">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="logsVisible" :title="`库存流水 - ${currentProductName}`" size="62%" destroy-on-close>
      <el-table v-loading="logsLoading" :data="logs" border size="small">
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column prop="bizTypeDesc" label="类型" width="90" />
        <el-table-column prop="quantity" label="变更" width="80" />
        <el-table-column label="可用库存变化" width="140">
          <template #default="{ row }">{{ row.beforeAvailable }} → {{ row.afterAvailable }}</template>
        </el-table-column>
        <el-table-column prop="orderNo" label="关联订单" min-width="220" />
        <el-table-column prop="remark" label="备注" min-width="180" />
      </el-table>
      <Pagination v-model:page="logQuery.pageNum!" v-model:limit="logQuery.pageSize!" :total="logTotal" @change="loadLogs" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination.vue'
import {
  adjustStock,
  fetchInventoryLogs,
  fetchInventoryPage,
  inboundStock,
  type InventoryLogQuery,
  type InventoryQuery
} from '@/api/inventory'
import { useUserStore } from '@/stores/user'
import type { InventoryItem, InventoryLogItem } from '@/types'

/**
 * 库存管理。
 *
 * 入库与盘点都走"数据库条件更新 + 缓存失效"，因此调整后第一次读会回源数据库，
 * 不会出现"管理员改完却看到旧值"的问题。
 */
const userStore = useUserStore()
const canUpdate = computed(() => userStore.hasPermission('inventory:update'))

const loading = ref(false)
const records = ref<InventoryItem[]>([])
const total = ref(0)
const submitting = ref(false)

const query = reactive<InventoryQuery>({ pageNum: 1, pageSize: 10, productId: undefined, lowStockOnly: false })

const inboundVisible = ref(false)
const adjustVisible = ref(false)
const logsVisible = ref(false)
const logsLoading = ref(false)
const currentProductId = ref('')
const currentProductName = ref('')
const inboundForm = reactive({ quantity: 10, remark: '' })
const adjustForm = reactive({ delta: 0, remark: '' })

const logs = ref<InventoryLogItem[]>([])
const logTotal = ref(0)
const logQuery = reactive<InventoryLogQuery>({ pageNum: 1, pageSize: 10 })

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const { data } = await fetchInventoryPage({ ...query })
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
  query.productId = undefined
  query.lowStockOnly = false
  query.pageNum = 1
  loadData()
}

function openInbound(row: InventoryItem): void {
  currentProductId.value = row.productId
  currentProductName.value = row.productName || row.productId
  inboundForm.quantity = 10
  inboundForm.remark = ''
  inboundVisible.value = true
}

function openAdjust(row: InventoryItem): void {
  currentProductId.value = row.productId
  currentProductName.value = row.productName || row.productId
  adjustForm.delta = 0
  adjustForm.remark = ''
  adjustVisible.value = true
}

async function submitInbound(): Promise<void> {
  submitting.value = true
  try {
    await inboundStock(currentProductId.value, { ...inboundForm })
    ElMessage.success('入库成功')
    inboundVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

async function submitAdjust(): Promise<void> {
  if (adjustForm.delta === 0) {
    ElMessage.warning('调整数量不能为 0')
    return
  }
  submitting.value = true
  try {
    await adjustStock(currentProductId.value, { ...adjustForm })
    ElMessage.success('调整成功')
    adjustVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

async function openLogs(row: InventoryItem): Promise<void> {
  currentProductId.value = row.productId
  currentProductName.value = row.productName || row.productId
  logQuery.pageNum = 1
  logsVisible.value = true
  await loadLogs()
}

async function loadLogs(): Promise<void> {
  logsLoading.value = true
  try {
    const { data } = await fetchInventoryLogs(currentProductId.value, { ...logQuery })
    logs.value = data.records
    logTotal.value = data.total
  } finally {
    logsLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.tip {
  margin-left: 10px;
}

.product-name {
  font-weight: 500;
}

.product-sku {
  color: #909399;
  font-size: 12px;
}

.low-stock {
  color: #f56c6c;
  font-weight: 700;
}

.form-hint {
  color: #909399;
  font-size: 12px;
}
</style>