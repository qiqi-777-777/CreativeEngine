const app = getApp()
const userData = require('../../utils/userData')
const SETTINGS_KEY = 'appSettings'

Page({
  data: {
    statusBarHeight: 20,
    settings: {
      messageEnabled: true
    },
    profileDesc: '未登录',
    storageSize: 0
  },

  onLoad() {
    const sysInfo = wx.getSystemInfoSync()
    this.setData({ statusBarHeight: sysInfo.statusBarHeight || 20 })
  },

  onShow() {
    this.loadSettings()
  },

  goBack() {
    wx.navigateBack()
  },

  loadSettings() {
    const settings = wx.getStorageSync(SETTINGS_KEY) || { messageEnabled: true }
    const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo')
    const storageInfo = wx.getStorageInfoSync()

    this.setData({
      settings,
      profileDesc: userInfo ? (userInfo.nickname || userInfo.username || '已登录') : '登录后可同步个人资料',
      storageSize: storageInfo.currentSize || 0
    })
  },

  toggleMessage(e) {
    const settings = {
      ...this.data.settings,
      messageEnabled: e.detail.value
    }
    wx.setStorageSync(SETTINGS_KEY, settings)
    this.setData({ settings })
  },

  goProfile() {
    if (!app.globalData.isLogin) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }

    wx.navigateTo({ url: '/pages/account-profile/account-profile' })
  },

  goPrivacy() {
    wx.navigateTo({ url: '/pages/privacy-policy/privacy-policy' })
  },

  goAgreement() {
    wx.navigateTo({ url: '/pages/user-agreement/user-agreement' })
  },

  clearCache() {
    wx.showModal({
      title: '清理本地记录',
      content: '确认清空政策收藏、测评记录、BP 草稿和消息通知吗？',
      confirmText: '清理',
      confirmColor: '#dc2626',
      success: (res) => {
        if (!res.confirm) return

        userData.clearLocalUserData()
        this.loadSettings()
        wx.showToast({ title: '已清理', icon: 'success' })
      }
    })
  }
})
