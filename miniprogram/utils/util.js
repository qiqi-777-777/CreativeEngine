/**
 * 工具函数库
 */

/**
 * 格式化时间
 * @param {Date} date 日期对象
 * @param {String} format 格式化字符串
 * @returns {String}
 */
function formatTime(date, format = 'YYYY-MM-DD HH:mm:ss') {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()

  const formatNumber = n => n < 10 ? '0' + n : n

  return format
    .replace('YYYY', year)
    .replace('MM', formatNumber(month))
    .replace('DD', formatNumber(day))
    .replace('HH', formatNumber(hour))
    .replace('mm', formatNumber(minute))
    .replace('ss', formatNumber(second))
}

/**
 * 格式化日期为相对时间
 * @param {String|Date} dateStr 日期字符串或日期对象
 * @returns {String}
 */
function formatRelativeTime(dateStr) {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  const month = 30 * day
  const year = 365 * day

  if (diff < minute) {
    return '刚刚'
  } else if (diff < hour) {
    return Math.floor(diff / minute) + '分钟前'
  } else if (diff < day) {
    return Math.floor(diff / hour) + '小时前'
  } else if (diff < month) {
    return Math.floor(diff / day) + '天前'
  } else if (diff < year) {
    return Math.floor(diff / month) + '个月前'
  } else {
    return Math.floor(diff / year) + '年前'
  }
}

/**
 * 防抖函数
 * @param {Function} func 要防抖的函数
 * @param {Number} wait 等待时间（毫秒）
 * @returns {Function}
 */
function debounce(func, wait = 500) {
  let timeout
  return function() {
    const context = this
    const args = arguments
    
    clearTimeout(timeout)
    timeout = setTimeout(() => {
      func.apply(context, args)
    }, wait)
  }
}

/**
 * 节流函数
 * @param {Function} func 要节流的函数
 * @param {Number} wait 等待时间（毫秒）
 * @returns {Function}
 */
function throttle(func, wait = 500) {
  let previous = 0
  return function() {
    const now = Date.now()
    const context = this
    const args = arguments
    
    if (now - previous > wait) {
      func.apply(context, args)
      previous = now
    }
  }
}

/**
 * 深拷贝
 * @param {Object} obj 要拷贝的对象
 * @returns {Object}
 */
function deepClone(obj) {
  if (obj === null || typeof obj !== 'object') {
    return obj
  }
  
  if (obj instanceof Date) {
    return new Date(obj.getTime())
  }
  
  if (obj instanceof Array) {
    return obj.map(item => deepClone(item))
  }
  
  if (obj instanceof Object) {
    const clonedObj = {}
    for (let key in obj) {
      if (obj.hasOwnProperty(key)) {
        clonedObj[key] = deepClone(obj[key])
      }
    }
    return clonedObj
  }
}

/**
 * 获取URL参数
 * @param {String} url URL字符串
 * @param {String} name 参数名
 * @returns {String}
 */
function getQueryString(url, name) {
  const reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)', 'i')
  const r = url.split('?')[1].match(reg)
  if (r != null) {
    return decodeURIComponent(r[2])
  }
  return null
}

/**
 * 验证手机号
 * @param {String} phone 手机号
 * @returns {Boolean}
 */
function validatePhone(phone) {
  return /^1[3-9]\d{9}$/.test(phone)
}

/**
 * 验证邮箱
 * @param {String} email 邮箱
 * @returns {Boolean}
 */
function validateEmail(email) {
  return /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/.test(email)
}

/**
 * 验证身份证号
 * @param {String} idCard 身份证号
 * @returns {Boolean}
 */
function validateIdCard(idCard) {
  return /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/.test(idCard)
}

/**
 * 存储数据到本地
 * @param {String} key 键
 * @param {Any} value 值
 */
function setStorage(key, value) {
  try {
    wx.setStorageSync(key, value)
    return true
  } catch (e) {
    console.error('setStorage error:', e)
    return false
  }
}

/**
 * 从本地读取数据
 * @param {String} key 键
 * @returns {Any}
 */
function getStorage(key) {
  try {
    return wx.getStorageSync(key)
  } catch (e) {
    console.error('getStorage error:', e)
    return null
  }
}

/**
 * 删除本地数据
 * @param {String} key 键
 */
function removeStorage(key) {
  try {
    wx.removeStorageSync(key)
    return true
  } catch (e) {
    console.error('removeStorage error:', e)
    return false
  }
}

/**
 * 显示loading
 * @param {String} title 提示文字
 */
function showLoading(title = '加载中...') {
  wx.showLoading({
    title: title,
    mask: true
  })
}

/**
 * 隐藏loading
 */
function hideLoading() {
  wx.hideLoading()
}

/**
 * 显示toast提示
 * @param {String} title 提示文字
 * @param {String} icon 图标类型
 */
function showToast(title, icon = 'none') {
  wx.showToast({
    title: title,
    icon: icon,
    duration: 2000
  })
}

module.exports = {
  formatTime,
  formatRelativeTime,
  debounce,
  throttle,
  deepClone,
  getQueryString,
  validatePhone,
  validateEmail,
  validateIdCard,
  setStorage,
  getStorage,
  removeStorage,
  showLoading,
  hideLoading,
  showToast
}

