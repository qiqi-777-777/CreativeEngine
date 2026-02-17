// pages/policy/policy.js
const app = getApp()

Page({
  data: {
    policyList: [],           // 政策列表
    filteredPolicyList: [],   // 过滤后的政策列表
    searchKeyword: ''         // 搜索关键词
  },

  onLoad() {
    this.loadPolicyList()
  },

  // 加载政策列表
  loadPolicyList() {
    wx.showLoading({ title: '加载中...' })
    
    wx.request({
      url: `${app.globalData.apiBase}/policy-data/list`,
      method: 'GET',
      success: (res) => {
        console.log('政策列表响应:', res.data)
        if (res.data.code === 200) {
          const policyList = res.data.data || []
          this.setData({
            policyList: policyList,
            filteredPolicyList: policyList
          })
        } else {
          wx.showToast({
            title: res.data.message || '加载失败',
            icon: 'none'
          })
        }
      },
      fail: (err) => {
        console.error('加载政策列表失败:', err)
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        })
      },
      complete: () => {
        wx.hideLoading()
      }
    })
  },
  
  // 搜索输入
  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({ searchKeyword: keyword })
    this.filterPolicyList(keyword)
  },
  
  // 执行搜索
  onSearch() {
    this.filterPolicyList(this.data.searchKeyword)
  },
  
  // 清空搜索
  onClearSearch() {
    this.setData({ 
      searchKeyword: '',
      filteredPolicyList: this.data.policyList
    })
  },
  
  // 过滤政策列表
  filterPolicyList(keyword) {
    const { policyList } = this.data
    
    if (!keyword || !keyword.trim()) {
      this.setData({ filteredPolicyList: policyList })
      return
    }
    
    const filtered = policyList.filter(policy => {
      return policy.policyName.toLowerCase().includes(keyword.toLowerCase())
    })
    
    this.setData({ filteredPolicyList: filtered })
  },

  // 选择政策
  onSelectPolicy(e) {
    const policyId = e.currentTarget.dataset.id
    // 跳转到政策详情页
    wx.navigateTo({
      url: `/pages/policy-detail/policy-detail?id=${policyId}`
    })
  }
})

