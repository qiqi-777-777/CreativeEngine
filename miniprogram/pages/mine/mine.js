const app = getApp()
const userData = require('../../utils/userData')
const avatarUtil = require('../../utils/avatar')

Page({
  data: {
    statusBarHeight: 20,
    isLogin: false,
    userInfo: null,
    avatarLetter: '',
    stats: [
      { label: '政策收藏', value: '0', desc: '已关注', route: '/pages/policy-favorites/policy-favorites' },
      { label: '测评记录', value: '0', desc: '职业方向', route: '/pages/career-records/career-records' },
      { label: 'BP 草稿', value: '0', desc: '待完善', route: '/pages/bp-drafts/bp-drafts' },
    ],
    services: [
      { label: '行业图谱', iconChar: '图' },
      { label: '我的匹配', iconChar: '配' },
      { label: 'BP 智写', iconChar: '写' },
      { label: '专家对接', iconChar: '专' },
    ],
    menuItems: [
      { label: '我的政策收藏' },
      { label: '职业测评记录' },
      { label: 'BP 草稿箱' },
      { label: '消息通知' },
      { label: '设置' }
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

    const stats = this.buildStats();

    this.setData({
      isLogin,
      userInfo: userInfo || null,
      avatarLetter,
      stats
    });

    if (isLogin) {
      userData.getStats().then((serverStats) => {
        this.setData({ stats: this.buildStats(serverStats) });
      });
    }
  },

  buildStats(counts = null) {
    const favorites = userData.getLocalList(userData.KEYS.policyFavorites);
    const careerRecords = userData.getLocalList(userData.KEYS.careerRecords);
    const bpDrafts = userData.getLocalList(userData.KEYS.bpDrafts);

    return [
      {
        label: '政策收藏',
        value: String(counts ? counts.policyFavorites || 0 : favorites.length),
        desc: '已关注',
        route: '/pages/policy-favorites/policy-favorites'
      },
      {
        label: '测评记录',
        value: String(counts ? counts.careerRecords || 0 : careerRecords.length),
        desc: '职业方向',
        route: '/pages/career-records/career-records'
      },
      {
        label: 'BP 草稿',
        value: String(counts ? counts.bpDrafts || 0 : bpDrafts.length),
        desc: '待完善',
        route: '/pages/bp-drafts/bp-drafts'
      }
    ];
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

  async chooseAvatar() {
    if (!this.data.isLogin) {
      this.handleLogin();
      return;
    }

    try {
      wx.showLoading({ title: '处理中...' });
      const userInfo = await avatarUtil.chooseAndSaveAvatar();
      wx.hideLoading();

      if (!userInfo) return;

      this.refreshLoginState();
      wx.showToast({
        title: '头像已更换',
        icon: 'success'
      });
    } catch (err) {
      wx.hideLoading();
      wx.showToast({
        title: '更换失败',
        icon: 'none'
      });
      console.error('更换头像失败:', err);
    }
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
    if (item && item.route) {
      wx.navigateTo({ url: item.route });
    }
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
    const routeMap = {
      '我的政策收藏': '/pages/policy-favorites/policy-favorites',
      '职业测评记录': '/pages/career-records/career-records',
      'BP 草稿箱': '/pages/bp-drafts/bp-drafts',
      '消息通知': '/pages/notifications/notifications',
      '设置': '/pages/settings/settings'
    };

    const url = routeMap[item.label];
    if (url) {
      wx.navigateTo({ url });
    } else {
      wx.showToast({
        title: `点击了 ${item.label}`,
        icon: 'none'
      });
    }
  }
})
