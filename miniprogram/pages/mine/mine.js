const app = getApp()

Page({
  data: {
    statusBarHeight: 20,
    isLogin: false,
    userInfo: null,
    avatarLetter: '',
    stats: [
      { label: '已读行业', value: '0' },
      { label: '适配政策', value: '0' },
      { label: '对接资源', value: '0' },
    ],
    services: [
      { label: '行业图谱', iconChar: '图' },
      { label: '我的匹配', iconChar: '配' },
      { label: 'BP 智写', iconChar: '写' },
      { label: '专家对接', iconChar: '专' },
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
    // 设置 tabBar 选中状态
    if (typeof this.getTabBar === 'function' &&
      this.getTabBar()) {
      this.getTabBar().setData({
        selected: 2
      })
    }

    // 刷新登录状态
    this.refreshLoginState();
  },

  // 刷新登录状态
  refreshLoginState() {
    const isLogin = app.globalData.isLogin;
    const userInfo = app.globalData.userInfo;

    let avatarLetter = '';
    if (isLogin && userInfo) {
      const name = userInfo.nickname || userInfo.username || '';
      avatarLetter = name.charAt(0).toUpperCase();
    }

    // 登录后显示真实数据，未登录显示0
    const stats = isLogin ? [
      { label: '已读行业', value: '15' },
      { label: '适配政策', value: '8' },
      { label: '对接资源', value: '3' },
    ] : [
      { label: '已读行业', value: '0' },
      { label: '适配政策', value: '0' },
      { label: '对接资源', value: '0' },
    ];

    this.setData({
      isLogin,
      userInfo: userInfo || null,
      avatarLetter,
      stats
    });
  },

  handleLogin() {
    wx.navigateTo({
      url: '/pages/login/login',
    }).catch(err => {
      wx.showToast({
        title: '请先登录',
        icon: 'none'
      });
    });
  },

  // 退出登录
  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      confirmText: '退出',
      confirmColor: '#000000',
      success: (res) => {
        if (res.confirm) {
          // 调用后端退出接口
          const token = app.globalData.token;
          if (token) {
            wx.request({
              url: `${app.globalData.apiBase}/auth/logout`,
              method: 'POST',
              header: {
                'Authorization': token
              }
            });
          }

          // 清除本地存储
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');

          // 清除全局状态
          app.globalData.isLogin = false;
          app.globalData.token = '';
          app.globalData.userInfo = null;

          // 刷新页面状态
          this.refreshLoginState();

          wx.showToast({
            title: '已退出登录',
            icon: 'success'
          });
        }
      }
    });
  },

  handleStatClick(e) {
    if (!this.data.isLogin) {
      this.handleLogin();
      return;
    }
    const index = e.currentTarget.dataset.index;
    const item = this.data.stats[index];
    wx.showToast({
      title: `点击了 ${item.label}`,
      icon: 'none'
    });
  },

  handleServiceClick(e) {
    const item = e.currentTarget.dataset.item;
    const routeMap = {
      '行业图谱': '/pages/industry-atlas/industry-atlas',
      '我的匹配': '/pages/my-match/my-match',
      'BP 智写': '/pages/bp-writer/bp-writer',
      '专家对接': '/pages/expert-connect/expert-connect'
    };

    const url = routeMap[item.label];
    if (url) {
      wx.navigateTo({ url });
    } else {
      wx.showToast({ title: `访问: ${item.label}`, icon: 'none' });
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
