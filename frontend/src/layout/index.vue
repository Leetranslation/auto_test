<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="brand">通用自动化测试平台</div>
      <el-menu :default-active="activeMenu" router background-color="#fff" text-color="#344054" active-text-color="#2563eb">
        <el-menu-item index="/dashboard">首页</el-menu-item>
        <el-menu-item index="/project">项目管理</el-menu-item>
        <el-menu-item index="/env">环境管理</el-menu-item>
        <el-menu-item index="/api">接口测试</el-menu-item>
        <el-menu-item index="/case">测试用例</el-menu-item>
        <el-menu-item index="/ui">UI自动化</el-menu-item>
        <el-menu-item index="/scene">场景编排</el-menu-item>
        <el-menu-item index="/exec">执行与报告</el-menu-item>
        <el-menu-item index="/schedule">定时任务</el-menu-item>
        <el-menu-item index="/suite">测试套件</el-menu-item>
        <el-menu-item index="/system/user">用户管理</el-menu-item>
        <el-menu-item index="/system/role">角色管理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="user">{{ userStore.displayName || userStore.username }}</span>
        <el-button link type="primary" @click="handleLogout">退出</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  border-right: 1px solid #e5eaf2;
}
.brand {
  height: 56px;
  line-height: 56px;
  text-align: center;
  font-weight: 700;
  color: #2563eb;
  border-bottom: 1px solid #e5eaf2;
}
.header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  border-bottom: 1px solid #e5eaf2;
}
</style>
