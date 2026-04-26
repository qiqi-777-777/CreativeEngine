const app = getApp()
const avatarUtil = require('../../utils/avatar')

const ROLE_OPTIONS = ['学生', '就业求职者', '创业者', '项目团队', '企业用户']

function cloneUserInfo(userInfo) {
  const source = userInfo || {}
  return {
    id: source.id || '',
    username: source.username || '',
    nickname: source.nickname || '',
    phone: source.phone || '',
    email: source.email || '',
    avatar: source.avatar || '',
    role: source.role || source.identity || '',
    status: source.status
  }
}

function formatTime(value) {
  if (!value) return '未同步'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '未同步'
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

Page({
  data: {
    statusBarHeight: 20,
    form: cloneUserInfo(),
    originalForm: cloneUserInfo(),
    avatarLetter: '',
    roleOptions: ROLE_OPTIONS,
    roleIndex: 0,
    statusText: '正常',
    lastSyncText: '未同步'
  },

  onLoad() {
    const sysInfo = wx.getSystemInfoSync()
    this.setData({ statusBarHeight: sysInfo.statusBarHeight || 20 })
    this.loadProfile()
  },

  goBack() {
    wx.navigateBack()
  },

  loadProfile() {
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo')
    if (!userInfo) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }

    const form = cloneUserInfo(userInfo)
    const roleIndex = Math.max(0, ROLE_OPTIONS.indexOf(form.role))
    const avatarName = form.nickname || form.username || ''
    const lastSyncAt = wx.getStorageSync('profileLastSyncAt')

    this.setData({
      form,
      originalForm: { ...form },
      avatarLetter: avatarName ? avatarName.charAt(0).toUpperCase() : '我',
      roleIndex,
      statusText: form.status === 0 ? '已禁用' : '正常',
      lastSyncText: formatTime(lastSyncAt)
    })
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({
      [`form.${field}`]: e.detail.value
    })
  },

  onRoleChange(e) {
    const index = Number(e.detail.value)
    this.setData({
      roleIndex: index,
      'form.role': ROLE_OPTIONS[index]
    })
  },

  async chooseAvatar() {
    try {
      wx.showLoading({ title: '处理中...' })
      const form = {
        ...this.data.form,
        nickname: this.data.form.nickname || this.data.form.username || '用户'
      }
      const userInfo = await avatarUtil.chooseAndSaveAvatar(form)
      wx.hideLoading()

      if (!userInfo) return

      const nextForm = cloneUserInfo(userInfo)
      const roleIndex = Math.max(0, ROLE_OPTIONS.indexOf(nextForm.role))
      this.setData({
        form: nextForm,
        originalForm: { ...nextForm },
        roleIndex,
        avatarLetter: (nextForm.nickname || nextForm.username || '我').charAt(0).toUpperCase(),
        lastSyncText: formatTime(new Date().toISOString())
      })

      wx.showToast({ title: '头像已更换', icon: 'success' })
    } catch (err) {
      wx.hideLoading()
      wx.showToast({ title: '更换失败', icon: 'none' })
      console.error('更换头像失败:', err)
    }
  },

  resetForm() {
    const form = { ...this.data.originalForm }
    const roleIndex = Math.max(0, ROLE_OPTIONS.indexOf(form.role))
    const avatarName = form.nickname || form.username || ''
    this.setData({
      form,
      roleIndex,
      avatarLetter: avatarName ? avatarName.charAt(0).toUpperCase() : '我'
    })
  },

  validateForm() {
    const { nickname, phone, email } = this.data.form

    if (!nickname || !nickname.trim()) {
      wx.showToast({ title: '昵称不能为空', icon: 'none' })
      return false
    }

    if (phone && !/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '手机号格式不正确', icon: 'none' })
      return false
    }

    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      wx.showToast({ title: '邮箱格式不正确', icon: 'none' })
      return false
    }

    return true
  },

  saveLocalProfile(form) {
    const oldUserInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
    const userInfo = {
      ...oldUserInfo,
      ...form,
      identity: form.role
    }
    const now = new Date().toISOString()

    wx.setStorageSync('userInfo', userInfo)
    wx.setStorageSync('profileLastSyncAt', now)
    app.globalData.userInfo = userInfo

    this.setData({
      originalForm: { ...form },
      avatarLetter: (form.nickname || form.username || '我').charAt(0).toUpperCase(),
      lastSyncText: formatTime(now)
    })
  },

  saveProfile() {
    if (!this.validateForm()) return

    const form = {
      ...this.data.form,
      nickname: this.data.form.nickname.trim(),
      phone: this.data.form.phone.trim(),
      email: this.data.form.email.trim()
    }

    wx.showLoading({ title: '保存中...' })

    wx.request({
      url: `${app.globalData.apiBase}/auth/userInfo`,
      method: 'PUT',
      header: {
        'Content-Type': 'application/json',
        'Authorization': app.globalData.token || wx.getStorageSync('token') || ''
      },
      data: {
        nickname: form.nickname,
        phone: form.phone,
        email: form.email,
        avatar: form.avatar
      },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          this.saveLocalProfile({
            ...form,
            ...(res.data.data || {})
          })
          wx.showToast({ title: '已保存', icon: 'success' })
        } else {
          this.saveLocalProfile(form)
          wx.showToast({ title: '已保存到本地', icon: 'none' })
        }
      },
      fail: () => {
        this.saveLocalProfile(form)
        wx.showToast({ title: '已保存到本地', icon: 'none' })
      },
      complete: () => {
        wx.hideLoading()
      }
    })
  }
})
