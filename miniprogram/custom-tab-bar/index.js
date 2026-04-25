// custom-tab-bar/index.js
Component({
  data: {
    selected: 0,
    color: '#999999',
    selectedColor: '#3B9FFF',
    list: [
      {
        pagePath: '/pages/index/index',
        iconPath: '/images/icon/home.png',
        selectedIconPath: '/images/icon/home-active.png',
        text: '首页'
      },
      {
        pagePath: '/pages/ai-assistant/ai-assistant',
        iconPath: '/images/icon/ai-plus.png',
        selectedIconPath: '/images/icon/ai-plus-active.png',
        text: 'AI助手',
        isSpecial: true
      },
      {
        pagePath: '/pages/mine/mine',
        iconPath: '/images/icon/user.png',
        selectedIconPath: '/images/icon/user-active.png',
        text: '我的'
      }
    ]
  },
  methods: {
    switchTab(e) {
      const { path } = e.currentTarget.dataset
      wx.switchTab({ url: path })
    }
  }
})
