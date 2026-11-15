<template>
  <div>
    <div class="toolbar">
      <el-select v-model="projectId" placeholder="选择项目" style="width: 180px" @change="loadAll">
        <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
      </el-select>
      <el-select v-model="serviceId" placeholder="选择服务" style="width: 180px" clearable @change="loadDefs">
        <el-option v-for="s in services" :key="s.id" :label="s.serviceName" :value="s.id" />
      </el-select>
      <el-button type="primary" @click="openService()">新建服务</el-button>
      <el-button type="primary" @click="openDef()">新建接口</el-button>
    </div>

    <el-table :data="definitions" border stripe>
      <el-table-column prop="apiName" label="接口名称" min-width="160" />
      <el-table-column prop="httpMethod" label="方法" width="90" />
      <el-table-column prop="path" label="路径" min-width="220" />
      <el-table-column prop="authType" label="认证" width="100" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDebug(row)">调试</el-button>
          <el-button link type="danger" @click="removeDef(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="serviceDialog" title="新建服务" width="420px">
      <el-form><el-form-item label="服务名称"><el-input v-model="serviceName" /></el-form-item></el-form>
      <template #footer>
        <el-button @click="serviceDialog = false">取消</el-button>
        <el-button type="primary" @click="saveService">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="defDialog" :title="defForm.id ? '编辑接口' : '新建接口'" width="520px">
      <el-form :model="defForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="defForm.apiName" /></el-form-item>
        <el-form-item label="方法">
          <el-select v-model="defForm.httpMethod">
            <el-option v-for="m in ['GET','POST','PUT','PATCH','DELETE']" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="路径"><el-input v-model="defForm.path" /></el-form-item>
        <el-form-item label="认证">
          <el-select v-model="defForm.authType">
            <el-option label="无" value="NONE" />
            <el-option label="Bearer" value="BEARER" />
            <el-option label="继承" value="INHERIT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="defDialog = false">取消</el-button>
        <el-button type="primary" @click="saveDef">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="debugDialog" title="接口调试" width="760px">
      <div class="debug-row">
        <el-select v-model="debug.envId" placeholder="环境" style="width: 200px" @change="onEnvChange">
          <el-option v-for="e in envs" :key="e.id" :label="e.envName" :value="e.id" />
        </el-select>
        <el-select v-model="debug.method" style="width: 110px">
          <el-option v-for="m in ['GET','POST','PUT','PATCH','DELETE']" :key="m" :label="m" :value="m" />
        </el-select>
        <el-input v-model="debug.url" placeholder="完整 URL" style="flex: 1" />
      </div>
      <el-input v-model="debug.body" type="textarea" :rows="4" placeholder="请求体 JSON" style="margin: 10px 0" />
      <el-divider>断言</el-divider>
      <div v-for="(a, i) in debug.assertions" :key="i" class="debug-row" style="margin-bottom: 6px">
        <el-select v-model="a.type" style="width: 130px">
          <el-option label="HTTP状态码" value="httpStatus" />
          <el-option label="body.code" value="bodyCode" />
          <el-option label="包含文本" value="contains" />
          <el-option label="耗时(ms)" value="elapsedMs" />
        </el-select>
        <el-input v-model="a.expect" placeholder="期望值" style="width: 160px" />
        <el-button link type="danger" @click="debug.assertions.splice(i, 1)">删除</el-button>
      </div>
      <el-button link type="primary" @click="debug.assertions.push({ type: 'httpStatus', expect: '200' })">+ 断言</el-button>
      <el-divider>变量提取</el-divider>
      <div v-for="(e, i) in debug.extracts" :key="i" class="debug-row" style="margin-bottom: 6px">
        <el-input v-model="e.varName" placeholder="变量名" style="width: 160px" />
        <el-input v-model="e.path" placeholder="JSON路径，如 data.token" style="width: 240px" />
        <el-button link type="danger" @click="debug.extracts.splice(i, 1)">删除</el-button>
      </div>
      <el-button link type="primary" @click="debug.extracts.push({ varName: '', path: '' })">+ 提取</el-button>
      <template #footer>
        <el-button @click="debugDialog = false">关闭</el-button>
        <el-button type="primary" :loading="sending" @click="send">发送</el-button>
      </template>
      <div v-if="result" class="result">
        <p>状态码 <b>{{ result.httpStatus }}</b> ｜ 耗时 <b>{{ result.elapsedMs }}ms</b></p>
        <p v-for="(r, i) in result.assertionResults" :key="i" :class="r.passed ? 'ok' : 'fail'">
          [{{ r.type }}] 期望={{ r.expect }} 实际={{ r.actual }} → {{ r.passed ? '通过' : '失败' }}
        </p>
        <p v-if="result.extractedVars && Object.keys(result.extractedVars).length">提取: {{ JSON.stringify(result.extractedVars) }}</p>
        <pre>{{ result.bodyText }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { projectList } from '@/api/project'
import { envList } from '@/api/env'
import { serviceList, serviceCreate, serviceDelete, definitionList, definitionCreate, definitionUpdate, definitionDelete, debugSend } from '@/api/asset'

const projects = ref<any[]>([])
const projectId = ref<number>()
const services = ref<any[]>([])
const serviceId = ref<number>()
const definitions = ref<any[]>([])
const envs = ref<any[]>([])

const serviceDialog = ref(false)
const serviceName = ref('')
const defDialog = ref(false)
const defForm = reactive<any>({})

const debugDialog = ref(false)
const sending = ref(false)
const debug = reactive<any>({ envId: null, method: 'GET', url: '', body: '', assertions: [{ type: 'httpStatus', expect: '200' }], extracts: [] })
const result = ref<any>(null)

async function loadProjects() {
  const data: any = await projectList({ page: 1, pageSize: 100 })
  projects.value = data.records
  if (projects.value.length && !projectId.value) {
    projectId.value = projects.value[0].id
    loadAll()
  }
}

async function loadAll() {
  if (!projectId.value) return
  services.value = (await serviceList(projectId.value)) as any[]
  envs.value = (await envList(projectId.value)) as any[]
  loadDefs()
}

async function loadDefs() {
  if (!projectId.value) return
  definitions.value = (await definitionList(projectId.value, serviceId.value)) as any[]
}

function openService() { serviceName.value = ''; serviceDialog.value = true }
async function saveService() {
  await serviceCreate({ projectId: projectId.value, serviceName: serviceName.value })
  ElMessage.success('已创建')
  serviceDialog.value = false
  loadAll()
}

function openDef(row?: any) {
  Object.keys(defForm).forEach((k) => delete defForm[k])
  if (row) Object.assign(defForm, row)
  else Object.assign(defForm, { httpMethod: 'GET', authType: 'NONE' })
  defDialog.value = true
}
async function saveDef() {
  defForm.projectId = projectId.value
  defForm.serviceId = serviceId.value
  if (defForm.id) await definitionUpdate(defForm)
  else await definitionCreate(defForm)
  ElMessage.success('已保存')
  defDialog.value = false
  loadDefs()
}
async function removeDef(row: any) {
  await definitionDelete(row.id)
  loadDefs()
}

function openDebug(row: any) {
  result.value = null
  debug.method = row.httpMethod
  debug.body = ''
  debug.assertions = [{ type: 'httpStatus', expect: '200' }]
  debug.extracts = []
  const env = envs.value[0]
  if (env) { debug.envId = env.id; debug.url = env.baseUrl + row.path }
  else debug.url = row.path
  debugDialog.value = true
}

function onEnvChange() {
  const env = envs.value.find((e) => e.id === debug.envId)
  if (env) {
    const idx = debug.url.indexOf('/api')
    if (idx > 0) debug.url = env.baseUrl + debug.url.slice(idx)
  }
}

async function send() {
  sending.value = true
  try {
    const res: any = await debugSend({
      method: debug.method, url: debug.url, body: debug.body,
      assertions: debug.assertions.map((a: any) => ({ type: a.type, expect: a.expect })),
      extracts: debug.extracts.map((e: any) => ({ varName: e.varName, path: e.path }))
    })
    result.value = res
  } finally {
    sending.value = false
  }
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
.debug-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.result {
  margin-top: 12px;
  font-size: 13px;
}
.result pre {
  background: #f6f8fa;
  padding: 10px;
  max-height: 220px;
  overflow: auto;
}
.ok { color: #15803d; }
.fail { color: #dc2626; }
</style>
