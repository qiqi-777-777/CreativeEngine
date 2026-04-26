const userData = require('../../utils/userData')

function formatTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

Page({
  data: {
    statusBarHeight: 20,
    records: [],
    topCareerCount: 0
  },

  onLoad() {
    const sysInfo = wx.getSystemInfoSync()
    this.setData({ statusBarHeight: sysInfo.statusBarHeight || 20 })
  },

  onShow() {
    this.loadRecords()
  },

  goBack() {
    wx.navigateBack()
  },

  async loadRecords() {
    const raw = await userData.getCareerRecords()
    const records = raw.map((record, index) => {
      const results = record.results || []
      const topNames = results.slice(0, 3).map((item) => item.name || item.title).filter(Boolean)
      const title = topNames.length > 0 ? `${topNames[0]} 等职业方向` : `职业测评 ${index + 1}`
      return {
        ...record,
        title,
        count: results.length,
        topNames,
        createdAtText: formatTime(record.createdAt),
        summary: topNames.length > 0 ? `本次推荐重点方向：${topNames.join('、')}` : '本次测评暂无推荐职业'
      }
    })

    this.setData({
      records,
      topCareerCount: raw.reduce((total, record) => total + ((record.results || []).length), 0)
    })
  },

  goToCareerMatch() {
    wx.navigateTo({ url: '/pages/career-match/career-match' })
  },

  openRecord(e) {
    const index = Number(e.currentTarget.dataset.index)
    const record = this.data.records[index]
    if (!record) return

    wx.navigateTo({
      url: `/pages/career-match-result/career-match-result?fromRecord=1&data=${encodeURIComponent(JSON.stringify(record.results || []))}`
    })
  },

  async removeRecord(e) {
    const id = e.currentTarget.dataset.id
    await userData.deleteCareerRecord(id)
    this.loadRecords()
    wx.showToast({ title: '已删除记录', icon: 'none' })
  }
})
