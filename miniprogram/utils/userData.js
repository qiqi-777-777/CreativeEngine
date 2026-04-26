const app = getApp()

const KEYS = {
  bpDrafts: 'bpDrafts',
  careerRecords: 'careerMatchRecords',
  policyFavorites: 'policyFavorites',
  notifications: 'notifications',
  currentBpDraft: 'bpCurrentDraftId'
}

function getToken() {
  return app.globalData.token || wx.getStorageSync('token') || ''
}

function getUserIdentity() {
  const userInfo = app.globalData.userInfo || wx.getStorageSync('userInfo') || {}
  if (userInfo.id) return `user-${userInfo.id}`
  if (userInfo.username) return `username-${userInfo.username}`
  if (getToken()) return `token-${getToken().slice(0, 12)}`
  return ''
}

function scopedKey(key) {
  const identity = getUserIdentity()
  return identity ? `${key}:${identity}` : key
}

function migrateLegacyList(key) {
  const identity = getUserIdentity()
  if (!identity) return

  const markKey = `${key}:migrated:${identity}`
  if (wx.getStorageSync(markKey)) return

  const current = wx.getStorageSync(scopedKey(key))
  const legacy = wx.getStorageSync(key)
  if ((!Array.isArray(current) || current.length === 0) && Array.isArray(legacy) && legacy.length > 0) {
    wx.setStorageSync(scopedKey(key), legacy)
  }
  wx.setStorageSync(markKey, true)
}

function getLocalList(key, fallback = []) {
  migrateLegacyList(key)
  const list = wx.getStorageSync(scopedKey(key))
  return Array.isArray(list) ? list : fallback
}

function setLocalList(key, list) {
  wx.setStorageSync(scopedKey(key), Array.isArray(list) ? list : [])
}

function getLocalValue(key, fallback = '') {
  const value = wx.getStorageSync(scopedKey(key))
  return value || fallback
}

function setLocalValue(key, value) {
  wx.setStorageSync(scopedKey(key), value)
}

function removeLocalValue(key) {
  wx.removeStorageSync(scopedKey(key))
}

function request(method, url, data) {
  const token = getToken()
  if (!token) return Promise.reject(new Error('no token'))

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.apiBase}${url}`,
      method,
      data: data || {},
      header: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          resolve(res.data.data)
        } else {
          reject(res.data || res)
        }
      },
      fail: reject
    })
  })
}

function upsertLocal(key, item, limit) {
  const list = getLocalList(key)
  const itemId = String(item.id || '')
  const next = [
    item,
    ...list.filter((row) => String(row.id || '') !== itemId)
  ].slice(0, limit || 50)
  setLocalList(key, next)
  return next
}

function removeLocalById(key, id) {
  const itemId = String(id)
  const next = getLocalList(key).filter((row) => String(row.id || '') !== itemId)
  setLocalList(key, next)
  return next
}

function removeLocalFavorite(keyValue) {
  const itemKey = String(keyValue)
  const next = getLocalList(KEYS.policyFavorites).filter((row) => String(row.id || row.policyName) !== itemKey)
  setLocalList(KEYS.policyFavorites, next)
  return next
}

async function getRemoteList(url, key, fallback = [], syncItem = null) {
  const localBefore = getLocalList(key, fallback)
  try {
    const data = await request('GET', url)
    const list = Array.isArray(data) ? data : fallback
    if (list.length === 0 && localBefore.length > 0) {
      if (syncItem) {
        localBefore.forEach((item) => syncItem(item))
      }
      return localBefore
    }
    setLocalList(key, list)
    return list
  } catch (err) {
    return localBefore
  }
}

function getBpDrafts() {
  return getRemoteList('/user-data/bp-drafts', KEYS.bpDrafts, [], (item) => {
    request('POST', '/user-data/bp-drafts', item).catch(() => {})
  })
}

function saveBpDraft(draft) {
  upsertLocal(KEYS.bpDrafts, draft, 30)
  return request('POST', '/user-data/bp-drafts', draft)
    .then((saved) => {
      if (saved && saved.id) upsertLocal(KEYS.bpDrafts, saved, 30)
      return saved || draft
    })
    .catch(() => draft)
}

function deleteBpDraft(id) {
  removeLocalById(KEYS.bpDrafts, id)
  return request('DELETE', `/user-data/bp-drafts/${encodeURIComponent(id)}`)
    .catch(() => true)
}

function getCurrentBpDraftId() {
  return getLocalValue(KEYS.currentBpDraft, '')
}

function setCurrentBpDraftId(id) {
  setLocalValue(KEYS.currentBpDraft, id)
}

function removeCurrentBpDraftId() {
  removeLocalValue(KEYS.currentBpDraft)
}

function getCareerRecords() {
  return getRemoteList('/user-data/career-records', KEYS.careerRecords, [], (item) => {
    request('POST', '/user-data/career-records', item).catch(() => {})
  })
}

function saveCareerRecord(record) {
  upsertLocal(KEYS.careerRecords, record, 20)
  return request('POST', '/user-data/career-records', record)
    .then((saved) => {
      if (saved && saved.id) upsertLocal(KEYS.careerRecords, saved, 20)
      return saved || record
    })
    .catch(() => record)
}

function deleteCareerRecord(id) {
  removeLocalById(KEYS.careerRecords, id)
  return request('DELETE', `/user-data/career-records/${encodeURIComponent(id)}`)
    .catch(() => true)
}

function getPolicyFavorites() {
  return getRemoteList('/user-data/policy-favorites', KEYS.policyFavorites, [], (item) => {
    request('POST', '/user-data/policy-favorites', item).catch(() => {})
  })
}

function savePolicyFavorite(favorite) {
  const key = favorite.id || favorite.policyName
  const list = getLocalList(KEYS.policyFavorites).filter((row) => String(row.id || row.policyName) !== String(key))
  setLocalList(KEYS.policyFavorites, [favorite, ...list])
  return request('POST', '/user-data/policy-favorites', favorite)
    .then((saved) => {
      if (saved) {
        const savedKey = saved.id || saved.policyName
        const next = getLocalList(KEYS.policyFavorites).filter((row) => String(row.id || row.policyName) !== String(savedKey))
        setLocalList(KEYS.policyFavorites, [saved, ...next])
      }
      return saved || favorite
    })
    .catch(() => favorite)
}

function deletePolicyFavorite(key) {
  removeLocalFavorite(key)
  return request('DELETE', `/user-data/policy-favorites?key=${encodeURIComponent(key)}`)
    .catch(() => true)
}

async function getNotifications(defaults = []) {
  const localBefore = getLocalList(KEYS.notifications)
  try {
    const data = await request('GET', '/user-data/notifications')
    if (Array.isArray(data) && data.length > 0) {
      setLocalList(KEYS.notifications, data)
      return data
    }
    if (localBefore.length > 0) {
      localBefore.forEach((item) => saveNotification(item))
      return localBefore
    }
    if (defaults.length > 0) {
      setLocalList(KEYS.notifications, defaults)
      defaults.forEach((item) => saveNotification(item))
      return defaults
    }
    return []
  } catch (err) {
    if (localBefore.length > 0) return localBefore
    setLocalList(KEYS.notifications, defaults)
    return defaults
  }
}

function saveNotification(notification) {
  upsertLocal(KEYS.notifications, notification, 50)
  return request('POST', '/user-data/notifications', notification)
    .catch(() => notification)
}

function markNotificationRead(id) {
  const next = getLocalList(KEYS.notifications).map((item) => (
    String(item.id) === String(id) ? { ...item, read: true } : item
  ))
  setLocalList(KEYS.notifications, next)
  return request('PUT', `/user-data/notifications/${encodeURIComponent(id)}/read`)
    .catch(() => true)
}

function markAllNotificationsRead() {
  const next = getLocalList(KEYS.notifications).map((item) => ({ ...item, read: true }))
  setLocalList(KEYS.notifications, next)
  return request('PUT', '/user-data/notifications/read-all')
    .catch(() => true)
}

async function getStats() {
  const localStats = {
    policyFavorites: getLocalList(KEYS.policyFavorites).length,
    careerRecords: getLocalList(KEYS.careerRecords).length,
    bpDrafts: getLocalList(KEYS.bpDrafts).length,
    notifications: getLocalList(KEYS.notifications).length,
    unreadNotifications: getLocalList(KEYS.notifications).filter((item) => !item.read).length
  }

  try {
    const remoteStats = await request('GET', '/user-data/stats')
    const localTotal = localStats.policyFavorites + localStats.careerRecords + localStats.bpDrafts + localStats.notifications
    const remoteTotal = (remoteStats.policyFavorites || 0) + (remoteStats.careerRecords || 0) + (remoteStats.bpDrafts || 0) + (remoteStats.notifications || 0)
    if (remoteTotal === 0 && localTotal > 0) {
      return localStats
    }
    return remoteStats
  } catch (err) {
    return localStats
  }
}

function clearLocalUserData() {
  Object.values(KEYS).forEach((key) => wx.removeStorageSync(scopedKey(key)))
}

module.exports = {
  KEYS,
  getLocalList,
  setLocalList,
  getBpDrafts,
  saveBpDraft,
  deleteBpDraft,
  getCurrentBpDraftId,
  setCurrentBpDraftId,
  removeCurrentBpDraftId,
  getCareerRecords,
  saveCareerRecord,
  deleteCareerRecord,
  getPolicyFavorites,
  savePolicyFavorite,
  deletePolicyFavorite,
  getNotifications,
  saveNotification,
  markNotificationRead,
  markAllNotificationsRead,
  getStats,
  clearLocalUserData
}
