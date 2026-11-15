<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="4" v-for="c in cards" :key="c.label">
        <el-card shadow="hover">
          <div class="num">{{ c.value }}</div>
          <div class="lbl">{{ c.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <span>最近执行</span>
        <el-button size="small" type="primary" style="float: right" @click="consumeRecord">消费录制文件</el-button>
      </template>
      <el-table :data="summary.recent || []" border stripe size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="executionNo" label="编号" min-width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === 'PASSED' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column prop="finishedAt" label="完成时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { dashboardSummary, recordConsume } from '@/api/dashboard'
import { projectList } from '@/api/project'

const summary = ref<any>({})
const cards = computed(() => [
  { label: '项目', value: summary.value.projectCount ?? 0 },
  { label: '用例', value: summary.value.caseCount ?? 0 },
  { label: '场景', value: summary.value.sceneCount ?? 0 },
  { label: '执行次数', value: summary.value.executionCount ?? 0 },
  { label: '通过率', value: (summary.value.passRate ?? 0) + '%' }
])

async function load() {
  summary.value = await dashboardSummary()
}

async function consumeRecord() {
  const projects: any = await projectList({ page: 1, pageSize: 100 })
  if (!projects.records?.length) { ElMessage.warning('请先创建项目'); return }
  const r: any = await recordConsume({ projectId: projects.records[0].id })
  ElMessage.success(`消费 ${r.consumed} 行，新建接口 ${r.newDefinitions} 个、用例 ${r.newCases} 个`)
  load()
}

onMounted(load)
</script>

<style scoped>
.num { font-size: 28px; font-weight: 700; color: #2563eb; }
.lbl { color: #98a2b3; margin-top: 4px; }
</style>
