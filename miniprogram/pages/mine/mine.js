const app = getApp()

Page({
  data: {
    statusBarHeight: 20, // 默认值，onLoad会更新
    stats: [
      { label: '我的收藏', value: '12' },
      { label: '浏览足迹', value: '86' },
      { label: '订阅政策', value: '5' },
    ],
    services: [
      { label: '政策申报', color: '#6366F1', bg: '#EEF2FF', iconChar: '政' }, // Indigo
      { label: '职业匹配', color: '#0EA5E9', bg: '#E0F2FE', iconChar: '职' }, // Sky
      { label: '找场地', color: '#10B981', bg: '#ECFDF5', iconChar: '场' }, // Emerald
      { label: '找资金', color: '#F59E0B', bg: '#FFFBEB', iconChar: '资' }, // Amber
    ],
    menuItems: [
      { label: '我的申请', badge: '2', icon: 'file-text' },
      { label: '企业档案', icon: 'briefcase' },
      { label: '资质认证', text: '去认证', icon: 'award' },
      { label: '消息通知', icon: 'message-square' },
      { label: '帮助与客服', icon: 'help-circle' },
      { label: '设置', icon: 'settings' },
    ]
  },

  onLoad: function () {
    // 获取系统状态栏高度
    const sysInfo = wx.getSystemInfoSync();
    this.setData({
      statusBarHeight: sysInfo.statusBarHeight
    });
  },

  onShow() {
    if (typeof this.getTabBar === 'function' &&
      this.getTabBar()) {
      this.getTabBar().setData({
        selected: 2
      })
    }
  },

  handleLogin() {
    wx.navigateTo({
      url: '/pages/login/login',
    }).catch(err => {
      // 如果没有 login 页面，提示
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      });
    });
  },

  handleStatClick(e) {
    const index = e.currentTarget.dataset.index;
    const item = this.data.stats[index];
    wx.showToast({
      title: `点击了 ${item.label}`,
      icon: 'none'
    });
  },

  handleServiceClick(e) {
    const item = e.currentTarget.dataset.item;
    wx.showToast({
      title: `访问: ${item.label}`,
      icon: 'none'
    });

    // 简单的路由逻辑示例
    if (item.label === '职业匹配') {
      wx.navigateTo({ url: '/pages/career-match/career-match' });
    }
  },

  handleMenuClick(e) {
    const item = e.currentTarget.dataset.item;
    wx.showToast({
      title: `点击了 ${item.label}`,
      icon: 'none'
    });
  }
})
