// pages/career-match-result/career-match-result.js
Page({
  data: {
    occupations: [],
    showDetailModal: false,
    selectedOccupation: {}
  },

  onLoad(options) {
    if (options.data) {
      try {
        const occupations = JSON.parse(decodeURIComponent(options.data))
        this.setData({ occupations })
      } catch (err) {
        console.error('解析数据失败:', err)
        wx.showToast({
          title: '数据加载失败',
          icon: 'none'
        })
      }
    }
  },

  // 查看职业详情
  viewOccupationDetail(e) {
    const occupation = e.currentTarget.dataset.occupation
    this.setData({
      selectedOccupation: occupation,
      showDetailModal: true
    })
  },

  // 隐藏详情
  hideDetail() {
    this.setData({
      showDetailModal: false
    })
  },

  // 阻止事件冒泡
  stopPropagation() {
    // 阻止点击弹窗内容时关闭弹窗
  },

  // 返回
  goBack() {
    wx.navigateBack()
  }
})
