<script setup>
/**
 * 操作日志（admin，只读）
 * AOP 采集的 @OperationLog 记录：模块/动作/操作人/IP/请求/结果/耗时
 */
defineOptions({ name: 'SystemLog' })

import { ref, onMounted } from 'vue'
import { listOperLogs } from '@/api/log'
import { usePagination } from '@/hooks/usePagination'

const dateRange = ref([])

const p = usePagination(
  (params) => {
    const [start, end] = dateRange.value || []
    return listOperLogs({
      ...params,
      startTime: start || undefined,
      endTime: end || undefined,
    })
  },
  { module: '', operatorName: '', resultCode: null },
)

onMounted(() => p.search())

// ---------- 参数查看 ----------
const paramsVisible = ref(false)
const paramsTitle = ref('')
const paramsJson = ref('')

function showParams(row) {
  paramsTitle.value = `${row.module} - ${row.action}`
  try {
    paramsJson.value = JSON.stringify(JSON.parse(row.params), null, 2)
  } catch {
    paramsJson.value = row.params || '（无参数）'
  }
  paramsVisible.value = true
}

function resultCodeType(code) {
  if (code === 200) return 'success'
  if (code === 403) return 'warning'
  return 'danger'
}
</script>

<template>
  <el-card shadow="never">
    <el-form inline @submit.prevent="p.search()">
      <el-form-item label="模块">
        <el-input v-model="p.query.module" placeholder="模块名" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item label="操作人">
        <el-input v-model="p.query.operatorName" placeholder="姓名/账号" clearable style="width: 140px" />
      </el-form-item>
      <el-form-item label="结果码">
        <el-select v-model="p.query.resultCode" placeholder="全部" clearable style="width: 110px">
          <el-option label="200 成功" :value="200" />
          <el-option label="400 校验失败" :value="400" />
          <el-option label="403 无权限" :value="403" />
          <el-option label="500 异常" :value="500" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 340px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="p.search()">查询</el-button>
        <el-button @click="dateRange = []; p.reset()">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="p.loading.value" :data="p.list.value" stripe>
      <el-table-column prop="module" label="模块" min-width="100" />
      <el-table-column prop="action" label="动作" min-width="110" />
      <el-table-column prop="operatorName" label="操作人" min-width="100" />
      <el-table-column prop="operatorIp" label="IP" min-width="120" />
      <el-table-column label="请求" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">{{ row.requestMethod }} {{ row.requestUri }}</template>
      </el-table-column>
      <el-table-column label="参数" width="70">
        <template #default="{ row }">
          <el-button v-if="row.params" link type="primary" @click="showParams(row)">查看</el-button>
          <span v-else style="color: var(--el-text-color-placeholder)">-</span>
        </template>
      </el-table-column>
      <el-table-column label="结果" width="90">
        <template #default="{ row }">
          <el-tag :type="resultCodeType(row.resultCode)" size="small" disable-transitions>
            {{ row.resultCode }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="耗时" width="90">
        <template #default="{ row }">{{ row.costMs != null ? `${row.costMs} ms` : '-' }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" min-width="160" />
    </el-table>

    <div class="page-card__footer">
      <el-pagination
        v-model:current-page="p.pageNum.value"
        v-model:page-size="p.pageSize.value"
        :total="p.total.value"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="p.onCurrentChange"
        @size-change="p.onSizeChange"
      />
    </div>
  </el-card>

  <el-dialog v-model="paramsVisible" :title="`请求参数 - ${paramsTitle}`" width="640px">
    <pre class="log-params">{{ paramsJson }}</pre>
    <template #footer>
      <el-button @click="paramsVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.log-params {
  margin: 0;
  max-height: 420px;
  overflow: auto;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
