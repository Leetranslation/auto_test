<template>
  <div>
    <div class="toolbar">
      <el-select v-model="projectId" placeholder="选择项目" style="width: 200px" @change="load">
        <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
      </el-select>
      <el-button type="primary" @click="load">刷新</el-button>
    </div>
    <el-table :data="records" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="executionNo" label="执行编号" width="180" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="durationMs" label="耗时(ms)" width="110" />
      <el-table-column prop="summaryJson" label="汇总" min-width="160" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-drawer v-model="drawer" title="执行详情" size="70%">
      <div v-if="detail">
        <div class="detail-head">
          <h4 style="margin:0">{{ detail.execution.executionNo }} — {{ detail.execution.status }}</h4>
          <el-button size="small" type="primary" @click="exportReport">导出报告</el-button>
        </div>
        <el-table :data="detail.steps" border stripe size="small">
          <el-table-column prop="sortNo" label="#" width="50" />
          <el-table-column prop="stepType" label="类型" width="90" />
          <el-table-column prop="stepName" label="名称" width="140" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="durationMs" label="耗时" width="80" />
          <el-table-column prop="errorMessage" label="错误" min-width="180" />
        </el-table>
        <h4>日志</h4>
        <el-timeline>
          <el-timeline-item v-for="l in detail.logs" :key="l.id" :timestamp="l.createdAt" :type="l.logLevel === 'ERROR' ? 'danger' : 'primary'">
            {{ l.message }}
          </el-timeline-item>
        </el-timeline>
        <h4>报告</h4>
        <pre>{{ detail.report }}</pre>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { projectList } from '@/api/project'
import { execList, execDetail } from '@/api/exec'

const projects = ref<any[]>([])
const projectId = ref<number>()
const records = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10 })
const drawer = ref(false)
const detail = ref<any>(null)

function statusType(s: string) {
  return s === 'PASSED' ? 'success' : s === 'FAILED' ? 'danger' : 'info'
}

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
  const data: any = await execList({ projectId: projectId.value, page: query.page, pageSize: query.pageSize })
  records.value = data.records
  total.value = data.total
}

async function openDetail(row: any) {
  detail.value = await execDetail(row.id)
  drawer.value = true
}

async function exportReport() {
  const id = detail.value?.execution?.id
  if (!id) return
  const token = localStorage.getItem('token')
  const resp = await fetch(`/api/report/export?executionId=${id}`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  const blob = await resp.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `report_${id}.html`
  a.click()
  URL.revokeObjectURL(url)
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
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
pre {
  background: #f6f8fa;
  padding: 10px;
  max-height: 200px;
  overflow: auto;
}
</style>
