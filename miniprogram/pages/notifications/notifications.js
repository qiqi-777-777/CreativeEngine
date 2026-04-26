const userData = require('../../utils/userData')

function buildDefaultNotifications() {
  const now = new Date().toISOString()
  return [
    {
      id: 'policy-update',
      title: '政策库已更新',
      type: '政策提醒',
      content: '系统已同步就业创业相关政策，建议查看与你项目匹配的新内容。',
      read: false,
      createdAt: now
    },
    {
      id: 'career-record',
      title: '职业测评结果会自动保存',
      type: '功能提醒',
      content: '完成职业匹配后，可在“职业测评记录”中回看推荐职业和匹配理由。',
      read: false,
      createdAt: now
    },
    {
      id: 'bp-draft',
      title: 'BP 草稿箱已开启',
      type: '功能提醒',
      content: '生成商业计划书后点击保存草稿，即可后续继续编辑和复制。',
      read: true,
      createdAt: now
    }
  ]
}

function formatTime(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  const pad = (num) => String(num).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

Page({
  data: {
    statusBarHeight: 20,
    notifications: [],
    unreadCount: 0
  },

  onLoad() {
    const sysInfo = wx.getSystemInfoSync()
    this.setData({ statusBarHeight: sysInfo.statusBarHeight || 20 })
  },

  onShow() {
    this.loadNotifications()
  },

  goBack() {
    wx.navigateBack()
  },

  async loadNotifications() {
    const notifications = await userData.getNotifications(buildDefaultNotifications())

    const formatted = notifications.map((item) => ({
      ...item,
      timeText: formatTime(item.createdAt)
    }))

    this.setData({
      notifications: formatted,
      unreadCount: formatted.filter((item) => !item.read).length
    })
  },

  async markRead(e) {
    const id = e.currentTarget.dataset.id
    await userData.markNotificationRead(id)
    this.loadNotifications()
  },

  async markAllRead() {
    await userData.markAllNotificationsRead()
    this.loadNotifications()
  }
})
