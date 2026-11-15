<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="用户名/姓名" clearable style="width: 220px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="primary" @click="openForm()">新增用户</el-button>
    </div>
    <el-table :data="records" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="displayName" label="姓名" width="140" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button link type="primary" @click="openAssignRoles(row)">分配角色</el-button>
          <el-button link type="warning" @click="toggleStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
          <el-button link type="danger" @click="resetPassword(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="load"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item v-if="!form.id" label="密码"><el-input v-model="form.passwordHash" type="password" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.displayName" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="400px">
      <el-select v-model="selectedRoleIds" multiple placeholder="选择角色" style="width: 100%">
        <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
      </el-select>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userList, userCreate, userUpdate, userStatus, userResetPassword, userRoles, userAssignRoles, roleList } from '@/api/system'

const records = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10, keyword: '' })
const dialogVisible = ref(false)
const form = reactive<any>({})
const roles = ref<any[]>([])
const roleDialogVisible = ref(false)
const selectedRoleIds = ref<number[]>([])
const currentUserId = ref<number>()

async function load() {
  const data: any = await userList(query)
  records.value = data.records
  total.value = data.total
}

function openForm(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

async function save() {
  if (form.id) {
    await userUpdate(form)
  } else {
    await userCreate(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function toggleStatus(row: any) {
  await userStatus(row.id, row.status === 1 ? 0 : 1)
  load()
}

async function resetPassword(row: any) {
  const { value } = await ElMessageBox.prompt('输入新密码（至少6位）', '重置密码', { inputType: 'password' })
  if (value) {
    await userResetPassword(row.id, value)
    ElMessage.success('密码已重置')
  }
}

async function openAssignRoles(row: any) {
  currentUserId.value = row.id
  roles.value = (await roleList()) as any[]
  selectedRoleIds.value = (await userRoles(row.id)) as number[]
  roleDialogVisible.value = true
}

async function saveRoles() {
  await userAssignRoles(currentUserId.value!, selectedRoleIds.value)
  ElMessage.success('角色已保存')
  roleDialogVisible.value = false
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}
</style>
