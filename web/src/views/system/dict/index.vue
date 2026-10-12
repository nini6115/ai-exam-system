<script setup>
/**
 * 字典管理（admin）
 * 左右布局：左=字典类型（分页 CRUD），右=选中类型的字典项（分页 CRUD）
 * 增删改后调 dictStore.invalidate 使 DictSelect 缓存失效
 */
defineOptions({ name: 'SystemDict' })

import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageDictTypes, addDictType, updateDictType, deleteDictType,
  pageDictDatas, addDictData, updateDictData, deleteDictData,
} from '@/api/dict'
import { usePagination } from '@/hooks/usePagination'
import { useDictStore } from '@/store/dict'

const dictStore = useDictStore()

// ---------- 左：字典类型 ----------
const typeP = usePagination(pageDictTypes, { dictName: '', dictCode: '', status: null })

// ---------- 右：字典数据（依赖左侧选中类型） ----------
const selectedType = ref(null)
const dataP = usePagination((params) => pageDictDatas({ ...params, dictTypeCode: selectedType.value?.dictCode }))

watch(selectedType, () => {
  dataP.search()
})

onMounted(() => typeP.search())

// ---------- 类型 新增/编辑 ----------
const typeDialogVisible = ref(false)
const typeEditing = ref(null)
const typeSaving = ref(false)
const typeFormRef = ref()
const typeForm = reactive({ dictName: '', dictCode: '', description: '', status: 1 })

const typeRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictCode: [
    { required: true, message: '请输入字典编码', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]{1,49}$/, message: '2~50 位小写字母/数字/下划线，字母开头', trigger: 'blur' },
  ],
}

function openTypeAdd() {
  typeEditing.value = null
  Object.assign(typeForm, { dictName: '', dictCode: '', description: '', status: 1 })
  typeDialogVisible.value = true
}

function openTypeEdit(row) {
  typeEditing.value = row
  Object.assign(typeForm, {
    dictName: row.dictName,
    dictCode: row.dictCode,
    description: row.description || '',
    status: row.status,
  })
  typeDialogVisible.value = true
}

async function saveType() {
  await typeFormRef.value.validate()
  typeSaving.value = true
  try {
    if (typeEditing.value) {
      await updateDictType({ id: typeEditing.value.id, ...typeForm })
      ElMessage.success('修改成功')
    } else {
      await addDictType({ ...typeForm })
      ElMessage.success('新增成功')
    }
    if (typeEditing.value) dictStore.invalidate(typeEditing.value.dictCode)
    typeDialogVisible.value = false
    typeP.load()
  } finally {
    typeSaving.value = false
  }
}

async function deleteType(row) {
  await ElMessageBox.confirm(
    `确认删除字典类型「${row.dictName}」？其下所有字典项将一并删除。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '删除' },
  )
  await deleteDictType(row.id)
  dictStore.invalidate(row.dictCode)
  if (selectedType.value?.id === row.id) selectedType.value = null
  ElMessage.success('已删除')
  typeP.load()
}

// ---------- 数据 新增/编辑 ----------
const dataDialogVisible = ref(false)
const dataEditing = ref(null)
const dataSaving = ref(false)
const dataFormRef = ref()
const dataForm = reactive({ label: '', value: '', sortOrder: 0, remark: '', status: 1 })

const dataRules = {
  label: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  value: [{ required: true, message: '请输入键值', trigger: 'blur' }],
}

function openDataAdd() {
  dataEditing.value = null
  Object.assign(dataForm, { label: '', value: '', sortOrder: 0, remark: '', status: 1 })
  dataDialogVisible.value = true
}

function openDataEdit(row) {
  dataEditing.value = row
  Object.assign(dataForm, {
    label: row.label,
    value: row.value,
    sortOrder: row.sortOrder ?? 0,
    remark: row.remark || '',
    status: row.status,
  })
  dataDialogVisible.value = true
}

async function saveData() {
  await dataFormRef.value.validate()
  dataSaving.value = true
  try {
    if (dataEditing.value) {
      await updateDictData({ id: dataEditing.value.id, ...dataForm })
      ElMessage.success('修改成功')
    } else {
      await addDictData({ dictTypeCode: selectedType.value.dictCode, ...dataForm })
      ElMessage.success('新增成功')
    }
    dictStore.invalidate(selectedType.value.dictCode)
    dataDialogVisible.value = false
    dataP.load()
  } finally {
    dataSaving.value = false
  }
}

async function deleteData(row) {
  await ElMessageBox.confirm(`确认删除字典项「${row.label}」？`, '删除确认', { type: 'warning', confirmButtonText: '删除' })
  await deleteDictData(row.id)
  dictStore.invalidate(selectedType.value.dictCode)
  ElMessage.success('已删除')
  dataP.load()
}
</script>

<template>
  <el-row :gutter="16">
    <!-- 左：字典类型 -->
    <el-col :xs="24" :md="11">
      <el-card shadow="never" header="字典类型">
        <el-form inline @submit.prevent="typeP.search()">
          <el-form-item>
            <el-input v-model="typeP.query.dictName" placeholder="字典名称" clearable style="width: 120px" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="typeP.query.dictCode" placeholder="字典编码" clearable style="width: 120px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="typeP.search()">查询</el-button>
            <el-button @click="openTypeAdd">新增</el-button>
          </el-form-item>
        </el-form>

        <el-table
          v-loading="typeP.loading.value"
          :data="typeP.list.value"
          stripe
          highlight-current-row
          style="cursor: pointer"
          @row-click="(row) => (selectedType = row)"
        >
          <el-table-column prop="dictName" label="名称" min-width="100" />
          <el-table-column prop="dictCode" label="编码" min-width="120" />
          <el-table-column label="状态" width="70">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" disable-transitions>
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button link type="primary" @click.stop="openTypeEdit(row)">编辑</el-button>
                <el-button link type="danger" @click.stop="deleteType(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="page-card__footer">
          <el-pagination
            v-model:current-page="typeP.pageNum.value"
            v-model:page-size="typeP.pageSize.value"
            :total="typeP.total.value"
            layout="total, prev, pager, next"
            @current-change="typeP.onCurrentChange"
          />
        </div>
      </el-card>
    </el-col>

    <!-- 右：字典数据 -->
    <el-col :xs="24" :md="13">
      <el-card shadow="never">
        <template #header>
          <div style="display: flex; align-items: center">
            <span>字典项{{ selectedType ? ` - ${selectedType.dictName}` : '' }}</span>
            <el-button
              v-if="selectedType"
              type="primary"
              size="small"
              style="margin-left: auto"
              @click="openDataAdd"
            >
              新增字典项
            </el-button>
          </div>
        </template>

        <el-empty v-if="!selectedType" description="点击左侧字典类型查看字典项" />
        <template v-else>
          <el-form inline @submit.prevent="dataP.search()">
            <el-form-item>
              <el-input v-model="dataP.query.label" placeholder="标签" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="dataP.search()">查询</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="dataP.loading.value" :data="dataP.list.value" stripe>
            <el-table-column prop="label" label="标签" min-width="110" />
            <el-table-column prop="value" label="键值" min-width="90" />
            <el-table-column prop="sortOrder" label="排序" width="70" />
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
            <el-table-column label="状态" width="70">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" disable-transitions>
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button link type="primary" @click="openDataEdit(row)">编辑</el-button>
                  <el-button link type="danger" @click="deleteData(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <div class="page-card__footer">
            <el-pagination
              v-model:current-page="dataP.pageNum.value"
              v-model:page-size="dataP.pageSize.value"
              :total="dataP.total.value"
              layout="total, prev, pager, next"
              @current-change="dataP.onCurrentChange"
            />
          </div>
        </template>
      </el-card>
    </el-col>
  </el-row>

  <!-- 类型 新增/编辑 -->
  <el-dialog v-model="typeDialogVisible" :title="typeEditing ? '编辑字典类型' : '新增字典类型'" width="480px">
    <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
      <el-form-item label="字典名称" prop="dictName">
        <el-input v-model="typeForm.dictName" maxlength="50" />
      </el-form-item>
      <el-form-item label="字典编码" prop="dictCode">
        <el-input v-model="typeForm.dictCode" :disabled="!!typeEditing" placeholder="如 question_category" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="typeForm.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="typeForm.description" type="textarea" :rows="2" maxlength="200" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="typeDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="typeSaving" @click="saveType">保存</el-button>
    </template>
  </el-dialog>

  <!-- 字典项 新增/编辑 -->
  <el-dialog v-model="dataDialogVisible" :title="dataEditing ? '编辑字典项' : '新增字典项'" width="480px">
    <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="90px">
      <el-form-item label="标签" prop="label">
        <el-input v-model="dataForm.label" maxlength="50" />
      </el-form-item>
      <el-form-item label="键值" prop="value">
        <el-input v-model="dataForm.value" maxlength="50" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="dataForm.sortOrder" :min="0" :max="9999" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="dataForm.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="dataForm.remark" type="textarea" :rows="2" maxlength="200" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dataDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="dataSaving" @click="saveData">保存</el-button>
    </template>
  </el-dialog>
</template>
