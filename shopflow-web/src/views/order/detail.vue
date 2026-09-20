<template>
  <div class="page-container" v-loading="loading">
    <el-card shadow="never" class="header-card">
      <div class="header">
        <div class="header-left">
          <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
          <span class="order-no">{{ detail?.orderNo }}</span>
          <StatusTag v-if="detail" scene="order" :status="detail.status" :text="detail.statusDesc" />
        </div>
        <div class="header-right">
          <el-button v-if="detail?.status === 0" type="success" :loading="submitting" @click="handlePay">模拟支付</el-button>
          <el-button v-if="detail?.status === 0" type="danger" :loading="submitting" @click="handleCancel">取消订单</el-button>
          <el-button v-if="detail?.status === 1 && canUpdate" type="warning" :loading="submitting" @click="handleStatus(2, '发货')">发货</el-button>
          <el-button v-if="detail?.status === 2 && canUpdate" type="success" :loading="submitting" @click="handleStatus(3, '完成')">完成订单</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header><span class="card-title">订单信息</span></template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="订单号">{{ detail?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="下单用户">{{ detail?.username || detail?.userId }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <StatusTag v-if="detail" scene="order" :status="detail.status" :text="detail.statusDesc" />
        </el-descriptions-item>
        <el-descriptions-item label="商品总额">￥{{ formatAmount(detail?.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="运费">￥{{ formatAmount(detail?.freightAmount) }}</el-descriptions-item>
        <el-descriptions-item label="应付金额">
          <span class="amount">￥{{ formatAmount(detail?.payAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="收货人">{{ detail?.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail?.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址">{{ detail?.receiverAddress }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatDateTime(detail?.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="支付截止">{{ formatDateTime(detail?.expireTime) }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ formatDateTime(detail?.payTime) }}</el-descriptions-item>
        <el-descriptions-item label="发货时间">{{ formatDateTime(detail?.deliverTime) }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDateTime(detail?.finishTime) }}</el-descriptions-item>
        <el-descriptions-item label="取消时间">{{ formatDateTime(detail?.cancelTime) }}</el-descriptions-item>
        <el-descriptions-item label="订单备注" :span="3">{{ detail?.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail?.cancelReason" label="取消原因" :span="3">
          {{ detail.cancelReason }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header>
        <span class="card-title">商品明细</span>
        <el-tag class="tip" type="info" effect="plain" size="small">
          下单时刻的商品快照，商品改价改 name 都不影响这里
        </el-tag>
      </template>
      <el-table :data="detail?.items ?? []" border>
        <el-table-column prop="productName" label="商品名称" min-width="220" />
        <el-table-column prop="sku" label="SKU" width="180" />
        <el-table-column label="单价" width="120">
          <template #default="{ row }">￥{{ formatAmount(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="90" />
        <el-table-column label="小计" width="130">
          <template #default="{ row }">
            <span class="amount">￥{{ formatAmount(row.subtotal) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header><span class="card-title">状态流转时间线</span></template>
      <el-timeline>
        <el-timeline-item
          v-for="(log, index) in detail?.statusLogs ?? []"
          :key="index"
          :timestamp="log.createTime"
          :type="log.toStatus === 4 ? 'danger' : 'primary'"
          placement="top"
        >
          <div class="timeline-title">
            {{ log.fromStatusDesc || '创建' }}
            <el-icon><Right /></el-icon>
            {{ log.toStatusDesc }}
          </div>
          <div class="timeline-sub">{{ log.operatorTypeDesc }}：{{ log.remark }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Right } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { cancelOrder, fetchOrderDetail, payOrder, updateOrderStatus } from '@/api/order'
import { useUserStore } from '@/stores/user'
import type { OrderDetail } from '@/types'
import { formatAmount, formatDateTime } from '@/utils/format'

/**
 * 订单详情。
 *
 * 明细来自后端返回的快照字段，不对商品表做二次查询，
 * 因此商品后续改价、下架甚至删除，都不会让历史订单"变样"。
 */
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const canUpdate = computed(() => userStore.hasPermission('order:update'))

const loading = ref(false)
const submitting = ref(false)
const detail = ref<OrderDetail | null>(null)

const orderNo = computed(() => String(route.params.orderNo ?? ''))

async function loadDetail(): Promise<void> {
  loading.value = true
  try {
    const { data } = await fetchOrderDetail(orderNo.value)
    detail.value = data
  } finally {
    loading.value = false
  }
}

function goBack(): void {
  router.push('/order')
}

function handlePay(): void {
  ElMessageBox.confirm(`确认支付订单 ${orderNo.value} 吗？`, '模拟支付', { type: 'info' })
    .then(async () => {
      submitting.value = true
      try {
        await payOrder(orderNo.value)
        ElMessage.success('支付成功')
        await loadDetail()
      } finally {
        submitting.value = false
      }
    })
    .catch(() => undefined)
}

function handleCancel(): void {
  ElMessageBox.prompt('请输入取消原因', '取消订单', {
    inputValue: '用户主动取消',
    inputValidator: (value) => (value && value.length <= 128) || '取消原因不能超过 128 字'
  })
    .then(async ({ value }) => {
      submitting.value = true
      try {
        await cancelOrder(orderNo.value, value)
        ElMessage.success('订单已取消，库存已释放')
        await loadDetail()
      } finally {
        submitting.value = false
      }
    })
    .catch(() => undefined)
}

function handleStatus(targetStatus: number, action: string): void {
  ElMessageBox.confirm(`确认对订单 ${orderNo.value} 执行「${action}」操作吗？`, '提示', { type: 'warning' })
    .then(async () => {
      submitting.value = true
      try {
        await updateOrderStatus(orderNo.value, { targetStatus, remark: `后台${action}` })
        ElMessage.success(`${action}成功`)
        await loadDetail()
      } finally {
        submitting.value = false
      }
    })
    .catch(() => undefined)
}

onMounted(loadDetail)
</script>

<style scoped>
.header-card {
  margin-bottom: 16px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-no {
  font-size: 16px;
  font-weight: 600;
}

.section-card {
  margin-bottom: 16px;
}

.amount {
  color: #f56c6c;
  font-weight: 600;
}

.tip {
  margin-left: 10px;
}

.timeline-title {
  font-weight: 500;
}

.timeline-sub {
  color: #909399;
  font-size: 12px;
  margin-top: 2px;
}
</style>