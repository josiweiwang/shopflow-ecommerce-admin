<template>
  <div class="pagination-wrapper">
    <el-pagination
      :current-page="page"
      :page-size="limit"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 分页组件。
 *
 * 统一封装分页器的交互细节：切换每页条数时回到第 1 页，
 * 避免出现"在第 5 页把条数改成 100，结果请求了一个不存在的页码"。
 */
defineProps<{
  page: number
  limit: number
  total: number
}>()

const emit = defineEmits<{
  (e: 'update:page', value: number): void
  (e: 'update:limit', value: number): void
  (e: 'change'): void
}>()

function handlePageChange(value: number): void {
  emit('update:page', value)
  emit('change')
}

function handleSizeChange(value: number): void {
  emit('update:limit', value)
  emit('update:page', 1)
  emit('change')
}
</script>