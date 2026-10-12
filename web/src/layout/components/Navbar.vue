<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

defineProps({
  collapsed: { type: Boolean, default: false },
})
const emit = defineEmits(['toggle'])

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 面包屑：取匹配路由链的 title（排除布局本身） */
const breadcrumbs = computed(() =>
  route.matched.filter((r) => r.meta?.title && r.name !== 'Layout').map((r) => ({ title: r.meta.title })),
)

async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    } catch {
      return
    }
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.replace('/login')
  }
}
</script>

<template>
  <div class="navbar">
    <div class="navbar__left">
      <el-icon class="navbar__toggle" :size="18" @click="emit('toggle')">
        <Expand v-if="collapsed" />
        <Fold v-else />
      </el-icon>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.title">
          {{ item.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <el-dropdown trigger="click" @command="handleCommand">
      <span class="navbar__user">
        <el-avatar :size="30" :src="userStore.avatar || undefined">
          {{ userStore.displayName.slice(0, 1) }}
        </el-avatar>
        <span class="navbar__username">{{ userStore.displayName }}</span>
        <el-icon><ArrowDown /></el-icon>
      </span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="profile">
            <el-icon><User /></el-icon>个人中心
          </el-dropdown-item>
          <el-dropdown-item command="logout" divided>
            <el-icon><SwitchButton /></el-icon>退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<style lang="scss" scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 16px;

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__toggle {
    cursor: pointer;
    color: var(--el-text-color-primary);
  }

  &__user {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    outline: none;
  }

  &__username {
    font-size: 14px;
    color: var(--el-text-color-primary);
  }
}
</style>
