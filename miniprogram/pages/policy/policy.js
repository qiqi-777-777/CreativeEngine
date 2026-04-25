// pages/policy/policy.js
const app = getApp()
const { formatPolicy, parseSearchTerms } = require('../../utils/policyText')

Page({
  data: {
    policyList: [],
    filteredPolicyList: [],
    searchKeyword: ''
  },

  onLoad() {
    this.loadPolicyList()
  },

  loadPolicyList(keyword = '') {
    wx.showLoading({ title: '加载中...' })

    const query = keyword && keyword.trim()
      ? `?keyword=${encodeURIComponent(keyword.trim())}`
      : ''

    wx.request({
      url: `${app.globalData.apiBase}/policy-data/list${query}`,
      method: 'GET',
      success: (res) => {
        if (res.data.code === 200) {
          const policyList = (res.data.data || []).map(formatPolicy)
          this.setData({
            policyList,
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

  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({ searchKeyword: keyword })
    this.filterPolicyList(keyword)
  },

  onSearch() {
    this.loadPolicyList(this.data.searchKeyword)
  },

  onClearSearch() {
    this.setData({
      searchKeyword: '',
      filteredPolicyList: this.data.policyList
    })
    this.loadPolicyList()
  },

  filterPolicyList(keyword) {
    const { policyList } = this.data

    if (!keyword || !keyword.trim()) {
      this.setData({ filteredPolicyList: policyList })
      return
    }

    const terms = parseSearchTerms(keyword).map((term) => term.toLowerCase())
    if (terms.length === 0) {
      this.setData({ filteredPolicyList: policyList })
      return
    }

    const filtered = policyList.filter((policy) => {
      const text = `${policy.policyName || ''} ${policy.keywords || ''} ${policy.displayContent || ''} ${policy.content || ''}`.toLowerCase()
      return terms.every((term) => text.includes(term))
    })

    this.setData({ filteredPolicyList: filtered })
  },

  onSelectPolicy(e) {
    const policyId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/policy-detail/policy-detail?id=${policyId}`
    })
  }
})
