const userData = require('../../utils/userData')

function formatTime(value) {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function toSummary(text) {
  const clean = String(text || '').replace(/\s+/g, ' ').trim()
  return clean.length > 72 ? `${clean.slice(0, 72)}...` : clean || '暂无政策摘要'
}

Page({
  data: {
    statusBarHeight: 20,
    favorites: [],
    recentCount: 0
  },

  onLoad() {
    const sysInfo = wx.getSystemInfoSync()
    this.setData({ statusBarHeight: sysInfo.statusBarHeight || 20 })
  },

  onShow() {
    this.loadFavorites()
  },

  goBack() {
    wx.navigateBack()
  },

  async loadFavorites() {
    const raw = await userData.getPolicyFavorites()
    const now = Date.now()
    const weekMs = 7 * 24 * 60 * 60 * 1000
    const favorites = raw.map((item) => ({
      ...item,
      key: String(item.id || item.policyName),
      savedAtText: formatTime(item.savedAt),
      summary: toSummary(item.displayContent || item.content),
      keywordList: String(item.keywords || '')
        .split(/[、,，]/)
        .map((keyword) => keyword.trim())
        .filter(Boolean)
        .slice(0, 4)
    }))

    this.setData({
      favorites,
      recentCount: raw.filter((item) => item.savedAt && now - new Date(item.savedAt).getTime() <= weekMs).length
    })
  },

  goToPolicy() {
    wx.navigateTo({ url: '/pages/policy/policy' })
  },

  goToPolicyDetail(e) {
    const id = e.currentTarget.dataset.id
    if (!id) {
      wx.showToast({ title: '暂缺政策详情 ID', icon: 'none' })
      return
    }

    wx.navigateTo({ url: `/pages/policy-detail/policy-detail?id=${id}` })
  },

  async removeFavorite(e) {
    const key = String(e.currentTarget.dataset.key)
    await userData.deletePolicyFavorite(key)
    this.loadFavorites()
    wx.showToast({ title: '已取消收藏', icon: 'none' })
  }
})
