// pages/index/index.js
const app = getApp()
const { getPolicyList } = require('../../utils/api')

Page({
  data: {
    bannerList: [],
    greeting: '',   // 新增：问候语
  },

  onLoad() {
    this.setGreeting()
    this.loadBannerList()
  },

  onShow() {
    if (typeof this.getTabBar === 'function' &&
      this.getTabBar()) {
      this.getTabBar().setData({
        selected: 0
      })
    }
  },

  onPullDownRefresh() {
    // 并行刷新数据
    Promise.all([
      this.loadBannerList()
    ]).then(() => {
      wx.stopPullDownRefresh()
    })
  },

  // 设置问候语
  setGreeting() {
    const hour = new Date().getHours()
    let greeting = 'Good Evening'
    if (hour < 6) greeting = 'Good Night'
    else if (hour < 12) greeting = 'Good Morning'
    else if (hour < 18) greeting = 'Good Afternoon'
    else greeting = 'Good Evening'

    this.setData({ greeting })
  },

  // 加载轮播图列表
  loadBannerList() {
    return new Promise((resolve) => {
      wx.request({
        url: `${app.globalData.apiBase}/banner/list`,
        method: 'GET',
        success: (res) => {
          if (res.data.code === 200 && res.data.data && res.data.data.length > 0) {
            const bannerList = res.data.data.map(item => {
              if (item.imageUrl && item.imageUrl.startsWith('/')) {
                const baseUrl = app.globalData.apiBase.replace('/api', '')
                // 追加时间戳防止微信开发者工具缓存旧图片
                item.imageUrl = baseUrl + item.imageUrl + '?t=' + new Date().getTime()
              }
              return item
            })
            this.setData({ bannerList })
          }
          resolve()
        },
        fail: (err) => {
          console.error('加载轮播图失败:', err)
          resolve()
        }
      })
    })
  },

  // 页面跳转函数
  goToPolicy() {
    wx.switchTab({
      url: '/pages/policy/policy',
      fail: () => {
        // 如果不是 tab 页面，则使用 navigateTo
        wx.navigateTo({ url: '/pages/policy/policy' })
      }
    })
  },

  goToImageInterpretation() {
    wx.navigateTo({ url: '/pages/image-interpretation/image-interpretation' })
  },

  goToCareerMatch() {
    wx.navigateTo({ url: '/pages/career-match/career-match' })
  },

  goToAiAssistant() {
    wx.navigateTo({ url: '/pages/ai-assistant/ai-assistant' })
  },

  goToResource() {
    wx.navigateTo({ url: '/pages/resource/resource' })
  },

  onShareAppMessage() {
    return {
      title: '创享引擎 - 青年新质生产力平台',
      path: '/pages/index/index'
    }
  }
})
