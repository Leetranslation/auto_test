<template>
  <div>
    <div class="toolbar">
      <el-select v-model="projectId" placeholder="选择项目" style="width: 200px" @change="load">
        <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
      </el-select>
      <el-button type="primary" @click="openForm()">新建场景</el-button>
    </div>
    <el-table :data="records" border stripe>
      <el-table-column prop="sceneCode" label="编号" width="180" />
      <el-table-column prop="sceneName" label="场景名称" min-width="180" />
      <el-table-column prop="currentVersion" label="版本" width="70" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button link type="success" @click="run(row)">执行</el-button>
          <el-button link type="primary" @click="openEditor(row)">编排</el-button>
          <el-button link type="primary" @click="openForm(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑场景' : '新建场景'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="场景名称"><el-input v-model="form.sceneName" /></el-form-item>
        <el-form-item label="场景编码"><el-input v-model="form.sceneCode" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editorVisible" :title="'编排 - ' + currentSceneName" width="920px" top="4vh">
      <div class="toolbar">
        <el-button @click="addStep('START')">+ 开始</el-button>
        <el-button @click="addStep('API')">+ API 节点</el-button>
        <el-button @click="addStep('UI')">+ UI 节点</el-button>
        <el-button @click="addStep('ASSERT')">+ 断言</el-button>
        <el-button @click="addStep('EXTRACT')">+ 变量提取</el-button>
        <el-button @click="addStep('CONDITION')">+ 条件</el-button>
        <el-button @click="addStep('END')">+ 结束</el-button>
      </div>

      <div class="hint">节点列表（顺序只影响显示，实际执行顺序由下面的「连线」决定；无连线时按列表顺序顺序执行）</div>
      <div v-for="(s, i) in steps" :key="s.nodeKey" class="step-row">
        <span class="step-idx" :title="s.nodeKey">{{ s.nodeKey }}</span>
        <el-select v-model="s.nodeType" style="width: 110px">
          <el-option v-for="t in ['START','API','UI','ASSERT','EXTRACT','CONDITION','END']" :key="t" :label="t" :value="t" />
        </el-select>
        <el-input v-model="s.nodeName" placeholder="步骤名" style="width: 150px" />
        <el-input v-model="s.configText" type="textarea" :rows="2" :placeholder="configPlaceholder(s.nodeType)" style="flex: 1" />
        <el-button-group>
          <el-button @click="move(i, -1)">↑</el-button>
          <el-button @click="move(i, 1)">↓</el-button>
          <el-button type="danger" @click="removeStep(s.nodeKey)">删</el-button>
        </el-button-group>
      </div>

      <el-divider>连线（流程顺序 / 条件分支）</el-divider>
      <div class="hint">条件节点：给它的出边设「真分支/假分支」，执行时按判断结果只走对应分支</div>
      <div v-for="(e, i) in edges" :key="i" class="edge-row">
        <el-select v-model="e.source" placeholder="源节点" style="width: 150px">
          <el-option v-for="k in stepKeys" :key="k" :label="k" :value="k" />
        </el-select>
        <span class="arrow">→</span>
        <el-select v-model="e.target" placeholder="目标节点" style="width: 150px">
          <el-option v-for="k in stepKeys" :key="k" :label="k" :value="k" />
        </el-select>
        <el-select v-model="e.condition" style="width: 140px">
          <el-option label="无条件" value="" />
          <el-option label="真分支 (true)" value="true" />
          <el-option label="假分支 (false)" value="false" />
        </el-select>
        <el-button type="danger" @click="edges.splice(i, 1)">删</el-button>
      </div>
      <el-button link type="primary" @click="addEdge">+ 添加连线</el-button>

      <template #footer>
        <el-checkbox v-model="publish" style="margin-right: 12px">发布</el-checkbox>
        <el-button @click="editorVisible = false">关闭</el-button>
        <el-button type="primary" @click="saveEditor">保存版本</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { projectList } from '@/api/project'
import { sceneList, sceneCreate, sceneUpdate, sceneDelete, sceneGraph, sceneVersionSave } from '@/api/scene'
import { execRun } from '@/api/exec'

const projects = ref<any[]>([])
const projectId = ref<number>()
const records = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10 })
const dialogVisible = ref(false)
const form = reactive<any>({})

const editorVisible = ref(false)
const steps = ref<any[]>([])
const edges = ref<any[]>([])
const currentSceneId = ref<number>()
const currentSceneName = ref('')
const publish = ref(false)
let nextKey = 1

const stepKeys = computed(() => steps.value.map((s) => s.nodeKey))

const placeholders: Record<string, string> = {
  API: '{"method":"POST","url":"http://...","body":"{...}","assertions":[{"type":"bodyCode","expect":0}],"extracts":[{"varName":"token","path":"data.token"}]}',
  UI: '{"baseUrl":"http://...","steps":[{"action":"click","locator":{"strategy":"DATA_TESTID","value":"xxx"}}]}',
  ASSERT: '{"type":"bodyCode","expect":0}',
  EXTRACT: '{"varName":"token","path":"data.token"}',
  CONDITION: '{"left":"${status}","op":"EQ","right":"0"}'
}
function configPlaceholder(t: string) {
  return placeholders[t] || ''
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
  const data: any = await sceneList({ projectId: projectId.value, page: query.page, pageSize: query.pageSize })
  records.value = data.records
  total.value = data.total
}

function openForm(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

async function save() {
  form.projectId = projectId.value
  if (form.id) await sceneUpdate(form)
  else await sceneCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function remove(row: any) {
  await sceneDelete(row.id)
  load()
}

async function run(row: any) {
  const loading = ElMessage.info('执行中...')
  try {
    const r: any = await execRun({ targetType: 'SCENE', targetId: row.id, projectId: projectId.value, environmentId: null })
    ElMessage({ type: r.status === 'PASSED' ? 'success' : 'error', message: `已提交执行: ${r.status}，去「执行与报告」查看`, duration: 5000 })
  } finally {
    loading.close()
  }
}

async function openEditor(row: any) {
  currentSceneId.value = row.id
  currentSceneName.value = row.sceneName
  publish.value = false
  try {
    const graph: any = await sceneGraph(row.id)
    const parsed = typeof graph === 'string' ? JSON.parse(graph) : graph
    steps.value = (parsed.nodes || []).map((n: any) => ({
      nodeKey: n.nodeKey || 'node_' + (nextKey++),
      nodeType: n.nodeType,
      nodeName: n.nodeName || '',
      configText: JSON.stringify(n.config || {})
    }))
    edges.value = (parsed.edges || []).map((e: any) => ({ source: e.source, target: e.target, condition: e.condition || '' }))
    const maxNum = steps.value.reduce((m, s) => {
      const n = parseInt(String(s.nodeKey).replace(/\D/g, ''), 10)
      return isNaN(n) ? m : Math.max(m, n)
    }, 0)
    nextKey = maxNum + 1
  } catch {
    steps.value = []
    edges.value = []
  }
  editorVisible.value = true
}

function addStep(type: string) {
  steps.value.push({ nodeKey: 'node_' + (nextKey++), nodeType: type, nodeName: '', configText: '' })
}

function removeStep(key: string) {
  steps.value = steps.value.filter((s) => s.nodeKey !== key)
  edges.value = edges.value.filter((e) => e.source !== key && e.target !== key)
}

function move(i: number, d: number) {
  const j = i + d
  if (j < 0 || j >= steps.value.length) return
  const tmp = steps.value[i]
  steps.value[i] = steps.value[j]
  steps.value[j] = tmp
}

function addEdge() {
  edges.value.push({ source: '', target: '', condition: '' })
}

async function saveEditor() {
  const nodes = steps.value.map((s) => {
    let config: any = {}
    try { config = s.configText ? JSON.parse(s.configText) : {} } catch { config = { raw: s.configText } }
    return { nodeKey: s.nodeKey, nodeType: s.nodeType, nodeName: s.nodeName || s.nodeType, config }
  })
  const validEdges = edges.value.filter((e) => e.source && e.target)
  const graphJson = JSON.stringify({ nodes, edges: validEdges })
  await sceneVersionSave({ sceneId: currentSceneId.value, graphJson, publish: publish.value })
  ElMessage.success('版本已保存')
  editorVisible.value = false
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
  flex-wrap: wrap;
}
.hint {
  font-size: 12px;
  color: #98a2b3;
  margin-bottom: 8px;
}
.step-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  margin-bottom: 8px;
}
.step-idx {
  width: 56px;
  line-height: 32px;
  text-align: center;
  color: #98a2b3;
  font-size: 12px;
  cursor: help;
}
.edge-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.arrow {
  color: #98a2b3;
}
</style>
