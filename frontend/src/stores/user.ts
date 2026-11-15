import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const displayName = ref(localStorage.getItem('displayName') || '')

  async function login(form: { username: string; password: string }) {
    const data = await loginApi(form)
    token.value = data.token
    username.value = data.username
    displayName.value = data.displayName
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('displayName', data.displayName || '')
  }

  function logout() {
    token.value = ''
    username.value = ''
    displayName.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('displayName')
  }

  return { token, username, displayName, login, logout }
})
