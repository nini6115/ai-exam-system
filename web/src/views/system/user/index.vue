<script setup>
/**
 * 用户管理（admin）
 * 用户 CRUD + 启用/禁用 + 重置密码 + 分配角色
 */
defineOptions({ name: 'SystemUser' })

import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, getUser, addUser, updateUser, deleteUser, resetPassword, updateStatus, assignRole } from '@/api/user'
import { listRoles } from '@/api/role'
import { usePagination } from '@/hooks/usePagination'
import { GENDER } from '@/constants'

// ---------- 列表 ----------
const roleOptions = ref([])
const p = usePagination(listUsers, { username: '', realName: '', status: null, roleCode: '' })

async function loadRoles() {
  try {
    roleOptions.value = (await listRoles()) || []
  } catch {
    /* 拦截器已提示 */
  }
}

onMounted(() => {
  p.search()
  loadRoles()
})

// ---------- 新增/编辑 ----------
const dialogVisible = ref(false)
const editing = ref(null) // null=新增
const saving = ref(false)
const formRef = ref()
const form = reactive({
  username: '',
  password: '',
  realName: '',
  userNo: '',
  gender: 0,
  phone: '',
  email: '',
  roleIds: [],
})

const rules = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,50}$/, message: '3~50 位字母/数字/下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6~32 位', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
}

function openAdd() {
  editing.value = null
  Object.assign(form, { username: '', password: '', realName: '', userNo: '', gender: 0, phone: '', email: '', roleIds: [] })
  dialogVisible.value = true
}

async function openEdit(row) {
  editing.value = row
  const detail = await getUser(row.id)
  Object.assign(form, {
    username: detail.username,
    password: '',
    realName: detail.realName || '',
    userNo: detail.userNo || '',
    gender: detail.gender ?? 0,
    phone: detail.phone || '',
    email: detail.email || '',
    roleIds: (detail.roles || []).map((r) => r.id),
  })
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editing.value) {
      const { username, password, ...payload } = form
      await updateUser({ id: editing.value.id, ...payload })
      ElMessage.success('修改成功')
    } else {
      await addUser({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    p.load()
  } finally {
    saving.value = false
  }
}

// ---------- 启用/禁用（switch before-change 带确认） ----------
async function beforeStatusChange(row) {
  const target = row.status === 1 ? 0 : 1
  const tip = target === 0 ? `禁用后账号「${row.username}」将无法登录，确认禁用？` : `确认启用账号「${row.username}」？`
  await ElMessageBox.confirm(tip, '提示', { type: 'warning' })
  await updateStatus({ userId: row.id, status: target })
  ElMessage.success(target === 1 ? '已启用' : '已禁用')
  return true
}

// ---------- 重置密码 ----------
const resetVisible = ref(false)
const resetTarget = ref(null)
const resetFormRef = ref()
const resetForm = reactive({ newPassword: '' })
const resetRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6~32 位', trigger: 'blur' },
  ],
}

function openReset(row) {
  resetTarget.value = row
  resetForm.newPassword = ''
  resetVisible.value = true
}

async function doReset() {
  await resetFormRef.value.validate()
  await resetPassword({ userId: resetTarget.value.id, newPassword: resetForm.newPassword })
  ElMessage.success('密码已重置')
  resetVisible.value = false
}

// ---------- 分配角色 ----------
const roleVisible = ref(false)
const roleTarget = ref(null)
const roleForm = reactive({ roleIds: [] })

function openAssign(row) {
  roleTarget.value = row
  roleForm.roleIds = (row.roles || []).map((r) => r.id)
  roleVisible.value = true
}

async function doAssign() {
  await assignRole({ userId: roleTarget.value.id, roleIds: roleForm.roleIds })
  ElMessage.success('角色已更新')
  roleVisible.value = false
  p.load()
}

// ---------- 删除 ----------
async function doDelete(row) {
  await ElMessageBox.confirm(`确认删除用户「${row.realName}（${row.username}）」？删除后不可恢复。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
  })
  await deleteUser(row.id)
  ElMessage.success('已删除')
  p.load()
}
</script>

<template>
  <div>
    <el-card shadow="never">
      <el-form inline @submit.prevent="p.search()">
        <el-form-item label="账号">
          <el-input v-model="p.query.username" placeholder="登录账号" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="p.query.realName" placeholder="真实姓名" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="p.query.status" placeholder="全部" clearable style="width: 100px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="p.query.roleCode" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.roleCode" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="p.search()">查询</el-button>
          <el-button @click="p.reset()">重置</el-button>
          <el-button type="primary" @click="openAdd">新增用户</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="p.loading.value" :data="p.list.value" stripe>
        <el-table-column prop="username" label="账号" min-width="110" />
        <el-table-column prop="realName" label="姓名" min-width="90" />
        <el-table-column prop="userNo" label="学号/工号" min-width="100" />
        <el-table-column label="性别" width="70">
          <template #default="{ row }">{{ GENDER[row.gender] ?? '未知' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="110" />
        <el-table-column label="角色" min-width="140">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r.id" size="small" style="margin-right: 4px">
              {{ r.roleName }}
            </el-tag>
            <span v-if="!row.roles?.length" style="color: var(--el-text-color-placeholder)">未分配</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              :before-change="() => beforeStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" min-width="150" />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="openAssign(row)">分配角色</el-button>
              <el-button link type="warning" @click="openReset(row)">重置密码</el-button>
              <el-button link type="danger" @click="doDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
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

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑用户' : '新增用户'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" :disabled="!!editing" placeholder="字母/数字/下划线" />
        </el-form-item>
        <el-form-item v-if="!editing" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6~32 位" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" maxlength="50" />
        </el-form-item>
        <el-form-item label="学号/工号">
          <el-input v-model="form.userNo" maxlength="30" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="0">未知</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" maxlength="100" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width: 100%" placeholder="可多选">
            <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="420px">
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="90px">
        <el-form-item label="账号">{{ resetTarget?.username }}</el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="6~32 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="doReset">确认重置</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog v-model="roleVisible" title="分配角色" width="420px">
      <el-checkbox-group v-model="roleForm.roleIds">
        <el-checkbox v-for="r in roleOptions" :key="r.id" :value="r.id">
          {{ r.roleName }}（{{ r.roleCode }}）
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" @click="doAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
