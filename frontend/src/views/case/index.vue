<template>
  <div>
    <div class="toolbar">
      <el-select v-model="projectId" placeholder="选择项目" style="width: 200px" @change="load">
        <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="用例名称/编码" clearable style="width: 200px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="primary" @click="openForm()">新建用例</el-button>
    </div>
    <el-table :data="records" border stripe>
      <el-table-column prop="caseCode" label="编号" width="170" />
      <el-table-column prop="caseName" label="名称" min-width="180" />
      <el-table-column prop="caseType" label="类型" width="80" />
      <el-table-column prop="priority" label="优先级" width="80" />
      <el-table-column prop="currentVersion" label="版本" width="70" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用例' : '新建用例'" width="640px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用例名称"><el-input v-model="form.caseName" /></el-form-item>
        <el-form-item label="用例编码"><el-input v-model="form.caseCode" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.caseType"><el-option label="API" value="API" /><el-option label="UI" value="UI" /></el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority"><el-option label="P0" value="P0" /><el-option label="P1" value="P1" /><el-option label="P2" value="P2" /></el-select>
        </el-form-item>
        <el-form-item label="定义 JSON"><el-input v-model="form.definitionJson" type="textarea" :rows="6" placeholder='{"method":"POST","url":"...","assertions":[...]}' /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { projectList } from '@/api/project'
import { caseList, caseCreate, caseUpdate, caseDelete } from '@/api/case'

const projects = ref<any[]>([])
const projectId = ref<number>()
const records = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10, keyword: '' })
const dialogVisible = ref(false)
const form = reactive<any>({})

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
  const data: any = await caseList({ projectId: projectId.value, page: query.page, pageSize: query.pageSize, keyword: query.keyword })
  records.value = data.records
  total.value = data.total
}

function openForm(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, row)
  else Object.assign(form, { caseType: 'API', priority: 'P1' })
  dialogVisible.value = true
}

async function save() {
  form.projectId = projectId.value
  if (form.id) await caseUpdate(form)
  else await caseCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function remove(row: any) {
  await caseDelete(row.id)
  ElMessage.success('已删除')
  load()
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
