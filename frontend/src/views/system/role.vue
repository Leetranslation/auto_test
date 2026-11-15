<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="openForm()">新增角色</el-button>
    </div>
    <el-table :data="records" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="roleCode" label="角色编码" width="150" />
      <el-table-column prop="roleName" label="角色名称" width="160" />
      <el-table-column prop="scopeType" label="作用域" width="110" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button link type="primary" @click="openAssign(row)">授权</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="460px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="角色编码"><el-input v-model="form.roleCode" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="角色名称"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="作用域">
          <el-select v-model="form.scopeType">
            <el-option label="平台级" value="PLATFORM" />
            <el-option label="项目级" value="PROJECT" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" title="分配权限" width="480px">
      <el-tree ref="treeRef" :data="permTree" show-checkbox node-key="id" :props="{ label: 'permissionName', children: 'children' }" default-expand-all />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { roleList, roleCreate, roleUpdate, roleAssign, rolePermissions, permissionList } from '@/api/system'

const records = ref<any[]>([])
const dialogVisible = ref(false)
const form = reactive<any>({})
const assignVisible = ref(false)
const permTree = ref<any[]>([])
const treeRef = ref()
const currentRoleId = ref<number>()

async function load() {
  records.value = (await roleList()) as any[]
}

function openForm(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, row)
  else Object.assign(form, { scopeType: 'PROJECT' })
  dialogVisible.value = true
}

async function save() {
  if (form.id) {
    await roleUpdate(form)
  } else {
    await roleCreate(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function openAssign(row: any) {
  currentRoleId.value = row.id
  const all: any[] = (await permissionList()) as any[]
  permTree.value = buildTree(all)
  const checked: number[] = (await rolePermissions(row.id)) as number[]
  assignVisible.value = true
  // 勾选已有权限
  setTimeout(() => {
    const leafIds = checked.filter((id) => !all.some((p) => p.id === id && all.some((x) => x.id !== id && false)))
    treeRef.value?.setCheckedKeys(checked)
  }, 100)
}

function buildTree(list: any[]) {
  return list.map((p) => ({ ...p, children: [] }))
}

async function saveAssign() {
  const checked = treeRef.value.getCheckedKeys().concat(treeRef.value.getHalfCheckedKeys())
  await roleAssign(currentRoleId.value!, checked)
  ElMessage.success('权限已保存')
  assignVisible.value = false
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
