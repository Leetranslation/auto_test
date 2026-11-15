<template>
  <div>
    <div class="toolbar">
      <el-select v-model="projectId" placeholder="选择项目" style="width: 200px" @change="load">
        <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
      </el-select>
      <el-button type="primary" @click="openForm()">新建套件</el-button>
    </div>
    <el-table :data="records" border stripe>
      <el-table-column prop="suiteName" label="套件名称" min-width="180" />
      <el-table-column prop="executionPolicy" label="策略" width="110" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="openItems(row)">成员</el-button>
          <el-button link type="success" @click="run(row)">一键执行</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建套件" width="420px">
      <el-form><el-form-item label="套件名称"><el-input v-model="form.suiteName" /></el-form-item></el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemsVisible" :title="'成员 - ' + currentSuiteName" width="620px">
      <div class="toolbar">
        <el-select v-model="newSceneId" placeholder="选择场景" style="width: 260px">
          <el-option v-for="s in scenes" :key="s.id" :label="s.sceneName" :value="s.id" />
        </el-select>
        <el-button type="primary" @click="addItem">添加</el-button>
      </div>
      <el-table :data="items" border stripe size="small">
        <el-table-column prop="sortNo" label="#" width="50" />
        <el-table-column prop="targetType" label="类型" width="90" />
        <el-table-column prop="targetId" label="目标ID" width="90" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }"><el-button link type="danger" @click="removeItem(row)">移除</el-button></template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="itemsVisible = false">关闭</el-button>
        <el-button type="success" @click="runCurrent">一键执行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { projectList } from '@/api/project'
import { sceneList } from '@/api/scene'
import { suiteList, suiteCreate, suiteDelete, suiteItems, suiteAddItem, suiteRemoveItem, suiteRun } from '@/api/suite'

const projects = ref<any[]>([])
const projectId = ref<number>()
const records = ref<any[]>([])
const dialogVisible = ref(false)
const form = reactive<any>({})
const itemsVisible = ref(false)
const items = ref<any[]>([])
const scenes = ref<any[]>([])
const newSceneId = ref<number>()
const currentSuiteId = ref<number>()
const currentSuiteName = ref('')

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
  records.value = (await suiteList(projectId.value)) as any[]
  const s: any = await sceneList({ projectId: projectId.value, page: 1, pageSize: 200 })
  scenes.value = s.records
}

function openForm() { form.suiteName = ''; dialogVisible.value = true }
async function save() {
  await suiteCreate({ projectId: projectId.value, suiteName: form.suiteName })
  ElMessage.success('已创建')
  dialogVisible.value = false
  load()
}
async function remove(row: any) {
  await suiteDelete(row.id)
  load()
}

async function openItems(row: any) {
  currentSuiteId.value = row.id
  currentSuiteName.value = row.suiteName
  items.value = (await suiteItems(row.id)) as any[]
  itemsVisible.value = true
}
async function addItem() {
  if (!newSceneId.value) return
  await suiteAddItem(currentSuiteId.value!, 'SCENE', newSceneId.value)
  items.value = (await suiteItems(currentSuiteId.value!)) as any[]
}
async function removeItem(row: any) {
  await suiteRemoveItem(row.id)
  items.value = (await suiteItems(currentSuiteId.value!)) as any[]
}

async function run(row: any) {
  const r: any = await suiteRun(row.id)
  ElMessage.success(`已提交 ${r.length} 个执行任务，去「执行与报告」查看进度`)
}
async function runCurrent() {
  await run({ id: currentSuiteId.value })
}

onMounted(loadProjects)
</script>

<style scoped>
.toolbar { display: flex; gap: 8px; margin-bottom: 14px; align-items: center; }
</style>
