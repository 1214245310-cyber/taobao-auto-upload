import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCode, login as apiLogin, getInfo } from '@/api/auth'

// 原系统 clientid（取自前端打包配置）。接自有后端时可替换。
const CLIENT_ID = 'e5cd7e4891bf95d1d19206ce24a7b32e'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<Record<string, unknown>>({})

  async function fetchCode() {
    return await getCode()
  }

  async function login(username: string, password: string, code: string, uuid: string) {
    const res = await apiLogin({ username, password, code, uuid, clientId: CLIENT_ID })
    token.value = res.data.access_token
    localStorage.setItem('token', res.data.access_token)
    return res
  }

  function logout() {
    token.value = ''
    localStorage.removeItem('token')
    userInfo.value = {}
  }

  async function fetchInfo() {
    const res = await getInfo()
    userInfo.value = (res.data && res.data.user) || {}
    return res.data
  }

  return { token, userInfo, fetchCode, login, logout, fetchInfo }
})
