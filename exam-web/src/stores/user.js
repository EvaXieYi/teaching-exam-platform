import { defineStore } from 'pinia'
import { login as loginApi, me } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  getters: {
    isAdmin: (s) => s.user?.role === 'ADMIN',
    isTeacher: (s) => s.user?.role === 'TEACHER',
    isStudent: (s) => s.user?.role === 'STUDENT'
  },
  actions: {
    async login(form) {
      const res = await loginApi(form)
      this.token = res.data.token
      this.user = res.data
      localStorage.setItem('token', this.token)
      localStorage.setItem('user', JSON.stringify(this.user))
      return this.user
    },
    async loadMe() {
      if (!this.token) return null
      const res = await me()
      this.user = { ...this.user, ...res.data, token: this.token }
      localStorage.setItem('user', JSON.stringify(this.user))
      return this.user
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
    homePath() {
      if (this.user?.role === 'STUDENT') return '/student'
      return '/admin'
    }
  }
})
