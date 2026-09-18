import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { login as loginApi, me, getMyAvatar, uploadMyAvatar } from '../api'

/** 登录态：token/用户信息在 localStorage；头像二进制只存在 MySQL，页面通过 /api/auth/avatar 拉取。 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    avatarUrl: '',
    avatarLoaded: false
  }),
  getters: {
    isAdmin: (s) => s.user?.role === 'ADMIN',
    isTeacher: (s) => s.user?.role === 'TEACHER',
    isStudent: (s) => s.user?.role === 'STUDENT',
    /** 优先账号管理里配置的真实姓名 realName，没有才用登录名 */
    displayName: (s) => {
      const real = (s.user?.realName || '').trim()
      if (real) return real
      return (s.user?.username || '').trim()
    }
  },
  actions: {
    persist() {
      localStorage.setItem('token', this.token)
      const slim = this.user ? { ...this.user } : null
      if (slim) delete slim.token
      localStorage.setItem('user', JSON.stringify(slim))
    },
    async login(form) {
      const res = await loginApi(form)
      this.token = res.data.token
      this.user = res.data
      this.persist()
      await this.loadAvatar()
      return this.user
    },
    async loadMe() {
      if (!this.token) return null
      const res = await me()
      this.user = { ...this.user, ...res.data, token: this.token }
      this.persist()
      await this.loadAvatar()
      return this.user
    },
    async loadAvatar() {
      if (!this.token) return
      try {
        const res = await getMyAvatar()
        const blob = res.data
        if (res.status !== 200 || !blob || !blob.size || (blob.type && blob.type.includes('json'))) {
          this.clearAvatarUrl()
          if (this.user) this.user.hasAvatar = false
          this.avatarLoaded = true
          return
        }
        this.clearAvatarUrl()
        this.avatarUrl = URL.createObjectURL(blob)
        if (this.user) this.user.hasAvatar = true
        this.avatarLoaded = true
        this.persist()
      } catch {
        this.clearAvatarUrl()
        this.avatarLoaded = true
      }
    },
    async uploadAvatar(file) {
      if (!file) return
      if (file.size > 2 * 1024 * 1024) {
        ElMessage.error('图片不能超过 2MB')
        return
      }
      await uploadMyAvatar(file)
      if (this.user) this.user.hasAvatar = true
      this.persist()
      await this.loadAvatar()
      ElMessage.success('头像已保存到服务器')
    },
    clearAvatarUrl() {
      if (this.avatarUrl) URL.revokeObjectURL(this.avatarUrl)
      this.avatarUrl = ''
    },
    logout() {
      this.clearAvatarUrl()
      this.avatarLoaded = false
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
