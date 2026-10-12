<script setup>
/**
 * 角色管理（admin）
 * 角色全量列表 + CRUD + 查看角色下用户（编辑时 roleCode 只读，后端不可改）
 */
defineOptions({ name: 'SystemRole' })

import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRoles, listRoleUsers, addRole, updateRole, deleteRole } from '@/api/role'
import { usePagination } from '@/hooks/usePagination'
import { GENDER } from '@/constants'

// ---------- 角色列表（全量） ----------
const loading = ref(false)
const roles = ref([])

async function load() {
  loading.value = true
  try {
    roles.value = (await listRoles()) || []
  } finally {
    loading.value = false
  }
}

onMounted(load)

// ---------- 新增/编辑 ----------
const dialogVisible = ref(false)
const editing = ref(null)
const saving = ref(false)
const formRef = ref()
const form = reactive({ roleCode: '', roleName: '', description: '' })

const rules = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]{1,29}$/, message: '2~30 位小写字母/数字/下划线，字母开头', trigger: 'blur' },
  ],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

function openAdd() {
  editing.value = null
  Object.assign(form, { roleCode: '', roleName: '', description: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  Object.assign(form, { roleCode: row.roleCode, roleName: row.roleName, description: row.description || '' })
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editing.value) {
      await updateRole({ id: editing.value.id, roleName: form.roleName, description: form.description })
      ElMessage.success('修改成功')
    } else {
      await addRole({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function doDelete(row) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
  })
  await deleteRole(row.id)
  ElMessage.success('已删除')
  load()
}

// ---------- 查看角色下用户 ----------
const usersVisible = ref(false)
const currentRole = ref(null)
const roleUsers = usePagination((params) => listRoleUsers(currentRole.value?.id, params))

function openUsers(row) {
  currentRole.value = row
  usersVisible.value = true
  roleUsers.search()
}
</script>

<template>
  <div>
    <el-card shadow="never">
      <div style="display: flex; margin-bottom: 12px">
        <span style="color: var(--el-text-color-secondary); font-size: 13px; line-height: 32px">
          系统按角色控制权限：admin 管理员 / teacher 教师 / student 学生
        </span>
        <div style="margin-left: auto">
          <el-button type="primary" @click="openAdd">新增角色</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="roles" stripe>
        <el-table-column prop="roleCode" label="角色编码" min-width="120" />
        <el-table-column prop="roleName" label="角色名称" min-width="120" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="openUsers(row)">查看用户</el-button>
              <el-button link type="danger" @click="doDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑角色' : '新增角色'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="!!editing" placeholder="如 teacher" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" maxlength="30" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 角色下用户 -->
    <el-dialog v-model="usersVisible" :title="`角色用户 - ${currentRole?.roleName || ''}`" width="640px">
      <el-table v-loading="roleUsers.loading.value" :data="roleUsers.list.value" stripe>
        <el-table-column prop="username" label="账号" min-width="110" />
        <el-table-column prop="realName" label="姓名" min-width="90" />
        <el-table-column prop="userNo" label="学号/工号" min-width="100" />
        <el-table-column label="性别" width="70">
          <template #default="{ row }">{{ GENDER[row.gender] ?? '未知' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">{{ row.status === 1 ? '正常' : '禁用' }}</template>
        </el-table-column>
      </el-table>
      <div class="page-card__footer">
        <el-pagination
          v-model:current-page="roleUsers.pageNum.value"
          v-model:page-size="roleUsers.pageSize.value"
          :total="roleUsers.total.value"
          layout="total, prev, pager, next"
          @current-change="roleUsers.onCurrentChange"
        />
      </div>
    </el-dialog>
  </div>
</template>
