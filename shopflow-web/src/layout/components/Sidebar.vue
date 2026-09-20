<template>
  <el-menu
    :default-active="activeMenu"
    :collapse="appStore.sidebarCollapsed"
    :collapse-transition="false"
    background-color="#304156"
    text-color="#bfcbd9"
    active-text-color="#409eff"
    @select="handleSelect"
  >
    <el-menu-item v-for="menu in permissionStore.menus" :key="menu.path" :index="menu.path">
      <el-icon>
        <component :is="menu.icon || 'Menu'" />
      </el-icon>
      <template #title>{{ menu.title }}</template>
    </el-menu-item>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { usePermissionStore } from '@/stores/permission'

/**
 * 侧边栏菜单。
 *
 * 菜单项来自权限 store（由路由表 + 当前用户权限推导），
 * 因此不会出现"菜单能点、点进去接口返回 403"的尴尬情况。
 */
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const permissionStore = usePermissionStore()

const activeMenu = computed(() => route.path)

function handleSelect(path: string): void {
  if (path !== route.path) {
    router.push(path)
  }
}
</script>

<style scoped>
:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item.is-active) {
  background-color: #263445;
}
</style>