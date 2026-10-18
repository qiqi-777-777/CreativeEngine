import { reactive, nextTick } from 'vue'

const styleTags = new Map()

function injectStyle(path, css) {
  removeStyle(path)
  if (!css) return
  const el = document.createElement('style')
  el.setAttribute('data-mp-page', path)
  el.textContent = css
  document.head.appendChild(el)
  styleTags.set(path, el)
}

function removeStyle(path) {
  const el = styleTags.get(path)
  if (el) {
    el.remove()
    styleTags.delete(path)
  }
}

function evalWxs(code) {
  const moduleObj = { exports: {} }
  try {
    // eslint-disable-next-line no-new-func
    const fn = new Function('module', 'exports', `${code}\n;return module.exports;`)
    return fn(moduleObj, moduleObj.exports)
  } catch (e) {
    console.warn('[wxs] 解析失败', e)
    return {}
  }
}

function toMpEvent(e) {
  const target = e.target || {}
  const el = e.currentTarget || target
  const dataset = { ...(el.dataset || {}) }
  let detail
  if (e.detail && typeof e.detail === 'object' && ('value' in e.detail || 'checked' in e.detail)) {
    // 自定义组件（如 mp-switch）已经按小程序格式抛出 detail
    detail = e.detail
  } else if (target.tagName === 'INPUT' && (target.type === 'checkbox' || target.type === 'radio')) {
    detail = { value: target.checked ? [target.value || 'on'] : [] }
  } else {
    detail = { value: el.value, checked: el.checked }
  }
  return {
    type: e.type,
    timeStamp: e.timeStamp,
    detail,
    currentTarget: { id: el.id || '', dataset },
    target: { id: target.id || '', dataset: { ...(target.dataset || {}) }, value: target.value },
    touches: e.touches,
    changedTouches: e.changedTouches
  }
}

export function createPage(options, view) {
  const data = reactive(JSON.parse(JSON.stringify(options?.data || {})))

  // 注入 wxs 模块
  for (const [name, code] of Object.entries(view.wxs || {})) {
    data[name] = evalWxs(code)
  }

  const methods = {}
  const self = {
    data,
    route: view.path.replace(/^\//, ''),
    options: {},
    setData(patch, cb) {
      Object.assign(data, patch || {})
      if (cb) nextTick(cb)
    },
    selectComponent() {
      return null
    },
    triggerEvent() {}
  }

  const ctx = new Proxy(self, {
    get(target, key) {
      if (key in target) return target[key]
      if (key in methods) return methods[key]
      if (key in data) return data[key]
      return undefined
    },
    set(target, key, value) {
      if (key in data) data[key] = value
      else if (key !== 'data') target[key] = value
      return true
    }
  })

  for (const [k, v] of Object.entries(options || {})) {
    if (typeof v === 'function') {
      methods[k] = (...args) => v.apply(ctx, args)
    }
  }

  methods.mpH = (nameOrFn, e) => {
    const fn = typeof nameOrFn === 'function' ? nameOrFn : methods[nameOrFn]
    if (fn) fn(toMpEvent(e))
  }

  // 动态 style 里的 rpx 无法用 CSS 变量表达，这里按手机框宽度换算成 px
  methods.mpRpx = (value) => {
    if (typeof value !== 'string' || value.indexOf('rpx') === -1) return value
    const unit = Math.min(window.innerWidth, 480) / 750
    return value.replace(/(-?\d*\.?\d+)rpx/g, (_m, n) => `${(parseFloat(n) * unit).toFixed(3)}px`)
  }

  return {
    name: view.path,
    template: view.template,
    data() {
      return data
    },
    methods,
    mounted() {
      document.title = view.title || '创享引擎'
      injectStyle(view.path, view.styles)
      const query = this.$route ? { ...this.$route.query } : {}
      self.options = query
      if (methods.onLoad) methods.onLoad(query)
      if (methods.onShow) methods.onShow()
      if (methods.onReady) methods.onReady()
    },
    unmounted() {
      if (methods.onUnload) methods.onUnload()
      removeStyle(view.path)
    }
  }
}
