<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="精确匹配订单号" clearable style="width: 220px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="item in ORDER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="下单时间">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 380px"
          />
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
          <span class="card-title">订单列表</span>
          <el-tag class="tip" type="info" effect="plain" size="small">
            {{ userStore.isAdmin ? '管理员可查看全部订单' : '仅显示当前账号的订单' }}
          </el-tag>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">模拟下单</el-button>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column prop="orderNo" label="订单号" width="240" />
        <el-table-column prop="username" label="下单用户" width="120" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span class="amount">￥{{ formatAmount(row.payAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag scene="order" :status="row.status" :text="row.statusDesc" />
          </template>
        </el-table-column>
        <el-table-column label="收货人" width="160">
          <template #default="{ row }">
            <div>{{ row.receiverName }}</div>
            <div class="sub-text">{{ row.receiverPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="商品" width="110">
          <template #default="{ row }">
            {{ row.itemCount ?? 0 }} 种 / {{ row.totalQuantity ?? 0 }} 件
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="180" />
        <el-table-column prop="expireTime" label="支付截止" width="180" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
            <el-button v-if="row.status === 0" link type="success" @click="handlePay(row)">支付</el-button>
            <el-button v-if="row.status === 0" link type="danger" @click="openCancel(row)">取消</el-button>
            <el-button v-if="row.status === 1 && canUpdate" link type="warning" @click="handleStatus(row, 2, '发货')">发货</el-button>
            <el-button v-if="row.status === 2 && canUpdate" link type="success" @click="handleStatus(row, 3, '完成')">完成</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.pageNum!" v-model:limit="query.pageSize!" :total="total" @change="loadData" />
    </el-card>

    <el-dialog v-model="createVisible" title="模拟下单" width="560px" destroy-on-close>
      <el-alert
        class="create-tip"
        type="info"
        :closable="false"
        show-icon
        title="前端生成幂等号，重复提交只会产生一笔订单；库存采用 Redis 原子预扣 + 数据库条件更新防超卖"
      />
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="商品" prop="productId">
          <el-select v-model="createForm.productId" placeholder="请选择上架商品" filterable style="width: 100%">
            <el-option
              v-for="item in productOptions"
              :key="item.id"
              :label="`${item.name}（￥${formatAmount(item.price)}｜可用 ${item.availableStock ?? 0}）`"
              :value="item.id"
              :disabled="(item.availableStock ?? 0) <= 0"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="createForm.quantity" :min="1" :max="999" />
        </el-form-item>
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="createForm.receiverName" placeholder="收货人姓名" maxlength="32" />
        </el-form-item>
        <el-form-item label="联系电话" prop="receiverPhone">
          <el-input v-model="createForm.receiverPhone" placeholder="11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="收货地址" prop="receiverAddress">
          <el-input v-model="createForm.receiverAddress" type="textarea" :rows="2" placeholder="详细收货地址" maxlength="255" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" placeholder="选填" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">提交订单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="cancelVisible" title="取消订单" width="420px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="订单号">
          <span>{{ currentOrder?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="取消原因">
          <el-input v-model="cancelReason" type="textarea" :rows="2" placeholder="例如：不想要了" maxlength="128" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelVisible = false">返回</el-button>
        <el-button type="danger" :loading="submitting" @click="submitCancel">确认取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination.vue'
import StatusTag from '@/components/StatusTag.vue'
import { cancelOrder, createOrder, fetchOrderPage, payOrder, updateOrderStatus } from '@/api/order'
import { fetchProductPage } from '@/api/product'
import { useUserStore } from '@/stores/user'
import type { OrderQuery, OrderRow, ProductItem } from '@/types'
import { ORDER_STATUS_OPTIONS, formatAmount, generateRequestNo } from '@/utils/format'

/**
 * 订单列表。
 *
 * 这里刻意把「模拟下单」放在后台页面里：既能演示完整下单链路，
 * 又不需要额外做一个前台商城页面，面试演示时一条龙走完下单→支付→发货→完成。
 */
const router = useRouter()
const userStore = useUserStore()
const canUpdate = computed(() => userStore.hasPermission('order:update'))

const loading = ref(false)
const submitting = ref(false)
const records = ref<OrderRow[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)

const query = reactive<OrderQuery>({
  pageNum: 1,
  pageSize: 10,
  orderNo: '',
  status: undefined,
  sortBy: 'createTime',
  order: 'desc'
})

const createVisible = ref(false)
const cancelVisible = ref(false)
const currentOrder = ref<OrderRow | null>(null)
const cancelReason = ref('')
const productOptions = ref<ProductItem[]>([])
const createFormRef = ref<FormInstance>()

const createForm = reactive({
  productId: '',
  quantity: 1,
  receiverName: '张三',
  receiverPhone: '13800000000',
  receiverAddress: '广东省深圳市南山区科技园',
  remark: ''
})

/** 同一次提交复用同一个幂等号 */
let currentRequestNo = generateRequestNo()

const createRules: FormRules = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  receiverName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  receiverAddress: [{ required: true, message: '请输入收货地址', trigger: 'blur' }]
}

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: OrderQuery = { ...query }
    if (dateRange.value?.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }
    const { data } = await fetchOrderPage(params)
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
  query.orderNo = ''
  query.status = undefined
  dateRange.value = null
  query.pageNum = 1
  loadData()
}

function goDetail(row: OrderRow): void {
  router.push(`/order/${row.orderNo}`)
}

async function openCreateDialog(): Promise<void> {
  createVisible.value = true
  currentRequestNo = generateRequestNo()
  const { data } = await fetchProductPage({ pageNum: 1, pageSize: 100, status: 1 })
  productOptions.value = data.records
}

async function submitCreate(): Promise<void> {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    const { data } = await createOrder({
      requestNo: currentRequestNo,
      receiverName: createForm.receiverName,
      receiverPhone: createForm.receiverPhone,
      receiverAddress: createForm.receiverAddress,
      remark: createForm.remark,
      items: [{ productId: createForm.productId, quantity: createForm.quantity }]
    })
    ElMessage.success(`下单成功：${data.orderNo}，应付 ￥${formatAmount(data.payAmount)}`)
    createVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

function handlePay(row: OrderRow): void {
  ElMessageBox.confirm(`确认支付订单 ${row.orderNo} 吗？`, '模拟支付', { type: 'info' })
    .then(async () => {
      await payOrder(row.orderNo)
      ElMessage.success('支付成功，库存已从锁定转为实际扣减')
      await loadData()
    })
    .catch(() => undefined)
}

function openCancel(row: OrderRow): void {
  currentOrder.value = row
  cancelReason.value = ''
  cancelVisible.value = true
}

async function submitCancel(): Promise<void> {
  if (!currentOrder.value) {
    return
  }
  submitting.value = true
  try {
    await cancelOrder(currentOrder.value.orderNo, cancelReason.value || '用户主动取消')
    ElMessage.success('订单已取消，锁定库存已释放')
    cancelVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

function handleStatus(row: OrderRow, targetStatus: number, action: string): void {
  ElMessageBox.confirm(`确认对订单 ${row.orderNo} 执行「${action}」操作吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await updateOrderStatus(row.orderNo, { targetStatus, remark: `后台${action}` })
      ElMessage.success(`${action}成功`)
      await loadData()
    })
    .catch(() => undefined)
}

onMounted(loadData)
</script>

<style scoped>
.tip {
  margin-left: 10px;
}

.amount {
  color: #f56c6c;
  font-weight: 600;
}

.sub-text {
  color: #909399;
  font-size: 12px;
}

.create-tip {
  margin-bottom: 14px;
}
</style>