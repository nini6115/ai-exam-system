<script setup>
/**
 * 主内容区：keep-alive 按 meta.keepAlive 缓存列表页（返回不丢查询状态）。
 * include 按组件 name 匹配，因此各列表页组件必须显式声明 name 与路由 name 一致。
 */
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const keepAliveNames = computed(() =>
  route.matched.flatMap((r) => (r.meta?.keepAlive && r.name ? [String(r.name)] : [])),
)
</script>

<template>
  <el-main class="app-main">
    <router-view v-slot="{ Component }">
      <keep-alive :include="keepAliveNames">
        <component :is="Component" :key="route.fullPath" />
      </keep-alive>
    </router-view>
  </el-main>
</template>

<style lang="scss" scoped>
.app-main {
  background-color: #f0f2f5;
  padding: 16px;
  overflow-y: auto;
}
</style>
