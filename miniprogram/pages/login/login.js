// pages/login/login.js
const app = getApp()

Page({
  data: {
    username: '',
    password: '',
    agreed: false
  },

  onLoad(options) {
    // 检查是否已登录
    if (app.globalData.isLogin) {
      wx.switchTab({
        url: '/pages/index/index'
      })
    }
  },

  // 用户名输入
  onUsernameInput(e) {
    this.setData({
      username: e.detail.value
    })
  },

  // 密码输入
  onPasswordInput(e) {
    this.setData({
      password: e.detail.value
    })
  },


  // 登录
  login() {
    const { username, password, agreed } = this.data

    if (!agreed) {
      wx.showToast({
        title: '请先同意用户协议',
        icon: 'none'
      })
      return
    }

    if (!username || username.length < 3) {
      wx.showToast({
        title: '用户名长度不能少于3位',
        icon: 'none'
      })
      return
    }

    if (!password || password.length < 6) {
      wx.showToast({
        title: '密码长度不能少于6位',
        icon: 'none'
      })
      return
    }

    wx.showLoading({
      title: '登录中...'
    })

    // 登录请求
    wx.request({
      url: `${app.globalData.apiBase}/auth/login`,
      method: 'POST',
      header: {
        'Content-Type': 'application/json'
      },
      data: {
        username: username,
        password: password
      },
      success: (res) => {
        wx.hideLoading()
        
        if (res.data.code === 200) {
          const { token, userInfo } = res.data.data
          
          // 保存登录状态
          wx.setStorageSync('token', token)
          wx.setStorageSync('userInfo', userInfo)
          
          app.globalData.isLogin = true
          app.globalData.token = token
          app.globalData.userInfo = userInfo

          wx.showToast({
            title: '登录成功',
            icon: 'success'
          })

          // 跳转回上一页或首页
          setTimeout(() => {
            const pages = getCurrentPages()
            if (pages.length > 1) {
              wx.navigateBack()
            } else {
              wx.switchTab({
                url: '/pages/index/index'
              })
            }
          }, 1500)
        } else {
          wx.showToast({
            title: res.data.message || '登录失败',
            icon: 'none'
          })
        }
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        })
      }
    })
  },

  // 跳转到注册页面
  goToRegister() {
    wx.navigateTo({
      url: '/pages/register/register'
    })
  },

  // 微信登录
  wechatLogin(e) {
    const { agreed } = this.data

    if (!agreed) {
      wx.showToast({
        title: '请先同意用户协议',
        icon: 'none'
      })
      return
    }

    if (e.detail.userInfo) {
      wx.showLoading({
        title: '登录中...'
      })

      // 获取微信登录凭证
      wx.login({
        success: (loginRes) => {
          if (loginRes.code) {
            // 发送微信登录请求
            wx.request({
              url: `${app.globalData.apiBase}/auth/wechat-login`,
              method: 'POST',
              data: {
                code: loginRes.code,
                userInfo: e.detail.userInfo
              },
              success: (res) => {
                wx.hideLoading()
                
                if (res.data.code === 200) {
                  const { token, userInfo } = res.data.data
                  
                  // 保存登录状态
                  wx.setStorageSync('token', token)
                  wx.setStorageSync('userInfo', userInfo)
                  
                  app.globalData.isLogin = true
                  app.globalData.token = token
                  app.globalData.userInfo = userInfo

                  wx.showToast({
                    title: '登录成功',
                    icon: 'success'
                  })

                  // 跳转回上一页或首页
                  setTimeout(() => {
                    const pages = getCurrentPages()
                    if (pages.length > 1) {
                      wx.navigateBack()
                    } else {
                      wx.switchTab({
                        url: '/pages/index/index'
                      })
                    }
                  }, 1500)
                } else {
                  wx.showToast({
                    title: res.data.message || '登录失败',
                    icon: 'none'
                  })
                }
              },
              fail: () => {
                wx.hideLoading()
                wx.showToast({
                  title: '网络错误',
                  icon: 'none'
                })
              }
            })
          }
        }
      })
    }
  },

  // 同意协议
  onAgreementChange(e) {
    this.setData({
      agreed: e.detail.value.length > 0
    })
  },

  // 显示用户协议
  showUserAgreement() {
    wx.navigateTo({
      url: '/pages/user-agreement/user-agreement'
    })
  },

  // 显示隐私政策
  showPrivacyPolicy() {
    wx.navigateTo({
      url: '/pages/privacy-policy/privacy-policy'
    })
  },

})
