<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="项目名称/编码" clearable style="width: 240px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="primary" @click="openForm()">新建项目</el-button>
    </div>
    <el-table :data="records" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="projectCode" label="项目编码" width="150" />
      <el-table-column prop="projectName" label="项目名称" min-width="160" />
      <el-table-column prop="projectType" label="类型" width="100" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button link type="warning" @click="toggleStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑项目' : '新建项目'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="项目编码"><el-input v-model="form.projectCode" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="项目名称"><el-input v-model="form.projectName" /></el-form-item>
        <el-form-item label="项目类型">
          <el-select v-model="form.projectType">
            <el-option label="混合" value="MIXED" />
            <el-option label="API" value="API" />
            <el-option label="Web" value="WEB" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
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
import { projectList, projectCreate, projectUpdate, projectStatus } from '@/api/project'

const records = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10, keyword: '' })
const dialogVisible = ref(false)
const form = reactive<any>({})

async function load() {
  const data: any = await projectList(query)
  records.value = data.records
  total.value = data.total
}

function openForm(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, row)
  else Object.assign(form, { projectType: 'MIXED' })
  dialogVisible.value = true
}

async function save() {
  if (form.id) {
    await projectUpdate(form)
  } else {
    await projectCreate(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function toggleStatus(row: any) {
  await projectStatus(row.id, row.status === 1 ? 0 : 1)
  load()
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
