<template>
  <el-tag :type="type" effect="light" size="small">{{ text }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { orderStatusTagType } from '@/utils/format'

/**
 * 状态标签。
 *
 * 通过 scene 区分业务场景，颜色映射统一放在 utils/format 里，
 * 各页面不需要再各自写一遍 switch。
 */
const props = defineProps<{
  /** 业务场景：order-订单状态 */
  scene: 'order'
  /** 状态码 */
  status: number
  /** 后端返回的状态描述，优先级高于内置映射 */
  text?: string
}>()

const ORDER_TEXT: Record<number, string> = {
  0: '待支付',
  1: '已支付',
  2: '配送中',
  3: '已完成',
  4: '已取消'
}

const type = computed(() => orderStatusTagType(props.status))

const text = computed(() => props.text || ORDER_TEXT[props.status] || '未知')
</script>