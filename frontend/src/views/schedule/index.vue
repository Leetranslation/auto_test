<template>
  <div>
    <div class="toolbar">
      <el-select v-model="projectId" placeholder="选择项目" style="width: 200px" @change="load">
        <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
      </el-select>
      <el-button type="primary" @click="openForm()">新建定时任务</el-button>
    </div>
    <el-table :data="records" border stripe>
      <el-table-column prop="jobName" label="任务名" min-width="160" />
      <el-table-column prop="targetType" label="目标类型" width="100" />
      <el-table-column prop="targetId" label="目标ID" width="90" />
      <el-table-column prop="nextRunAt" label="下次执行" width="180" />
      <el-table-column prop="lastRunAt" label="上次执行" width="180" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="warning" @click="toggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建定时任务" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="任务名"><el-input v-model="form.jobName" /></el-form-item>
        <el-form-item label="目标场景ID"><el-input-number v-model="form.targetId" :min="1" /></el-form-item>
        <el-form-item label="下次执行">
          <el-date-picker v-model="form.nextRunAt" type="datetime" placeholder="默认1分钟后" style="width: 100%" />
        </el-form-item>
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
import { scheduleList, scheduleCreate, scheduleDelete, scheduleStatus } from '@/api/schedule'

const projects = ref<any[]>([])
const projectId = ref<number>()
const records = ref<any[]>([])
const dialogVisible = ref(false)
const form = reactive<any>({ targetType: 'SCENE' })

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
  records.value = (await scheduleList(projectId.value)) as any[]
}

function openForm() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { targetType: 'SCENE' })
  dialogVisible.value = true
}

async function save() {
  form.projectId = projectId.value
  await scheduleCreate(form)
  ElMessage.success('已创建（默认1分钟后执行，之后每日循环）')
  dialogVisible.value = false
  load()
}

async function toggle(row: any) {
  await scheduleStatus(row.id, row.status === 1 ? 0 : 1)
  load()
}

async function remove(row: any) {
  await scheduleDelete(row.id)
  load()
}

onMounted(loadProjects)
</script>

<style scoped>
.toolbar { display: flex; gap: 8px; margin-bottom: 14px; align-items: center; }
</style>
