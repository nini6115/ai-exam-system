<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/store/user'
import { menus, filterMenus } from '../menu'

const props = defineProps({
  collapsed: { type: Boolean, default: false },
  activeMenu: { type: String, default: '' },
})

const userStore = useUserStore()
const visibleMenus = computed(() => filterMenus(menus, userStore.roleCodes))
</script>

<template>
  <el-menu
    :default-active="props.activeMenu"
    :collapse="props.collapsed"
    :collapse-transition="false"
    router
    background-color="#001529"
    text-color="rgba(255,255,255,0.68)"
    active-text-color="#fff"
    class="app-sidebar"
  >
    <template v-for="item in visibleMenus" :key="item.path || item.title">
      <!-- 有子菜单 -->
      <el-sub-menu v-if="item.children" :index="item.title">
        <template #title>
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </template>
        <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
          {{ child.title }}
        </el-menu-item>
      </el-sub-menu>

      <!-- 无子菜单 -->
      <el-menu-item v-else :index="item.path">
        <el-icon><component :is="item.icon" /></el-icon>
        <template #title>{{ item.title }}</template>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<style lang="scss" scoped>
.app-sidebar {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;

  &:not(.el-menu--collapse) {
    width: 210px;
  }

  :deep(.el-menu-item.is-active) {
    background-color: var(--el-color-primary);
  }
}
</style>
