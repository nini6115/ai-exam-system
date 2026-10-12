<script setup>
/**
 * 学生选择弹窗（发布考试用）：分页 + 关键字过滤，多选回显
 * 数据源 /user/list（roleCode=student）
 */
import { ref } from 'vue'
import { listUsers } from '@/api/user'
import { usePagination } from '@/hooks/usePagination'

const visible = ref(false)
const p = usePagination(listUsers, { username: '', realName: '', status: 1, roleCode: 'student' })

// 已选学生：{ userId, realName, userNo, username }
const selected = ref([])

function open(initial = []) {
  selected.value = [...initial]
  visible.value = true
  p.search()
}

function isSelected(row) {
  return selected.value.some((s) => s.userId === row.id)
}

function toggle(row, checked) {
  if (checked) {
    selected.value.push({ userId: row.id, realName: row.realName, userNo: row.userNo, username: row.username })
  } else {
    selected.value = selected.value.filter((s) => s.userId !== row.id)
  }
}

function removeAt(i) {
  selected.value.splice(i, 1)
}

function confirm() {
  emit('confirm', [...selected.value])
  visible.value = false
}

const emit = defineEmits(['confirm'])

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" title="选择考生" width="760px" :close-on-click-modal="false">
    <div style="display: flex; gap: 16px">
      <!-- 左：学生列表 -->
      <div style="flex: 1.4">
        <el-form inline @submit.prevent="p.search()">
          <el-form-item>
            <el-input v-model="p.query.username" placeholder="账号" clearable style="width: 110px" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="p.query.realName" placeholder="姓名" clearable style="width: 110px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="p.search()">查询</el-button>
          </el-form-item>
        </el-form>

        <el-table
          v-loading="p.loading.value"
          :data="p.list.value"
          stripe
          max-height="360"
          @row-click="(row) => toggle(row, !isSelected(row))"
        >
          <el-table-column width="46">
            <template #default="{ row }">
              <el-checkbox :model-value="isSelected(row)" @change="(val) => toggle(row, val)" @click.stop />
            </template>
          </el-table-column>
          <el-table-column prop="username" label="账号" min-width="100" />
          <el-table-column prop="realName" label="姓名" min-width="90" />
          <el-table-column prop="userNo" label="学号" min-width="90" />
        </el-table>
        <div class="page-card__footer">
          <el-pagination
            v-model:current-page="p.pageNum.value"
            v-model:page-size="p.pageSize.value"
            :total="p.total.value"
            layout="total, prev, pager, next"
            @current-change="p.onCurrentChange"
          />
        </div>
      </div>

      <!-- 右：已选回显 -->
      <div style="flex: 1; border-left: 1px solid var(--el-border-color-lighter); padding-left: 16px">
        <div style="margin-bottom: 8px; font-weight: 600">已选 {{ selected.length }} 人</div>
        <div style="max-height: 400px; overflow: auto">
          <div
            v-for="(s, i) in selected"
            :key="s.userId"
            style="display: flex; align-items: center; gap: 8px; padding: 4px 0"
          >
            <span class="text-ellipsis" style="flex: 1">
              {{ s.realName }}
              <span style="color: var(--el-text-color-secondary); font-size: 12px">{{ s.userNo || s.username }}</span>
            </span>
            <el-button link type="danger" size="small" @click="removeAt(i)">移除</el-button>
          </div>
          <el-empty v-if="!selected.length" description="尚未选择考生" :image-size="60" />
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="confirm">确定（{{ selected.length }}）</el-button>
    </template>
  </el-dialog>
</template>
