<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col v-for="card in statCards" :key="card.label" :xs="12" :sm="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-body">
            <div class="stat-icon" :style="{ backgroundColor: card.color }">
              <el-icon :size="22"><component :is="card.icon" /></el-icon>
            </div>
            <div>
              <div class="stat-label">{{ card.label }}</div>
              <div class="stat-value">{{ card.value }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="chart-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">订单量与成交额趋势</span>
          <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="15">近 15 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendRef" class="chart trend-chart"></div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :md="14">
        <el-card class="chart-card" shadow="never">
          <template #header><span class="card-title">热销商品 Top 10（按成交件数）</span></template>
          <div ref="topRef" class="chart top-chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card class="chart-card" shadow="never">
          <template #header><span class="card-title">待处理事项</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="待支付订单">
              <el-tag type="warning" effect="plain">{{ overview?.pendingPaymentCount ?? 0 }} 笔</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="累计成交额">
              {{ formatAmount(overview?.totalAmount) }} 元
            </el-descriptions-item>
            <el-descriptions-item label="今日成交额">
              {{ formatAmount(overview?.todayAmount) }} 元
            </el-descriptions-item>
            <el-descriptions-item label="统计口径">
              已支付及之后的订单（不含已取消）
            </el-descriptions-item>
          </el-descriptions>
          <el-alert
            class="hint"
            type="info"
            :closable="false"
            title="看板数据缓存 60 秒，允许秒级延迟以降低数据库压力"
            show-icon
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { fetchOverview, fetchTopProducts, fetchTrend } from '@/api/dashboard'
import type { DashboardOverview, TopProduct, TrendPoint } from '@/types'
import { formatAmount } from '@/utils/format'

/**
 * 数据看板。
 *
 * 图表实例保存在组件作用域内，卸载时统一 dispose 并移除 resize 监听，
 * 避免路由切换后残留定时与事件监听（这类泄漏在后台系统里很常见）。
 */
const overview = ref<DashboardOverview | null>(null)
const trendDays = ref(7)
const trendRef = ref<HTMLDivElement | null>(null)
const topRef = ref<HTMLDivElement | null>(null)

let trendChart: echarts.ECharts | null = null
let topChart: echarts.ECharts | null = null

const statCards = computed(() => [
  {
    label: '用户总数',
    value: overview.value?.userCount ?? 0,
    icon: 'User',
    color: '#409eff'
  },
  {
    label: '商品总数',
    value: overview.value?.productCount ?? 0,
    icon: 'Goods',
    color: '#67c23a'
  },
  {
    label: '订单总数',
    value: overview.value?.orderCount ?? 0,
    icon: 'Tickets',
    color: '#e6a23c'
  },
  {
    label: '今日成交额',
    value: formatAmount(overview.value?.todayAmount),
    icon: 'Money',
    color: '#f56c6c'
  }
])

async function loadOverview(): Promise<void> {
  const { data } = await fetchOverview()
  overview.value = data
}

async function loadTrend(): Promise<void> {
  const { data } = await fetchTrend(trendDays.value)
  renderTrend(data)
}

async function loadTopProducts(): Promise<void> {
  const { data } = await fetchTopProducts(10)
  renderTopProducts(data)
}

function renderTrend(points: TrendPoint[]): void {
  if (!trendRef.value) {
    return
  }
  if (!trendChart) {
    trendChart = echarts.init(trendRef.value)
  }
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['订单数', '成交额'], right: 10 },
    grid: { left: 50, right: 60, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      boundaryGap: true,
      data: points.map((point) => point.date.slice(5))
    },
    yAxis: [
      { type: 'value', name: '订单数', minInterval: 1 },
      { type: 'value', name: '成交额(元)' }
    ],
    series: [
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        areaStyle: { opacity: 0.12 },
        itemStyle: { color: '#409eff' },
        data: points.map((point) => point.orderCount)
      },
      {
        name: '成交额',
        type: 'bar',
        yAxisIndex: 1,
        barWidth: 16,
        itemStyle: { color: '#67c23a' },
        data: points.map((point) => point.amount)
      }
    ]
  })
}

function renderTopProducts(products: TopProduct[]): void {
  if (!topRef.value) {
    return
  }
  if (!topChart) {
    topChart = echarts.init(topRef.value)
  }
  // ECharts 的类目轴从下往上渲染，这里反转一下让第一名显示在最上方
  const ordered = [...products].reverse()
  topChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 150, right: 30, top: 10, bottom: 30 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: {
      type: 'category',
      data: ordered.map((item) => item.productName)
    },
    series: [
      {
        name: '成交件数',
        type: 'bar',
        barWidth: 14,
        itemStyle: { color: '#409eff', borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right' },
        data: ordered.map((item) => item.quantity)
      }
    ]
  })
}

function handleResize(): void {
  trendChart?.resize()
  topChart?.resize()
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await Promise.all([loadOverview(), loadTrend(), loadTopProducts()])
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  topChart?.dispose()
  trendChart = null
  topChart = null
})
</script>

<style scoped>
.stat-card {
  margin-bottom: 16px;
}

.stat-body {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 10px;
  color: #fff;
}

.chart-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chart {
  width: 100%;
}

.trend-chart {
  height: 320px;
}

.top-chart {
  height: 360px;
}

.hint {
  margin-top: 12px;
}
</style>