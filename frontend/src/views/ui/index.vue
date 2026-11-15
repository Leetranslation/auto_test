<template>
  <div>
    <div class="toolbar">
      <el-select v-model="projectId" placeholder="选择项目" style="width: 200px" @change="loadApps">
        <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
      </el-select>
      <el-button type="primary" @click="openApp()">新建应用</el-button>
    </div>
    <el-table :data="apps" border stripe>
      <el-table-column prop="appName" label="应用名称" width="160" />
      <el-table-column prop="baseUrl" label="Base URL" min-width="220" />
      <el-table-column prop="browserType" label="浏览器" width="110" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openApp(row)">编辑</el-button>
          <el-button link type="primary" @click="openPages(row)">页面对象</el-button>
          <el-button link type="danger" @click="removeApp(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="appDialog" :title="appForm.id ? '编辑应用' : '新建应用'" width="500px">
      <el-form :model="appForm" label-width="90px">
        <el-form-item label="应用名称"><el-input v-model="appForm.appName" /></el-form-item>
        <el-form-item label="Base URL"><el-input v-model="appForm.baseUrl" /></el-form-item>
        <el-form-item label="浏览器"><el-input v-model="appForm.browserType" placeholder="CHROMIUM" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appDialog = false">取消</el-button>
        <el-button type="primary" @click="saveApp">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pageDialog" :title="'页面对象 - ' + currentAppName" width="700px">
      <div class="toolbar">
        <el-button type="primary" @click="openPage()">新建页面</el-button>
      </div>
      <el-table :data="pages" border stripe>
        <el-table-column prop="pageName" label="页面名称" width="150" />
        <el-table-column prop="urlPattern" label="URL 模式" min-width="200" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openLocators(row)">定位器</el-button>
            <el-button link type="danger" @click="removePage(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="pageFormDialog" title="页面对象" width="460px">
      <el-form :model="pageForm" label-width="90px">
        <el-form-item label="页面名称"><el-input v-model="pageForm.pageName" /></el-form-item>
        <el-form-item label="URL 模式"><el-input v-model="pageForm.urlPattern" placeholder="/login" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="pageForm.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pageFormDialog = false">取消</el-button>
        <el-button type="primary" @click="savePage">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="locatorDialog" :title="'定位器 - ' + currentPageName" width="720px">
      <div class="toolbar">
        <el-input v-model="newLoc.locatorName" placeholder="名称" style="width: 140px" />
        <el-select v-model="newLoc.strategy" style="width: 140px">
          <el-option v-for="s in ['DATA_TESTID','ROLE','LABEL','NAME','ID','CSS','XPATH']" :key="s" :label="s" :value="s" />
        </el-select>
        <el-input v-model="newLoc.locatorValue" placeholder="定位值" style="width: 200px" />
        <el-button type="primary" @click="addLocator">添加</el-button>
      </div>
      <el-table :data="locators" border stripe>
        <el-table-column prop="locatorName" label="名称" width="140" />
        <el-table-column prop="strategy" label="策略" width="110" />
        <el-table-column prop="locatorValue" label="定位值" min-width="220" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }"><el-button link type="danger" @click="removeLocator(row)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { projectList } from '@/api/project'
import { appList, appCreate, appUpdate, appDelete, pageList, pageCreate, pageDelete, locatorList, locatorSave, locatorDelete } from '@/api/ui'

const projects = ref<any[]>([])
const projectId = ref<number>()
const apps = ref<any[]>([])
const appDialog = ref(false)
const appForm = reactive<any>({})

const pageDialog = ref(false)
const pages = ref<any[]>([])
const currentAppId = ref<number>()
const currentAppName = ref('')
const pageFormDialog = ref(false)
const pageForm = reactive<any>({})

const locatorDialog = ref(false)
const locators = ref<any[]>([])
const currentPageId = ref<number>()
const currentPageName = ref('')
const newLoc = reactive<any>({ strategy: 'DATA_TESTID' })

async function loadProjects() {
  const data: any = await projectList({ page: 1, pageSize: 100 })
  projects.value = data.records
  if (projects.value.length && !projectId.value) {
    projectId.value = projects.value[0].id
    loadApps()
  }
}

async function loadApps() {
  if (!projectId.value) return
  apps.value = (await appList(projectId.value)) as any[]
}

function openApp(row?: any) {
  Object.keys(appForm).forEach((k) => delete appForm[k])
  if (row) Object.assign(appForm, row)
  else Object.assign(appForm, { browserType: 'CHROMIUM' })
  appDialog.value = true
}
async function saveApp() {
  appForm.projectId = projectId.value
  if (appForm.id) await appUpdate(appForm)
  else await appCreate(appForm)
  ElMessage.success('保存成功')
  appDialog.value = false
  loadApps()
}
async function removeApp(row: any) {
  await appDelete(row.id)
  loadApps()
}

async function openPages(row: any) {
  currentAppId.value = row.id
  currentAppName.value = row.appName
  pages.value = (await pageList(projectId.value!, row.id)) as any[]
  pageDialog.value = true
}
function openPage(row?: any) {
  Object.keys(pageForm).forEach((k) => delete pageForm[k])
  if (row) Object.assign(pageForm, row)
  pageFormDialog.value = true
}
async function savePage() {
  pageForm.projectId = projectId.value
  pageForm.applicationId = currentAppId.value
  await pageCreate(pageForm)
  ElMessage.success('保存成功')
  pageFormDialog.value = false
  pages.value = (await pageList(projectId.value!, currentAppId.value)) as any[]
}
async function removePage(row: any) {
  await pageDelete(row.id)
  pages.value = (await pageList(projectId.value!, currentAppId.value)) as any[]
}

async function openLocators(row: any) {
  currentPageId.value = row.id
  currentPageName.value = row.pageName
  locators.value = (await locatorList(row.id)) as any[]
  locatorDialog.value = true
}
async function addLocator() {
  if (!newLoc.locatorName || !newLoc.locatorValue) return
  await locatorSave({ pageObjectId: currentPageId.value, locatorName: newLoc.locatorName, strategy: newLoc.strategy, locatorValue: newLoc.locatorValue })
  newLoc.locatorName = ''
  newLoc.locatorValue = ''
  locators.value = (await locatorList(currentPageId.value!)) as any[]
}
async function removeLocator(row: any) {
  await locatorDelete(row.id)
  locators.value = (await locatorList(currentPageId.value!)) as any[]
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
