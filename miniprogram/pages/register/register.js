// pages/register/register.js
const app = getApp()

Page({
  data: {
    username: '',
    password: '',
    confirmPassword: '',
    nickname: '',
    email: '',
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

  // 确认密码输入
  onConfirmPasswordInput(e) {
    this.setData({
      confirmPassword: e.detail.value
    })
  },

  // 昵称输入
  onNicknameInput(e) {
    this.setData({
      nickname: e.detail.value
    })
  },

  // 邮箱输入
  onEmailInput(e) {
    this.setData({
      email: e.detail.value
    })
  },

  // 注册
  register() {
    const { username, password, confirmPassword, nickname, email, agreed } = this.data

    if (!agreed) {
      wx.showToast({
        title: '请先同意用户协议',
        icon: 'none'
      })
      return
    }

    if (!this.validateUsername(username)) {
      return
    }

    if (!this.validatePassword(password)) {
      return
    }

    if (password !== confirmPassword) {
      wx.showToast({
        title: '两次输入的密码不一致',
        icon: 'none'
      })
      return
    }

    if (email && !this.validateEmail(email)) {
      wx.showToast({
        title: '邮箱格式不正确',
        icon: 'none'
      })
      return
    }

    wx.showLoading({
      title: '注册中...'
    })

    // 注册请求
    wx.request({
      url: `${app.globalData.apiBase}/auth/register`,
      method: 'POST',
      header: {
        'Content-Type': 'application/json'
      },
      data: {
        username: username,
        password: password,
        nickname: nickname || username,
        email: email
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
            title: '注册成功',
            icon: 'success'
          })

          // 跳转到首页
          setTimeout(() => {
            wx.switchTab({
              url: '/pages/index/index'
            })
          }, 1500)
        } else {
          wx.showToast({
            title: res.data.message || '注册失败',
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

  // 跳转到登录页面
  goToLogin() {
    wx.navigateBack()
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

  // 验证用户名
  validateUsername(username) {
    if (!username) {
      wx.showToast({
        title: '用户名不能为空',
        icon: 'none'
      })
      return false
    }

    if (username.length < 3 || username.length > 20) {
      wx.showToast({
        title: '用户名长度应在3-20个字符之间',
        icon: 'none'
      })
      return false
    }

    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
      wx.showToast({
        title: '用户名只能包含字母、数字和下划线',
        icon: 'none'
      })
      return false
    }

    return true
  },

  // 验证密码
  validatePassword(password) {
    if (!password) {
      wx.showToast({
        title: '密码不能为空',
        icon: 'none'
      })
      return false
    }

    if (password.length < 6) {
      wx.showToast({
        title: '密码长度不能少于6位',
        icon: 'none'
      })
      return false
    }

    return true
  },

  // 验证邮箱
  validateEmail(email) {
    const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return emailReg.test(email)
  },

})
