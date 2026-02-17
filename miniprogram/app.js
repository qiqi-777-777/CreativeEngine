// app.js
App({
  onLaunch() {
    // 小程序初始化
    console.log('创享引擎小程序启动')
    
    // 获取系统信息
    const systemInfo = wx.getSystemInfoSync()
    this.globalData.systemInfo = systemInfo
    
    // 检查登录状态
    this.checkLoginStatus()
  },

  // 检查登录状态
  checkLoginStatus() {
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')
    
    if (token) {
      this.globalData.isLogin = true
      this.globalData.token = token
      this.globalData.userInfo = userInfo
    }
  },

  // 全局数据
  globalData: {
    isLogin: false,
    token: '',
    userInfo: null,
    systemInfo: null,
    // 后端API地址（请根据实际情况修改）
    apiBase: 'http://localhost:8080/api'
  }
})

