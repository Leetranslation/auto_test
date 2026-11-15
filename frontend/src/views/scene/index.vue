<template>
  <div>
    <!-- ============ 场景列表 ============ -->
    <div v-if="!editorVisible" class="list-page">
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
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditor(row)">编排</el-button>
            <el-button link type="success" @click="run(row)">执行</el-button>
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
    </div>

    <!-- ============ 可视化 DAG 编排器（全屏） ============ -->
    <div v-else class="flow-editor">
      <div class="editor-header">
        <span class="title">场景编排 — {{ currentSceneName }}</span>
        <div class="header-actions">
          <el-button size="small" @click="validate">静态校验</el-button>
          <el-button size="small" type="primary" @click="saveEditor">保存</el-button>
          <el-button size="small" type="success" @click="runEditor">运行</el-button>
          <el-checkbox v-model="publish" size="small" style="margin: 0 8px">发布</el-checkbox>
          <el-button size="small" @click="closeEditor">关闭</el-button>
        </div>
      </div>

      <div class="editor-main">
        <!-- 左：节点组件库 -->
        <div class="left-palette">
          <div class="palette-title">节点组件库</div>
          <div
            v-for="n in NODE_TYPES"
            :key="n.type"
            class="palette-item"
            draggable="true"
            @dragstart="onDragStart($event, n.type)"
            @click="addNode(n.type)"
          >
            <span class="dot" :style="{ background: n.color }"></span>{{ n.label }}
          </div>
          <div class="palette-tip">拖拽或点击节点到画布</div>
        </div>

        <!-- 中：无限画布 -->
        <div class="center-canvas" @drop="onDrop" @dragover="onDragOver">
          <VueFlow
            v-model:nodes="nodes"
            v-model:edges="edges"
            :node-types="nodeTypes"
            :default-viewport="{ zoom: 1 }"
            fit-view-on-init
            :delete-key-code="['Backspace', 'Delete']"
            @node-click="onNodeClick"
            @edge-click="onEdgeClick"
            @pane-click="onPaneClick"
            @connect="onConnect"
          >
            <Background :gap="16" pattern-color="#e5e7eb" />
            <Controls />
            <MiniMap />
          </VueFlow>
        </div>

        <!-- 右：节点配置 -->
        <div class="right-config">
          <div class="config-title">{{ selectedEdge ? '连线配置' : '节点配置' }}</div>
          <template v-if="selectedNode">
            <div class="cfg-row">
              <span class="lbl">类型</span>
              <el-tag size="small">{{ nodeTypeLabel(selectedNode.data.nodeType) }}</el-tag>
            </div>
            <div class="cfg-row">
              <span class="lbl">名称</span>
              <el-input v-model="selectedNode.data.label" size="small" />
            </div>
            <div class="cfg-col">
              <span class="lbl">配置 JSON</span>
              <el-input v-model="selectedNode.data.configText" type="textarea" :rows="12" :placeholder="configPlaceholder(selectedNode.data.nodeType)" />
            </div>
          </template>
          <template v-else-if="selectedEdge">
            <div class="cfg-row">
              <span class="lbl">连线条件</span>
              <el-select v-model="selectedEdge.data.condition" size="small" @change="syncEdgeLabel(selectedEdge)">
                <el-option label="无条件" value="" />
                <el-option label="真分支 (true)" value="true" />
                <el-option label="假分支 (false)" value="false" />
              </el-select>
            </div>
            <div class="cfg-tip">条件分支节点的出边需设 true/false；其余节点出边留空即可。</div>
          </template>
          <template v-else>
            <div class="cfg-tip">点击画布中的节点或连线进行配置</div>
          </template>
        </div>
      </div>

      <!-- 底部：运行日志 + 变量 -->
      <div class="bottom-panel">
        <el-tabs v-model="bottomTab" class="bottom-tabs">
          <el-tab-pane label="运行日志" name="log">
            <div class="log-box">
              <div v-for="(l, i) in logs" :key="i" class="log-line" :class="{ err: l.logLevel === 'ERROR' }">
                [{{ l.logLevel }}] {{ l.message }}
              </div>
              <div v-if="!logs.length" class="log-empty">暂无日志，点「运行」开始执行</div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="变量" name="vars">
            <div class="vars-box">
              <div v-for="(v, k) in variables" :key="k" class="var-row">
                <span class="k">{{ k }}</span><span class="v">{{ v }}</span>
              </div>
              <div v-if="!Object.keys(variables).length" class="log-empty">暂无变量</div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'
import SceneNode from '@/components/SceneNode.vue'
import { projectList } from '@/api/project'
import { sceneList, sceneCreate, sceneUpdate, sceneDelete, sceneGraph, sceneVersionSave } from '@/api/scene'
import { execRun, execDetail } from '@/api/exec'

const nodeTypes = markRaw({ scene: SceneNode })

const NODE_TYPES = [
  { type: 'START', label: '开始', color: '#16a34a' },
  { type: 'END', label: '结束', color: '#ca8a04' },
  { type: 'API', label: 'API 请求', color: '#2563eb' },
  { type: 'UI', label: 'UI 操作', color: '#7c3aed' },
  { type: 'ASSERT', label: '断言', color: '#dc2626' },
  { type: 'EXTRACT', label: '变量提取', color: '#0891b2' },
  { type: 'CONDITION', label: '条件分支', color: '#ea580c' },
  { type: 'LOOP', label: '循环', color: '#db2777' },
  { type: 'WAIT', label: '等待', color: '#64748b' },
  { type: 'SUBSCENE', label: '子场景', color: '#0d9488' },
  { type: 'DATASET', label: '数据集迭代', color: '#4f46e5' },
  { type: 'SCRIPT', label: '脚本', color: '#475569' }
]

const PLACEHOLDERS: Record<string, string> = {
  START: '{}',
  END: '{}',
  API: '{"method":"POST","url":"http://...","body":"{...}","assertions":[{"type":"bodyCode","expect":0}],"extracts":[{"varName":"token","path":"data.token"}]}',
  UI: '{"baseUrl":"http://...","steps":[{"action":"click","locator":{"strategy":"DATA_TESTID","value":"xxx"}}]}',
  ASSERT: '{"type":"bodyCode","expect":0}',
  EXTRACT: '{"varName":"token","path":"data.token"}',
  CONDITION: '{"left":"${status}","op":"EQ","right":"0"}',
  LOOP: '{"maxIterations":10,"condition":{"left":"${i}","op":"LT","right":"10"}}',
  WAIT: '{"millis":1000}',
  SUBSCENE: '{"sceneId":0}',
  DATASET: '{"datasetRef":"xxx","itemVar":"row"}',
  SCRIPT: '{"language":"python","code":"..."}'
}

const projects = ref<any[]>([])
const projectId = ref<number>()
const records = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10 })
const dialogVisible = ref(false)
const form = reactive<any>({})

const editorVisible = ref(false)
const currentSceneId = ref<number>()
const currentSceneName = ref('')
const publish = ref(false)

const nodes = ref<any[]>([])
const edges = ref<any[]>([])
const selectedNodeId = ref<string | null>(null)
const selectedEdgeId = ref<string | null>(null)
const bottomTab = ref('log')
const logs = ref<any[]>([])
const variables = ref<Record<string, any>>({})

let nodeSeq = 0
let edgeSeq = 0
let pollTimer: any = null
const { screenToFlowCoordinate } = useVueFlow()

const selectedNode = computed(() => nodes.value.find((n) => n.id === selectedNodeId.value) || null)
const selectedEdge = computed(() => edges.value.find((e) => e.id === selectedEdgeId.value) || null)

function nodeTypeLabel(t: string) {
  return NODE_TYPES.find((n) => n.type === t)?.label || t
}
function configPlaceholder(t: string) {
  return PLACEHOLDERS[t] || ''
}

// ---- 列表 ----
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
  await execRun({ targetType: 'SCENE', targetId: row.id, projectId: projectId.value, environmentId: null })
  ElMessage.success('已提交执行，去「执行与报告」查看')
}

// ---- 画布拖拽 ----
function onDragStart(event: DragEvent, type: string) {
  if (event.dataTransfer) {
    event.dataTransfer.setData('application/vueflow', type)
    event.dataTransfer.effectAllowed = 'move'
  }
}
function onDragOver(event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
}
function onDrop(event: DragEvent) {
  const type = event.dataTransfer?.getData('application/vueflow')
  if (!type) return
  let position = { x: 100 + (nodeSeq % 5) * 200, y: 80 + Math.floor(nodeSeq / 5) * 120 }
  try {
    position = screenToFlowCoordinate({ x: event.clientX, y: event.clientY })
  } catch {
    /* 初始化前兜底 */
  }
  addNode(type, position)
}
function addNode(type: string, position?: { x: number; y: number }) {
  const meta = NODE_TYPES.find((n) => n.type === type)
  const id = `node_${++nodeSeq}`
  nodes.value.push({
    id,
    type: 'scene',
    position: position || { x: 100 + (nodeSeq % 5) * 200, y: 80 + Math.floor(nodeSeq / 5) * 120 },
    data: { nodeType: type, label: meta?.label || type, configText: '' }
  })
  selectedNodeId.value = id
  selectedEdgeId.value = null
}
function onConnect(params: any) {
  const id = `edge_${++edgeSeq}`
  edges.value.push({ id, source: params.source, target: params.target, label: '', data: { condition: '' } })
}
function syncEdgeLabel(e: any) {
  e.label = e.data.condition || ''
}

// ---- 选中 ----
function onNodeClick({ node }: any) {
  selectedNodeId.value = node.id
  selectedEdgeId.value = null
}
function onEdgeClick({ edge }: any) {
  selectedEdgeId.value = edge.id
  selectedNodeId.value = null
}
function onPaneClick() {
  selectedNodeId.value = null
  selectedEdgeId.value = null
}

// ---- 打开/关闭 ----
async function openEditor(row: any) {
  currentSceneId.value = row.id
  currentSceneName.value = row.sceneName
  publish.value = false
  nodes.value = []
  edges.value = []
  logs.value = []
  variables.value = {}
  try {
    const graph: any = await sceneGraph(row.id)
    const parsed = typeof graph === 'string' ? JSON.parse(graph) : graph
    nodes.value = (parsed.nodes || []).map((n: any) => ({
      id: n.nodeKey || `node_${++nodeSeq}`,
      type: 'scene',
      position: { x: 100 + (nodeSeq % 5) * 200, y: 80 + Math.floor(nodeSeq / 5) * 120 },
      data: { nodeType: n.nodeType, label: n.nodeName || n.nodeType, configText: JSON.stringify(n.config || {}) }
    }))
    edges.value = (parsed.edges || []).map((e: any) => ({
      id: `edge_${++edgeSeq}`,
      source: e.source,
      target: e.target,
      label: e.condition || '',
      data: { condition: e.condition || '' }
    }))
    const maxN = nodes.value.reduce((m, n) => Math.max(m, parseInt(String(n.id).replace(/\D/g, ''), 10) || 0), 0)
    nodeSeq = maxN
  } catch {
    nodes.value = []
    edges.value = []
  }
  editorVisible.value = true
}
function closeEditor() {
  editorVisible.value = false
  stopPoll()
}

// ---- 校验 / 保存 / 运行 ----
function validate() {
  const hasStart = nodes.value.some((n) => n.data.nodeType === 'START')
  const hasEnd = nodes.value.some((n) => n.data.nodeType === 'END')
  const ids = new Set(nodes.value.map((n) => n.id))
  const linked = new Set<string>()
  edges.value.forEach((e) => {
    linked.add(e.source)
    linked.add(e.target)
  })
  const orphan = nodes.value.filter((n) => !linked.has(n.id) && n.data.nodeType !== 'START' && n.data.nodeType !== 'END')
  const problems: string[] = []
  if (!hasStart) problems.push('缺少「开始」节点')
  if (!hasEnd) problems.push('缺少「结束」节点')
  if (orphan.length) problems.push(`存在孤立节点：${orphan.map((n) => n.data.label).join('、')}`)
  if (problems.length) {
    ElMessage.warning(problems.join('；'))
  } else {
    ElMessage.success('静态校验通过')
  }
}
function buildGraphJson() {
  const graphNodes = nodes.value.map((n) => {
    let config: any = {}
    try {
      config = n.data.configText ? JSON.parse(n.data.configText) : {}
    } catch {
      config = { raw: n.data.configText }
    }
    return { nodeKey: n.id, nodeType: n.data.nodeType, nodeName: n.data.label || n.data.nodeType, config }
  })
  const graphEdges = edges.value.map((e) => ({ source: e.source, target: e.target, condition: e.data?.condition || '' }))
  return JSON.stringify({ nodes: graphNodes, edges: graphEdges })
}
async function saveEditor() {
  await sceneVersionSave({ sceneId: currentSceneId.value, graphJson: buildGraphJson(), publish: publish.value })
  ElMessage.success('版本已保存')
}
async function runEditor() {
  await saveEditor()
  const r: any = await execRun({ targetType: 'SCENE', targetId: currentSceneId.value, projectId: projectId.value, environmentId: null })
  logs.value = []
  variables.value = {}
  bottomTab.value = 'log'
  startPoll(r.id)
  ElMessage.success('已提交执行')
}
function startPoll(executionId: number) {
  stopPoll()
  const tick = async () => {
    try {
      const d: any = await execDetail(executionId)
      logs.value = d.logs || []
      const vs = (d.logs || []).find((l: any) => l.message && l.message.startsWith('变量快照:'))
      if (vs) {
        try {
          variables.value = JSON.parse(vs.message.replace('变量快照:', '').trim())
        } catch {
          /* ignore */
        }
      }
      const st = d.execution?.status
      if (st === 'RUNNING' || st === 'QUEUED') {
        pollTimer = setTimeout(tick, 1500)
      } else if (st) {
        ElMessage({ type: st === 'PASSED' ? 'success' : 'error', message: `执行完成：${st}` })
      }
    } catch {
      pollTimer = setTimeout(tick, 2000)
    }
  }
  pollTimer = setTimeout(tick, 300)
}
function stopPoll() {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

onMounted(loadProjects)
onUnmounted(stopPoll)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  align-items: center;
}
.list-page {
  padding: 0;
}

/* 全屏编排器 */
.flow-editor {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: #fff;
  display: flex;
  flex-direction: column;
}
.editor-header {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #e5e7eb;
}
.editor-header .title {
  font-weight: 600;
  font-size: 15px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}
.editor-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}
.left-palette {
  width: 180px;
  border-right: 1px solid #e5e7eb;
  padding: 10px;
  overflow-y: auto;
  background: #fafafa;
}
.palette-title {
  font-weight: 600;
  margin-bottom: 10px;
  color: #374151;
}
.palette-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: grab;
  background: #fff;
  font-size: 13px;
  color: #1f2937;
}
.palette-item:active {
  cursor: grabbing;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.palette-tip {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 10px;
}
.center-canvas {
  flex: 1;
  min-width: 0;
  position: relative;
}
.right-config {
  width: 300px;
  border-left: 1px solid #e5e7eb;
  padding: 12px;
  overflow-y: auto;
  background: #fafafa;
}
.config-title {
  font-weight: 600;
  margin-bottom: 12px;
  color: #374151;
}
.cfg-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.cfg-col {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.lbl {
  width: 60px;
  font-size: 12px;
  color: #6b7280;
  flex-shrink: 0;
}
.cfg-tip {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.6;
}
.bottom-panel {
  height: 200px;
  border-top: 1px solid #e5e7eb;
  padding: 0 12px;
  overflow: hidden;
}
.bottom-tabs {
  height: 100%;
}
.log-box,
.vars-box {
  height: 150px;
  overflow-y: auto;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}
.log-line {
  padding: 2px 0;
  color: #374151;
}
.log-line.err {
  color: #dc2626;
}
.log-empty {
  color: #9ca3af;
  margin-top: 10px;
}
.var-row {
  display: flex;
  gap: 12px;
  padding: 2px 0;
}
.var-row .k {
  color: #2563eb;
  min-width: 120px;
}
.var-row .v {
  color: #374151;
  word-break: break-all;
}
</style>
