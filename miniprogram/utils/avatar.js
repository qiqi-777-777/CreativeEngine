const app = getApp()

function getToken() {
  return app.globalData.token || wx.getStorageSync('token') || ''
}

function chooseImageFile() {
  return new Promise((resolve, reject) => {
    const onFail = (err) => {
      if (err && /cancel/i.test(String(err.errMsg || ''))) {
        resolve('')
        return
      }
      reject(err)
    }

    if (wx.chooseMedia) {
      wx.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['album', 'camera'],
        sizeType: ['compressed'],
        success: (res) => {
          const file = res.tempFiles && res.tempFiles[0]
          resolve(file ? file.tempFilePath : '')
        },
        fail: onFail
      })
      return
    }

    wx.chooseImage({
      count: 1,
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      success: (res) => {
        resolve(res.tempFilePaths && res.tempFilePaths[0] ? res.tempFilePaths[0] : '')
      },
      fail: onFail
    })
  })
}

function saveImageFile(tempFilePath) {
  if (!tempFilePath) return Promise.resolve('')

  return new Promise((resolve) => {
    if (!wx.saveFile) {
      resolve(tempFilePath)
      return
    }

    wx.saveFile({
      tempFilePath,
      success: (res) => resolve(res.savedFilePath || tempFilePath),
      fail: () => resolve(tempFilePath)
    })
  })
}

function saveLocalUserInfo(userInfo) {
  wx.setStorageSync('profileLastSyncAt', new Date().toISOString())
  wx.setStorageSync('userInfo', userInfo)
  app.globalData.userInfo = userInfo
  app.globalData.isLogin = true
}

function syncUserInfo(userInfo) {
  const token = getToken()
  if (!token) return Promise.resolve(userInfo)

  return new Promise((resolve) => {
    wx.request({
      url: `${app.globalData.apiBase}/auth/userInfo`,
      method: 'PUT',
      header: {
        'Content-Type': 'application/json',
        Authorization: token
      },
      data: {
        nickname: userInfo.nickname || userInfo.username || '用户',
        phone: userInfo.phone || '',
        email: userInfo.email || '',
        avatar: userInfo.avatar || ''
      },
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          const syncedUserInfo = {
            ...userInfo,
            ...res.data.data
          }
          saveLocalUserInfo(syncedUserInfo)
          resolve(syncedUserInfo)
          return
        }
        resolve(userInfo)
      },
      fail: () => resolve(userInfo)
    })
  })
}

async function chooseAndSaveAvatar(extraFields = {}) {
  const tempFilePath = await chooseImageFile()
  if (!tempFilePath) return null

  const avatar = await saveImageFile(tempFilePath)
  const oldUserInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
  const nextUserInfo = {
    ...oldUserInfo,
    ...extraFields,
    avatar
  }

  saveLocalUserInfo(nextUserInfo)
  return syncUserInfo(nextUserInfo)
}

module.exports = {
  chooseAndSaveAvatar,
  saveLocalUserInfo,
  syncUserInfo
}
