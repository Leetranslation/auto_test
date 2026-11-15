<template>
  <div>
    <div class="toolbar">
      <el-select v-model="projectId" placeholder="选择项目" style="width: 200px" @change="load">
        <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
      </el-select>
      <el-button type="primary" @click="openForm()">新建环境</el-button>
    </div>
    <el-table :data="records" border stripe>
      <el-table-column prop="envCode" label="编码" width="130" />
      <el-table-column prop="envName" label="名称" width="140" />
      <el-table-column prop="baseUrl" label="Base URL" min-width="220" />
      <el-table-column prop="envLevel" label="级别" width="90" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button link type="primary" @click="openVars(row)">变量</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑环境' : '新建环境'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="环境编码"><el-input v-model="form.envCode" /></el-form-item>
        <el-form-item label="环境名称"><el-input v-model="form.envName" /></el-form-item>
        <el-form-item label="Base URL"><el-input v-model="form.baseUrl" /></el-form-item>
        <el-form-item label="级别">
          <el-select v-model="form.envLevel">
            <el-option label="测试" value="TEST" />
            <el-option label="开发" value="DEV" />
            <el-option label="预发" value="PRED" />
            <el-option label="生产" value="PROD" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="varDialogVisible" :title="'变量 - ' + currentEnvName" width="560px">
      <div class="toolbar">
        <el-input v-model="newVarKey" placeholder="变量名" style="width: 160px" />
        <el-input v-model="newVarValue" placeholder="变量值" style="width: 200px" />
        <el-checkbox v-model="newVarSecret">敏感</el-checkbox>
        <el-button type="primary" @click="addVar">添加</el-button>
      </div>
      <el-table :data="variables" border stripe>
        <el-table-column prop="varKey" label="变量名" width="160" />
        <el-table-column label="值" min-width="180">
          <template #default="{ row }">{{ row.isSecret === 1 ? '******' : row.valueCiphertext }}</template>
        </el-table-column>
        <el-table-column prop="isSecret" label="敏感" width="70">
          <template #default="{ row }">{{ row.isSecret === 1 ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }"><el-button link type="danger" @click="removeVar(row)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { projectList } from '@/api/project'
import { envList, envCreate, envUpdate, envDelete, variableList, variableSave, variableDelete } from '@/api/env'

const projects = ref<any[]>([])
const projectId = ref<number>()
const records = ref<any[]>([])
const dialogVisible = ref(false)
const form = reactive<any>({})
const varDialogVisible = ref(false)
const variables = ref<any[]>([])
const currentEnvId = ref<number>()
const currentEnvName = ref('')
const newVarKey = ref('')
const newVarValue = ref('')
const newVarSecret = ref(false)

async function loadProjects() {
  const data: any = await projectList({ page: 1, pageSize: 100 })
  projects.value = data.records
  if (projects.value.length && !projectId.value) {
    projectId.value = projects.value[0].id
    load()
  }
}

async function load() {
  if (!projectId.value) return
  records.value = (await envList(projectId.value)) as any[]
}

function openForm(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, row)
  else Object.assign(form, { envLevel: 'TEST', status: 1 })
  dialogVisible.value = true
}

async function save() {
  form.projectId = projectId.value
  if (form.id) await envUpdate(form)
  else await envCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function remove(row: any) {
  await envDelete(row.id)
  ElMessage.success('已删除')
  load()
}

async function openVars(row: any) {
  currentEnvId.value = row.id
  currentEnvName.value = row.envName
  await loadVars()
  varDialogVisible.value = true
}

async function loadVars() {
  variables.value = (await variableList(projectId.value!, currentEnvId.value!)) as any[]
}

async function addVar() {
  if (!newVarKey.value) return
  await variableSave({ projectId: projectId.value, environmentId: currentEnvId.value, varKey: newVarKey.value, valueCiphertext: newVarValue.value, isSecret: newVarSecret.value ? 1 : 0 })
  newVarKey.value = ''
  newVarValue.value = ''
  newVarSecret.value = false
  loadVars()
}

async function removeVar(row: any) {
  await variableDelete(row.id)
  loadVars()
}

onMounted(loadProjects)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  align-items: center;
}
</style>
