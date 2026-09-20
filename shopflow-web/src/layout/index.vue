<template>
  <el-container class="layout">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '210px'" class="layout-aside">
      <div class="layout-logo">
        <span class="logo-mark">SF</span>
        <span v-show="!appStore.sidebarCollapsed" class="logo-text">ShopFlow</span>
      </div>
      <Sidebar />
    </el-aside>

    <el-container class="layout-body">
      <el-header class="layout-header">
        <Navbar />
      </el-header>
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import Navbar from './components/Navbar.vue'
import Sidebar from './components/Sidebar.vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'

/**
 * 后台整体布局：左侧菜单 + 顶部导航 + 内容区。
 */
const appStore = useAppStore()
const userStore = useUserStore()

onMounted(() => {
  // 刷新页面后内存中的用户信息会丢失，这里补一次，避免菜单为空
  if (!userStore.userInfo) {
    userStore.loadUserInfo().catch(() => userStore.reset())
  }
})
</script>

<style scoped>
.layout {
  height: 100vh;
}

.layout-aside {
  background-color: #304156;
  transition: width 0.2s;
  overflow-x: hidden;
}

.layout-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 56px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
  border-bottom: 1px solid #1f2d3d;
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 6px;
  background: linear-gradient(135deg, #409eff, #36cfc9);
  font-size: 13px;
  margin-right: 8px;
}

.layout-body {
  background-color: #f0f2f5;
}

.layout-header {
  height: 56px;
  padding: 0 16px;
  background-color: #fff;
  box-shadow: 0 1px 4px rgb(0 21 41 / 8%);
  display: flex;
  align-items: center;
}

.layout-main {
  padding: 0;
  overflow-y: auto;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.2s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-8px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(8px);
}
</style>