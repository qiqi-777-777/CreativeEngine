const userData = require('../../utils/userData')

function formatTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function shortText(value) {
  const text = String(value || '').replace(/\s+/g, ' ').trim()
  return text.length > 70 ? `${text.slice(0, 70)}...` : text || '暂无正文内容'
}

Page({
  data: {
    statusBarHeight: 20,
    drafts: [],
    completedCount: 0
  },

  onLoad() {
    const sysInfo = wx.getSystemInfoSync()
    this.setData({ statusBarHeight: sysInfo.statusBarHeight || 20 })
  },

  onShow() {
    this.loadDrafts()
  },

  goBack() {
    wx.navigateBack()
  },

  async loadDrafts() {
    const raw = await userData.getBpDrafts()
    const drafts = raw.map((draft) => ({
      ...draft,
      projectName: draft.projectName || '未命名项目',
      status: draft.content ? '已生成' : '编辑中',
      updatedAtText: formatTime(draft.updatedAt || draft.createdAt),
      summary: shortText(draft.oneLiner || draft.content)
    }))

    this.setData({
      drafts,
      completedCount: raw.filter((draft) => Boolean(draft.content)).length
    })
  },

  goToWriter() {
    wx.navigateTo({ url: '/pages/bp-writer/bp-writer' })
  },

  openDraft(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/bp-writer/bp-writer?draftId=${id}` })
  },

  copyDraft(e) {
    const id = e.currentTarget.dataset.id
    const draft = this.data.drafts.find((item) => item.id === id)
    if (!draft || !draft.content) {
      wx.showToast({ title: '暂无可复制正文', icon: 'none' })
      return
    }

    wx.setClipboardData({
      data: draft.content,
      success: () => wx.showToast({ title: '已复制', icon: 'success' })
    })
  },

  async removeDraft(e) {
    const id = e.currentTarget.dataset.id
    await userData.deleteBpDraft(id)
    this.loadDrafts()
    wx.showToast({ title: '已删除草稿', icon: 'none' })
  }
})
