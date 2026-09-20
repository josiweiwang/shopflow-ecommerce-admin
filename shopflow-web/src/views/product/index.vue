<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="商品名称 / SKU" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-cascader
            v-model="query.categoryId"
            :options="categoryOptions"
            :props="{ emitPath: false }"
            placeholder="全部分类"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="item in PRODUCT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格区间">
          <el-input-number v-model="query.minPrice" :min="0" :controls="false" placeholder="最低价" style="width: 110px" />
          <span class="price-separator">-</span>
          <el-input-number v-model="query.maxPrice" :min="0" :controls="false" placeholder="最高价" style="width: 110px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="table-toolbar">
        <span class="card-title">商品列表</span>
        <div>
          <el-button v-if="userStore.hasPermission('product:create')" type="primary" :icon="Plus" @click="openCreate">
            新增商品
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="records" border stripe>
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div class="product-cell">
              <div class="product-name">{{ row.name }}</div>
              <div class="product-sku">SKU：{{ row.sku }}</div>
              <div v-if="row.subtitle" class="product-subtitle">{{ row.subtitle }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column label="价格" width="140">
          <template #default="{ row }">
            <div class="price">￥{{ formatAmount(row.price) }}</div>
            <div v-if="row.originalPrice > row.price" class="original-price">￥{{ formatAmount(row.originalPrice) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="150">
          <template #default="{ row }">
            <div>
              可用
              <span :class="{ 'low-stock': row.lowStock }">{{ row.availableStock ?? 0 }}</span>
              / 锁定 {{ row.lockedStock ?? 0 }}
            </div>
            <el-tag v-if="row.lowStock" type="danger" size="small" effect="plain">低库存</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :disabled="!userStore.hasPermission('product:update')"
              :before-change="() => toggleStatus(row)"
            />
            <div class="status-text">{{ row.statusDesc }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="userStore.hasPermission('product:update')" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="userStore.hasPermission('product:delete')" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        v-model:page="query.pageNum!"
        v-model:limit="query.pageSize!"
        :total="total"
        @change="loadData"
      />
    </el-card>

    <ProductForm
      v-model:visible="dialogVisible"
      :product-id="editingId"
      :category-options="categoryOptions"
      @success="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination.vue'
import ProductForm from './components/ProductForm.vue'
import { fetchCategoryTree } from '@/api/category'
import { deleteProduct, fetchProductPage, updateProductStatus } from '@/api/product'
import { useUserStore } from '@/stores/user'
import type { CategoryNode, CategoryOption, ProductItem, ProductQuery } from '@/types'
import { PRODUCT_STATUS_OPTIONS, formatAmount } from '@/utils/format'

/**
 * 商品列表。
 *
 * 排序字段由后端做白名单控制（price / sales / createTime），
 * 前端只传 sortBy + order，不需要也不允许拼接 SQL 片段。
 */
const userStore = useUserStore()

const loading = ref(false)
const records = ref<ProductItem[]>([])
const total = ref(0)
const categoryOptions = ref<CategoryOption[]>([])
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)

const query = reactive<ProductQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  categoryId: undefined,
  status: undefined,
  minPrice: undefined,
  maxPrice: undefined,
  sortBy: 'createTime',
  order: 'desc'
})

function toCascaderOptions(nodes: CategoryNode[]): CategoryOption[] {
  return nodes.map((node) => ({
    value: node.id,
    label: node.name,
    children: node.children?.length ? toCascaderOptions(node.children) : undefined
  }))
}

async function loadCategories(): Promise<void> {
  const { data } = await fetchCategoryTree()
  categoryOptions.value = toCascaderOptions(data)
}

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const { data } = await fetchProductPage({ ...query })
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
  query.categoryId = undefined
  query.status = undefined
  query.minPrice = undefined
  query.maxPrice = undefined
  query.pageNum = 1
  loadData()
}

function openCreate(): void {
  editingId.value = null
  dialogVisible.value = true
}

function openEdit(row: ProductItem): void {
  editingId.value = row.id
  dialogVisible.value = true
}

async function toggleStatus(row: ProductItem): Promise<boolean> {
  const targetStatus = row.status === 1 ? 0 : 1
  try {
    await updateProductStatus(row.id, targetStatus)
    ElMessage.success(targetStatus === 1 ? '商品已上架' : '商品已下架')
    await loadData()
    return true
  } catch {
    return false
  }
}

function handleDelete(row: ProductItem): void {
  ElMessageBox.confirm(
    `确定删除商品「${row.name}」吗？删除后历史订单不受影响。`,
    '提示',
    { type: 'warning' }
  )
    .then(async () => {
      await deleteProduct(row.id)
      ElMessage.success('删除成功')
      await loadData()
    })
    .catch(() => undefined)
}

onMounted(async () => {
  await Promise.all([loadCategories(), loadData()])
})
</script>

<style scoped>
.price-separator {
  margin: 0 6px;
  color: #909399;
}

.product-cell {
  line-height: 1.5;
}

.product-name {
  font-weight: 500;
}

.product-sku,
.product-subtitle {
  color: #909399;
  font-size: 12px;
}

.price {
  color: #f56c6c;
  font-weight: 600;
}

.original-price {
  color: #c0c4cc;
  font-size: 12px;
  text-decoration: line-through;
}

.low-stock {
  color: #f56c6c;
  font-weight: 700;
}

.status-text {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
</style>