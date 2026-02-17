// 网络请求工具类
const app = getApp()

/**
 * 发起网络请求
 * @param {Object} options 请求配置
 * @returns {Promise}
 */
function request(options) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: app.globalData.apiBase + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': app.globalData.token ? `Bearer ${app.globalData.token}` : '',
        ...options.header
      },
      success: (res) => {
        if (res.statusCode === 200) {
          // 请求成功
          if (res.data.code === 0 || res.data.success) {
            resolve(res.data)
          } else {
            // 业务错误
            wx.showToast({
              title: res.data.message || '请求失败',
              icon: 'none'
            })
            reject(res.data)
          }
        } else if (res.statusCode === 401) {
          // 未授权，需要登录
          wx.showModal({
            title: '提示',
            content: '登录已过期，请重新登录',
            confirmText: '去登录',
            success: (modalRes) => {
              if (modalRes.confirm) {
                // 清除登录信息
                app.globalData.isLogin = false
                app.globalData.token = ''
                wx.removeStorageSync('token')
                
                // 跳转到登录页
                // wx.navigateTo({
                //   url: '/pages/login/login'
                // })
              }
            }
          })
          reject(res)
        } else {
          // 其他错误
          wx.showToast({
            title: '网络请求失败',
            icon: 'none'
          })
          reject(res)
        }
      },
      fail: (err) => {
        wx.showToast({
          title: '网络连接失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

/**
 * GET请求
 */
function get(url, data = {}) {
  return request({
    url: url,
    method: 'GET',
    data: data
  })
}

/**
 * POST请求
 */
function post(url, data = {}) {
  return request({
    url: url,
    method: 'POST',
    data: data
  })
}

/**
 * PUT请求
 */
function put(url, data = {}) {
  return request({
    url: url,
    method: 'PUT',
    data: data
  })
}

/**
 * DELETE请求
 */
function del(url, data = {}) {
  return request({
    url: url,
    method: 'DELETE',
    data: data
  })
}

/**
 * 上传文件
 */
function uploadFile(filePath, url, name = 'file', formData = {}) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: app.globalData.apiBase + url,
      filePath: filePath,
      name: name,
      formData: formData,
      header: {
        'Authorization': app.globalData.token ? `Bearer ${app.globalData.token}` : ''
      },
      success: (res) => {
        const data = JSON.parse(res.data)
        if (data.code === 0 || data.success) {
          resolve(data)
        } else {
          wx.showToast({
            title: data.message || '上传失败',
            icon: 'none'
          })
          reject(data)
        }
      },
      fail: (err) => {
        wx.showToast({
          title: '上传失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

module.exports = {
  request,
  get,
  post,
  put,
  del,
  uploadFile
}

