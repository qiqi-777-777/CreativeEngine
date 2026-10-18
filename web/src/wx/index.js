import { getNav } from './nav'

/* ---------------- 存储 ---------------- */
const storage = {
  get(key) {
    const v = localStorage.getItem(key)
    if (v == null) return ''
    try {
      return JSON.parse(v)
    } catch (e) {
      return v
    }
  },
  set(key, value) {
    if (value === undefined) {
      localStorage.removeItem(key)
      return
    }
    localStorage.setItem(key, JSON.stringify(value))
  },
  remove(key) {
    localStorage.removeItem(key)
  },
  clear() {
    localStorage.clear()
  }
}

/* ---------------- 全局 app ---------------- */
const globalData = {
  isLogin: false,
  userInfo: null,
  systemInfo: {
    windowWidth: 390,
    windowHeight: 844,
    screenWidth: 390,
    screenHeight: 844,
    pixelRatio: 2,
    statusBarHeight: 44,
    safeArea: { top: 44, bottom: 810, left: 0, right: 390, width: 390, height: 766 },
    platform: 'devtools',
    system: 'H5',
    brand: 'web',
    model: 'browser'
  },
  apiBase: '/api'
}

Object.defineProperty(globalData, 'token', {
  get() {
    return storage.get('token') || ''
  },
  set(v) {
    storage.set('token', v)
  },
  enumerable: true,
  configurable: true
})

Object.defineProperty(globalData, 'isLogin', {
  get() {
    return !!storage.get('token')
  },
  set(v) {
    /* 由 token 决定，忽略写入 */
  },
  enumerable: true,
  configurable: true
})

const app = {
  globalData,
  onLaunch() {},
  onShow() {},
  onHide() {},
  checkLoginStatus() {
    const token = storage.get('token')
    if (token) {
      globalData.userInfo = storage.get('userInfo') || null
    }
  }
}

export function getApp() {
  return app
}

/* ---------------- 路由 ---------------- */
function parseUrl(url) {
  const [pathPart, queryPart] = String(url).split('?')
  const query = {}
  if (queryPart) {
    for (const pair of queryPart.split('&')) {
      if (!pair) continue
      const [k, v] = pair.split('=')
      query[decodeURIComponent(k)] = decodeURIComponent(v || '')
    }
  }
  return { path: pathPart.startsWith('/') ? pathPart : '/' + pathPart, query }
}

function go(url, mode) {
  const router = getNav()
  if (!router) return
  const { path, query } = parseUrl(url)
  if (mode === 'replace') router.replace({ path, query })
  else if (mode === 'back') router.back()
  else router.push({ path, query })
}

/* ---------------- UI ---------------- */
let toastTimer = null
function showToast(opts = {}) {
  const { title = '', duration = 1500, icon = 'none' } = opts
  let el = document.getElementById('mp-toast')
  if (!el) {
    el = document.createElement('div')
    el.id = 'mp-toast'
    el.className = 'mp-toast'
    document.body.appendChild(el)
  }
  el.innerHTML = `${icon === 'success' ? '<div class="mp-toast-icon">✓</div>' : ''}<div class="mp-toast-text"></div>`
  el.querySelector('.mp-toast-text').textContent = title
  el.style.display = 'flex'
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    el.style.display = 'none'
  }, duration)
  opts.success && opts.success()
  opts.complete && opts.complete()
  return { then: (fn) => Promise.resolve().then(fn) }
}

function showLoading(opts = {}) {
  let el = document.getElementById('mp-loading')
  if (!el) {
    el = document.createElement('div')
    el.id = 'mp-loading'
    el.className = 'mp-loading'
    el.innerHTML = '<div class="mp-loading-spinner"></div>'
    document.body.appendChild(el)
  }
  el.style.display = 'flex'
  const text = opts.title
  if (text) {
    const t = document.createElement('div')
    t.className = 'mp-loading-text'
    t.textContent = text
    el.appendChild(t)
  }
}

function hideLoading() {
  const el = document.getElementById('mp-loading')
  if (el) el.style.display = 'none'
}

function showModal(opts = {}) {
  return new Promise((resolve) => {
    const mask = document.createElement('div')
    mask.className = 'mp-modal-mask'
    mask.innerHTML = `
      <div class="mp-modal">
        <div class="mp-modal-title"></div>
        <div class="mp-modal-content"></div>
        <div class="mp-modal-actions">
          <button class="mp-modal-cancel">取消</button>
          <button class="mp-modal-confirm">确定</button>
        </div>
      </div>`
    mask.querySelector('.mp-modal-title').textContent = opts.title || '提示'
    mask.querySelector('.mp-modal-content').textContent = opts.content || ''
    const close = (confirm) => {
      mask.remove()
      const res = { confirm, cancel: !confirm }
      opts.success && opts.success(res)
      opts.complete && opts.complete()
      resolve(res)
    }
    mask.querySelector('.mp-modal-confirm').onclick = () => close(true)
    mask.querySelector('.mp-modal-cancel').onclick = () => close(false)
    if (opts.showCancel === false) mask.querySelector('.mp-modal-cancel').style.display = 'none'
    document.body.appendChild(mask)
  })
}

/* ---------------- 网络 ---------------- */
function queryString(data) {
  return Object.keys(data)
    .filter((k) => data[k] !== undefined && data[k] !== null && data[k] !== '')
    .map((k) => `${encodeURIComponent(k)}=${encodeURIComponent(typeof data[k] === 'object' ? JSON.stringify(data[k]) : data[k])}`)
    .join('&')
}

function request(opts = {}) {
  const method = (opts.method || 'GET').toUpperCase()
  let url = opts.url
  const headers = { ...(opts.header || {}) }
  const init = { method, headers }

  if (method === 'GET') {
    if (opts.data && Object.keys(opts.data).length) {
      const qs = queryString(opts.data)
      if (qs) url += (url.includes('?') ? '&' : '?') + qs
    }
  } else {
    headers['Content-Type'] = headers['Content-Type'] || 'application/json'
    init.body = typeof opts.data === 'string' ? opts.data : JSON.stringify(opts.data || {})
  }

  fetch(url, init)
    .then(async (res) => {
      const text = await res.text()
      let body = text
      try {
        body = JSON.parse(text)
      } catch (e) { /* 保留原始文本 */ }
      opts.success && opts.success({ statusCode: res.status, data: body, header: {} })
      opts.complete && opts.complete()
    })
    .catch((err) => {
      opts.fail && opts.fail(err)
      opts.complete && opts.complete()
    })
}

/* ---------------- 文件 ---------------- */
const fileRegistry = new Map()
let fileSeq = 0

function registerFile(file) {
  const key = `h5-file-${++fileSeq}`
  fileRegistry.set(key, file)
  return key
}

function pickFiles(opts = {}) {
  const count = opts.count || 1
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/*'
  if (count > 1) input.multiple = true
  input.style.display = 'none'
  document.body.appendChild(input)
  input.onchange = () => {
    const files = Array.from(input.files || [])
    const paths = files.map(registerFile)
    const res = {
      tempFilePaths: paths,
      tempFiles: files.map((f, i) => ({ path: paths[i], size: f.size, name: f.name, type: f.type }))
    }
    opts.success && opts.success(res)
    opts.complete && opts.complete()
    input.remove()
  }
  input.click()
}

function uploadFile(opts = {}) {
  const file = fileRegistry.get(opts.filePath)
  const fd = new FormData()
  if (file) fd.append(opts.name || 'file', file, file.name || 'upload.jpg')
  Object.entries(opts.formData || {}).forEach(([k, v]) => fd.append(k, v))
  fetch(opts.url, {
    method: 'POST',
    body: fd,
    headers: { ...(opts.header || {}) }
  })
    .then(async (res) => {
      const text = await res.text()
      opts.success && opts.success({ statusCode: res.status, data: text })
      opts.complete && opts.complete()
    })
    .catch((err) => {
      opts.fail && opts.fail(err)
      opts.complete && opts.complete()
    })
}

export const wx = {
  /* 路由 */
  navigateTo: (o = {}) => go(o.url, 'push'),
  redirectTo: (o = {}) => go(o.url, 'replace'),
  reLaunch: (o = {}) => go(o.url, 'replace'),
  switchTab: (o = {}) => go(o.url, 'push'),
  navigateBack: (o = {}) => {
    const router = getNav()
    if (o.delta && o.delta > 1) router.go(-o.delta)
    else if (window.history.length > 1) router.back()
    else go('/pages/index/index', 'replace')
  },

  /* 存储 */
  getStorageSync: (k) => storage.get(k),
  setStorageSync: (k, v) => storage.set(k, v),
  removeStorageSync: (k) => storage.remove(k),
  clearStorageSync: () => storage.clear(),
  getStorageInfoSync: () => {
    const keys = Object.keys(localStorage)
    let size = 0
    keys.forEach((k) => {
      size += (localStorage.getItem(k) || '').length
    })
    return { keys, currentSize: Math.ceil(size / 1024), limitSize: 10240 }
  },
  getStorage: (o = {}) => { o.success && o.success({ data: storage.get(o.key) }) },
  setStorage: (o = {}) => { storage.set(o.key, o.data); o.success && o.success() },
  removeStorage: (o = {}) => { storage.remove(o.key); o.success && o.success() },

  /* UI */
  showToast,
  hideToast: () => {
    const el = document.getElementById('mp-toast')
    if (el) el.style.display = 'none'
  },
  showLoading,
  hideLoading,
  showModal,
  stopPullDownRefresh: () => {},
  setNavigationBarTitle: (o = {}) => { if (o.title) document.title = o.title },

  /* 系统 */
  getSystemInfoSync: () => globalData.systemInfo,
  getSystemInfo: (o = {}) => { o.success && o.success(globalData.systemInfo) },
  getWindowInfo: () => globalData.systemInfo,

  /* 网络 */
  request,
  uploadFile,

  /* 媒体 */
  chooseImage: pickFiles,
  chooseMedia: (o = {}) => pickFiles(o),
  saveFile: (o = {}) => {
    o.success && o.success({ savedFilePath: o.tempFilePath })
    return Promise.resolve({ savedFilePath: o.tempFilePath })
  },
  previewImage: () => {},

  /* 其它 */
  login: (o = {}) => {
    const err = { errMsg: 'login:fail 微信登录仅支持小程序' }
    o.fail && o.fail(err)
    o.complete && o.complete(err)
  },
  nextTick: (fn) => Promise.resolve().then(fn),
  makePhoneCall: () => {},
  setClipboardData: (o = {}) => {
    if (navigator.clipboard) navigator.clipboard.writeText(o.data || '')
    o.success && o.success()
  }
}
