// custom-tab-bar/index.js
Component({
  data: {
    selected: 0,
    color: "#999999",
    selectedColor: "#3B9FFF",
    list: [
      {
        pagePath: "/pages/index/index",
        iconPath: "/images/icon/home.svg",
        selectedIconPath: "/images/icon/home-active.svg",
        text: "首页"
      },
      {
        pagePath: "/pages/ai-assistant/ai-assistant",
        iconPath: "/images/icon/ai-plus.svg",
        selectedIconPath: "/images/icon/ai-plus.svg",
        text: "AI助手",
        isSpecial: true // 标记为特殊按钮
      },
      {
        pagePath: "/pages/mine/mine",
        iconPath: "/images/icon/user.svg",
        selectedIconPath: "/images/icon/user-active.svg",
        text: "我的"
      }
    ]
  },
  methods: {
    switchTab(e) {
      const data = e.currentTarget.dataset
      const url = data.path
      wx.switchTab({url})
      // setData由目标页面onShow调用，这里不需要设置，否则会有闪烁
    }
  }
})