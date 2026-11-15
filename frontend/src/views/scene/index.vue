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
        <el-table-column label="操作" width="200">
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

    <!-- ============ 第一层：场景编辑主页面 ============ -->
    <div v-else class="flow-editor">
      <!-- 顶部操作栏 -->
      <div class="editor-header">
        <span class="title">场景编辑 — {{ sceneForm.sceneName }}</span>
        <div class="header-actions">
          <el-button size="small" @click="cancelEditor">取消</el-button>
          <el-button size="small" type="success" @click="runEditor">执行</el-button>
          <el-button size="small" type="primary" @click="saveEditor">保存</el-button>
        </div>
      </div>

      <div class="editor-main">
        <!-- 左侧：API 列表区 -->
        <div class="left-api">
          <el-tabs v-model="apiTab" class="api-tabs">
            <el-tab-pane label="API 列表" name="all" />
            <el-tab-pane label="未使用" name="unused" />
            <el-tab-pane label="已使用" name="used" />
          </el-tabs>
          <el-button size="small" type="primary" class="add-api-btn" @click="addApiDialogVisible = true">添加 API</el-button>

          <div class="api-list">
            <div v-for="d in pagedApis" :key="d.id" class="api-item" :class="{ used: usedApiIds.has(d.id) }">
              <div class="api-desc">{{ d.apiName }}</div>
              <div class="api-meta">{{ d.serviceName }}</div>
              <div class="api-path">{{ d.path }}</div>
              <el-button v-if="!usedApiIds.has(d.id)" size="small" link type="primary" @click="addApiNode(d)">+ 选择</el-button>
              <el-tag v-else size="small" type="success">已使用</el-tag>
            </div>
            <div v-if="!filteredApis.length" class="empty">暂无接口数据</div>
          </div>

          <el-pagination
            small
            layout="prev, pager, next, total"
            :total="filteredApis.length"
            :page-size="apiPageSize"
            v-model:current-page="apiPage"
            class="api-pagination"
          />
        </div>

        <!-- 中间：场景流程画布 -->
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

        <!-- 右侧：管理功能区 -->
        <div class="right-manage">
          <el-tabs v-model="rightTab" class="manage-tabs">
            <el-tab-pane label="基础信息" name="base">
              <el-form label-width="70px" size="small">
                <el-form-item label="名称"><el-input v-model="sceneForm.sceneName" /></el-form-item>
                <el-form-item label="描述"><el-input v-model="sceneForm.description" type="textarea" :rows="3" /></el-form-item>
                <el-form-item label="项目"><el-input :model-value="projectName" disabled /></el-form-item>
                <el-form-item label="版本"><span>v{{ sceneForm.currentVersion }}</span></el-form-item>
                <el-form-item label="状态"><el-tag size="small">{{ sceneForm.status }}</el-tag></el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="上下文管理" name="context">
              <div class="ctx-tip">场景级变量（节点可读取/输出）</div>
              <div v-for="(v, k) in variables" :key="k" class="var-row">
                <el-input :model-value="k" disabled size="small" class="var-k" />
                <el-input v-model="variables[k]" size="small" class="var-v" />
                <el-button size="small" link type="danger" @click="deleteVariable(k)">删</el-button>
              </div>
              <div class="ctx-add">
                <el-input v-model="newVarKey" placeholder="变量名" size="small" class="var-k" />
                <el-input v-model="newVarValue" placeholder="变量值" size="small" class="var-v" />
                <el-button size="small" type="primary" @click="addVariable">添加</el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="脚本管理" name="script">
              <div class="ctx-tip">已绑定脚本的节点</div>
              <div v-for="n in scriptNodes" :key="n.id" class="script-item" @click="openNodeDetail(n)">
                <span class="s-name">{{ n.data.label }}</span>
                <span class="s-tag">Python</span>
              </div>
              <div v-if="!scriptNodes.length" class="empty">暂无脚本节点</div>
            </el-tab-pane>

            <el-tab-pane label="流程管理" name="flow">
              <div class="ctx-tip">节点</div>
              <div v-for="n in nodes" :key="n.id" class="flow-item">
                <span class="f-type">{{ typeTag(n.data.nodeType) }}</span>
                <span class="f-name">{{ n.data.label }}</span>
              </div>
              <div class="ctx-tip" style="margin-top: 10px">连线</div>
              <div v-for="e in edges" :key="e.id" class="flow-item">
                {{ nodeLabel(e.source) }} → {{ nodeLabel(e.target) }}
                <el-tag v-if="e.data?.condition" size="small" style="margin-left: 6px">{{ e.data.condition }}</el-tag>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>

    <!-- 添加 API 弹窗（分页选择） -->
    <el-dialog v-model="addApiDialogVisible" title="添加 API" width="720px">
      <el-table :data="pagedApis" border stripe size="small" @row-click="addApiNode">
        <el-table-column prop="apiName" label="接口描述" min-width="160" />
        <el-table-column prop="serviceName" label="服务名称" min-width="140" />
        <el-table-column prop="path" label="接口路径" min-width="220" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button v-if="!usedApiIds.has(row.id)" size="small" link type="primary" @click.stop="addApiNode(row)">选择</el-button>
            <el-tag v-else size="small" type="success">已使用</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination small layout="prev, pager, next" :total="filteredApis.length" :page-size="apiPageSize" v-model:current-page="apiPage" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { VueFlow } from '@vue-flow/core'
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
import { execRun } from '@/api/exec'
import { definitionList, serviceList } from '@/api/asset'

const nodeTypes = markRaw({ scene: SceneNode })

const TYPE_TAGS: Record<string, string> = {
  START: '开始', END: '结束', API: 'API', UI: 'UI', ASSERT: '断言',
  EXTRACT: '变量提取', CONDITION: '条件分支', LOOP: '循环', WAIT: '等待',
  SUBSCENE: '子场景', DATASET: '数据集迭代', SCRIPT: '脚本'
}
function typeTag(t: string) {
  return TYPE_TAGS[t] || t
}

// ---- 列表页 ----
const projects = ref<any[]>([])
const projectId = ref<number>()
const records = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10 })
const dialogVisible = ref(false)
const form = reactive<any>({})

// ---- 编辑器状态 ----
const editorVisible = ref(false)
const currentSceneId = ref<number>()
const sceneForm = reactive<any>({})
const nodes = ref<any[]>([])
const edges = ref<any[]>([])
const selectedNodeId = ref<string | null>(null)
const selectedEdgeId = ref<string | null>(null)
const rightTab = ref('base')
const apiTab = ref('all')
const apiPage = ref(1)
const apiPageSize = 10
const apiDefs = ref<any[]>([])
const services = ref<any[]>([])
const variables = ref<Record<string, any>>({})
const newVarKey = ref('')
const newVarValue = ref('')
const addApiDialogVisible = ref(false)
let nodeSeq = 0
let edgeSeq = 0

const projectName = computed(() => projects.value.find((p) => p.id === projectId.value)?.projectName || '')
const usedApiIds = computed(() => new Set(nodes.value.map((n) => n.data?.apiDefinitionId).filter(Boolean)))
const filteredApis = computed(() => {
  if (apiTab.value === 'used') return apiDefs.value.filter((d) => usedApiIds.value.has(d.id))
  if (apiTab.value === 'unused') return apiDefs.value.filter((d) => !usedApiIds.value.has(d.id))
  return apiDefs.value
})
const pagedApis = computed(() => {
  const s = (apiPage.value - 1) * apiPageSize
  return filteredApis.value.slice(s, s + apiPageSize)
})
const scriptNodes = computed(() => nodes.value.filter((n) => n.data?.script))
const selectedNode = computed(() => nodes.value.find((n) => n.id === selectedNodeId.value) || null)

function nodeLabel(id: string) {
  return nodes.value.find((n) => n.id === id)?.data?.label || id
}

// ---- 列表页方法 ----
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

// ---- API 列表 ----
async function loadApis() {
  const [defs, svcs]: any[] = await Promise.all([
    definitionList(projectId.value!),
    serviceList(projectId.value!)
  ])
  services.value = svcs || []
  const svcMap = new Map(services.value.map((s: any) => [s.id, s.serviceName]))
  apiDefs.value = (defs || []).map((d: any) => ({ ...d, serviceName: svcMap.get(d.serviceId) || '—' }))
}

// ---- 画布 ----
function onDragOver(event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
}
function onDrop() {
  /* 预留：支持从 API 列表拖入 */
}
function addApiNode(api: any) {
  const id = `node_${++nodeSeq}`
  nodes.value.push({
    id,
    type: 'scene',
    position: { x: 120 + (nodeSeq % 4) * 220, y: 100 + Math.floor(nodeSeq / 4) * 140 },
    data: {
      nodeType: 'API',
      label: api.apiName,
      apiDefinitionId: api.id,
      apiPath: api.path,
      apiMethod: api.httpMethod,
      script: genScriptTemplate(api),
      configText: '{}'
    }
  })
  selectedNodeId.value = id
  addApiDialogVisible.value = false
}
function genScriptTemplate(api: any) {
  return `# -*- coding: utf-8 -*-
"""
脚本名称：${api.apiName}
接口：${api.httpMethod || 'GET'} ${api.path}
说明：自动生成的初始模板，请按需修改
"""
import json


def init_result(code=0, msg="success", data=None):
    return {"code": code, "msg": msg, "data": data}


def execute(ctx):
    """节点执行入口，ctx 提供请求上下文与前序节点变量"""
    api_data = ctx.get("api", {})
    params = api_data.get("params", {})
    # 在此发起请求并处理响应（由执行器注入真实请求能力）
    response = {}
    return init_result(data=response)
`
}
function onConnect(params: any) {
  const id = `edge_${++edgeSeq}`
  edges.value.push({ id, source: params.source, target: params.target, label: '', data: { condition: '' } })
}
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

// ---- 打开/取消 ----
async function openEditor(row: any) {
  currentSceneId.value = row.id
  Object.assign(sceneForm, {
    sceneName: row.sceneName,
    description: row.description || '',
    currentVersion: row.currentVersion,
    status: row.status
  })
  nodes.value = []
  edges.value = []
  variables.value = {}
  apiPage.value = 1
  try {
    const graph: any = await sceneGraph(row.id)
    const parsed = typeof graph === 'string' ? JSON.parse(graph) : graph
    variables.value = parsed.variables || {}
    nodes.value = (parsed.nodes || []).map((n: any) => ({
      id: n.nodeKey || `node_${++nodeSeq}`,
      type: 'scene',
      position: { x: 120 + (nodeSeq % 4) * 220, y: 100 + Math.floor(nodeSeq / 4) * 140 },
      data: {
        nodeType: n.nodeType,
        label: n.nodeName || n.nodeType,
        apiDefinitionId: n.apiDefinitionId || null,
        apiPath: n.apiPath || '',
        apiMethod: n.apiMethod || '',
        script: n.script || '',
        configText: JSON.stringify(n.config || {})
      }
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
  await loadApis()
  editorVisible.value = true
}
async function cancelEditor() {
  try {
    await ElMessageBox.confirm('确定退出编辑？未保存的修改将丢失。', '提示', { type: 'warning' })
    editorVisible.value = false
  } catch {
    /* 取消 */
  }
}

// ---- 上下文变量 ----
function addVariable() {
  if (!newVarKey.value) return
  variables.value[newVarKey.value] = newVarValue.value
  newVarKey.value = ''
  newVarValue.value = ''
}
function deleteVariable(k: string) {
  delete variables.value[k]
}

// ---- 保存 / 执行 ----
function buildGraphJson() {
  const graphNodes = nodes.value.map((n) => {
    let config: any = {}
    try {
      config = n.data.configText ? JSON.parse(n.data.configText) : {}
    } catch {
      config = { raw: n.data.configText }
    }
    return {
      nodeKey: n.id,
      nodeType: n.data.nodeType,
      nodeName: n.data.label || n.data.nodeType,
      apiDefinitionId: n.data.apiDefinitionId || null,
      apiPath: n.data.apiPath || '',
      apiMethod: n.data.apiMethod || '',
      script: n.data.script || '',
      config
    }
  })
  const graphEdges = edges.value.map((e) => ({ source: e.source, target: e.target, condition: e.data?.condition || '' }))
  return JSON.stringify({ nodes: graphNodes, edges: graphEdges, variables: variables.value })
}
async function saveEditor() {
  await sceneUpdate({ id: currentSceneId.value, projectId: projectId.value, sceneName: sceneForm.sceneName, description: sceneForm.description })
  await sceneVersionSave({ sceneId: currentSceneId.value, graphJson: buildGraphJson(), publish: false })
  ElMessage.success('已保存')
}
async function runEditor() {
  await saveEditor()
  await execRun({ targetType: 'SCENE', targetId: currentSceneId.value, projectId: projectId.value, environmentId: null })
  ElMessage.success('已提交执行，去「执行与报告」查看')
}

// ---- 第二层节点详情入口（第三步实现）----
function openNodeDetail(n: any) {
  selectedNodeId.value = n.id
  ElMessage.info('节点详情编辑页在第三步实现')
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
.list-page {
  padding: 0;
}

/* 第一层编辑器 */
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
  gap: 6px;
}
.editor-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左：API 列表 */
.left-api {
  width: 300px;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}
.api-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 8px;
}
.add-api-btn {
  margin: 8px;
}
.api-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}
.api-item {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 8px 10px;
  margin-bottom: 8px;
  background: #fff;
  position: relative;
}
.api-item.used {
  opacity: 0.7;
}
.api-desc {
  font-size: 13px;
  color: #1f2937;
  padding-right: 60px;
}
.api-meta {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 2px;
}
.api-path {
  font-size: 11px;
  color: #6b7280;
  margin-top: 2px;
  word-break: break-all;
}
.api-item .el-button,
.api-item .el-tag {
  position: absolute;
  top: 8px;
  right: 8px;
}
.api-pagination {
  padding: 8px;
}
.empty {
  color: #9ca3af;
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
}

/* 中：画布 */
.center-canvas {
  flex: 1;
  min-width: 0;
  position: relative;
  background: #f8fafc;
}

/* 右：管理区 */
.right-manage {
  width: 320px;
  border-left: 1px solid #e5e7eb;
  background: #fafafa;
  overflow-y: auto;
  padding: 0 12px;
}
.manage-tabs :deep(.el-tabs__header) {
  margin: 0;
}
.ctx-tip {
  font-size: 12px;
  color: #9ca3af;
  margin: 8px 0;
}
.var-row,
.ctx-add {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
  align-items: center;
}
.var-k {
  width: 100px;
}
.var-v {
  flex: 1;
}
.script-item,
.flow-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  margin-bottom: 6px;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
}
.s-name,
.f-name {
  flex: 1;
  color: #1f2937;
}
.s-tag,
.f-type {
  font-size: 11px;
  color: #6b7280;
}
</style>
