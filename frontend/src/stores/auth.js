import { defineStore } from 'pinia'
import { ref } from 'vue'
import http, { setAuthToken } from '../api/http'

const T = 'bc_token'
const R = 'bc_role'

export const useAuthStore = defineStore('auth', () => {
  const token = ref('')
  const role = ref('')

  function restore() {
    const t = localStorage.getItem(T)
    const r = localStorage.getItem(R)
    if (t) {
      token.value = t
      setAuthToken(t)
    }
    if (r) role.value = r
  }

  function saveSession(t, r) {
    token.value = t
    role.value = r || ''
    localStorage.setItem(T, t)
    localStorage.setItem(R, role.value)
    setAuthToken(t)
  }

  async function login(username, password) {
    const { data } = await http.post('/auth/login', { username, password })
    saveSession(data.token, data.role)
  }

  async function register(username, password) {
    const { data } = await http.post('/auth/register', { username, password })
    saveSession(data.token, data.role)
  }

  function logout() {
    token.value = ''
    role.value = ''
    localStorage.removeItem(T)
    localStorage.removeItem(R)
    setAuthToken(null)
  }

  return { token, role, restore, login, register, logout }
})
