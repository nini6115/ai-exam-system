<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import AppMain from './components/AppMain.vue'

const route = useRoute()
const collapsed = ref(false)

const activeMenu = computed(() => route.path)
</script>

<template>
  <el-container class="app-layout">
    <el-aside :width="collapsed ? '64px' : '210px'" class="app-aside">
      <div class="app-logo">
        <el-icon :size="24"><Monitor /></el-icon>
        <span v-show="!collapsed" class="app-logo__text">AI在线考试系统</span>
      </div>
      <Sidebar :collapsed="collapsed" :active-menu="activeMenu" />
    </el-aside>

    <el-container>
      <el-header class="app-header" height="56px">
        <Navbar :collapsed="collapsed" @toggle="collapsed = !collapsed" />
      </el-header>
      <AppMain />
    </el-container>
  </el-container>
</template>

<style lang="scss" scoped>
.app-layout {
  height: 100vh;
}

.app-aside {
  display: flex;
  flex-direction: column;
  background-color: #001529;
  transition: width 0.2s;
  overflow: hidden;
}

.app-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 56px;
  color: #fff;
  flex-shrink: 0;

  &__text {
    font-size: 15px;
    font-weight: 600;
    white-space: nowrap;
  }
}

.app-header {
  padding: 0;
  background: #fff;
  border-bottom: 1px solid var(--el-border-color-light);
}
</style>
