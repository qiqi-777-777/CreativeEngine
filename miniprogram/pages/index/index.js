// pages/index/index.js
const app = getApp()
const { getPolicyList } = require('../../utils/api')

Page({
  data: {
    bannerList: [],
    policyList: [], // 新增：政策列表数据
    greeting: '',   // 新增：问候语
  },

  onLoad() {
    this.setGreeting()
    this.loadBannerList()
    this.loadPolicyList() // 新增：加载政策
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
      this.loadBannerList(),
      this.loadPolicyList()
    ]).then(() => {
      wx.stopPullDownRefresh()
    })
  },

  // 设置问候语
  setGreeting() {
    const hour = new Date().getHours()
    let greeting = '你好'
    if (hour < 6) greeting = '夜深了'
    else if (hour < 9) greeting = '早上好'
    else if (hour < 12) greeting = '上午好'
    else if (hour < 14) greeting = '中午好'
    else if (hour < 18) greeting = '下午好'
    else greeting = '晚上好'
    
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
                item.imageUrl = baseUrl + item.imageUrl
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

  // 加载推荐政策（取前3条）
  loadPolicyList() {
    return getPolicyList({ page: 1, size: 3 }).then(res => {
      if (res.code === 200 && res.data && res.data.records) {
        this.setData({
          policyList: res.data.records
        })
      }
    }).catch(err => {
      console.error('加载政策失败', err)
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

  goToPolicyDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/policy-detail/policy-detail?id=${id}`
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
