/**
 * 小程序 -> H5 转换脚本
 * 读取 ../miniprogram 下的 WXML / WXSS / JS，生成 Vue 可用的模板、样式与页面选项。
 * 用法: npm run convert
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { compile } from '@vue/compiler-dom'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const WEB_ROOT = path.resolve(__dirname, '..')
const MP_ROOT = path.resolve(WEB_ROOT, '..', 'miniprogram')
const OUT_MP = path.resolve(WEB_ROOT, 'src', 'mp')
const OUT_GEN = path.resolve(WEB_ROOT, 'src', 'generated')
const OUT_PUBLIC = path.resolve(WEB_ROOT, 'public')

const TAG_MAP = {
  view: 'div',
  text: 'span',
  image: 'img',
  'scroll-view': 'div',
  swiper: 'mp-swiper',
  'swiper-item': 'mp-swiper-item',
  block: 'template',
  icon: 'i',
  'checkbox-group': 'div',
  'radio-group': 'div',
  checkbox: 'input',
  switch: 'mp-switch',
  navigator: 'a'
}

const EVENT_MAP = {
  tap: 'click',
  longpress: 'contextmenu',
  input: 'input',
  change: 'change',
  confirm: 'keyup.enter',
  blur: 'blur',
  focus: 'focus',
  touchstart: 'touchstart',
  touchmove: 'touchmove',
  touchend: 'touchend',
  getuserprofile: 'click',
  submit: 'submit'
}

const DROP_ATTRS = new Set([
  'scroll-y', 'scroll-x', 'enable-flex', 'show-scrollbar', 'enhanced', 'bounces',
  'scroll-with-animation', 'upper-threshold', 'lower-threshold', 'scroll-top',
  'scroll-into-view', 'lazy-load', 'webp', 'show-menu-by-longpress',
  'placeholder-class', 'placeholder-style', 'hover-class', 'hover-start-time',
  'hover-stay-time', 'open-type', 'lang', 'session-from', 'show-error',
  'cursor-spacing', 'adjust-position', 'always-embed', 'confirm-type',
  'confirm-hold', 'cursor', 'selection-start', 'selection-end', 'auto-height',
  'fixed', 'adjust-position', 'wx:key'
])

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function emptyDir(dir) {
  if (fs.existsSync(dir)) fs.rmSync(dir, { recursive: true, force: true })
  fs.mkdirSync(dir, { recursive: true })
}

/** ---------- JS 转换 ---------- */
function fixRequirePath(p) {
  let out = p
  if (!/\.(js|json)$/.test(out)) out += '.js'
  return out
}

function convertJs(src) {
  let out = src

  // const { a, b } = require('...')
  let modSeq = 0
  out = out.replace(
    /(?:const|let|var)\s*\{([^}]+)\}\s*=\s*require\(\s*(['"])([^'"]+)\2\s*\)[ \t]*;?/g,
    (_m, names, _q, p) => {
      const v = `__mpMod${++modSeq}`
      return `import ${v} from '${fixRequirePath(p)}'\nconst {${names}} = ${v}\n`
    }
  )

  // const x = require('...')
  out = out.replace(
    /(?:const|let|var)\s+([A-Za-z_$][\w$]*)\s*=\s*require\(\s*(['"])([^'"]+)\2\s*\)[ \t]*;?/g,
    (_m, name, _q, p) => `import ${name} from '${fixRequirePath(p)}'\n`
  )

  // module.exports = { ... } / module.exports = xxx
  out = out.replace(/module\.exports\s*=\s*\{/g, 'export default {')
  out = out.replace(/module\.exports\s*=\s*([A-Za-z_$][\w$]*)\s*;?/g, 'export default $1')

  // Page({...}) / Component({...})：兼容文件前存在注释/import 的情况
  const wrapper = /(^|\r?\n)([ \t]*)(Page|Component)\s*\(\s*\{/.exec(out)
  if (wrapper) {
    const head = out.slice(0, wrapper.index + wrapper[1].length)
    let body = out.slice(wrapper.index + wrapper[0].length)
    body = body.replace(/\}\s*\)\s*;?\s*$/, '}')
    out = head + 'export default {' + body
  }

  return out
}

/** ---------- WXML 转换 ---------- */
function findTagEnd(src, start) {
  let i = start + 1
  let quote = null
  while (i < src.length) {
    const c = src[i]
    if (quote) {
      if (c === quote) quote = null
    } else if (c === '"' || c === "'") {
      quote = c
    } else if (c === '>') {
      return i
    }
    i++
  }
  return -1
}

function parseTagContent(content) {
  const nameMatch = /^\s*([a-zA-Z][\w-]*)/.exec(content)
  if (!nameMatch) return null
  const name = nameMatch[1]
  let i = nameMatch[0].length
  const attrs = []
  while (i < content.length) {
    while (i < content.length && /\s/.test(content[i])) i++
    if (i >= content.length) break
    if (content[i] === '/') { i++; continue }
    const start = i
    while (i < content.length && !/[\s=]/.test(content[i])) i++
    const attrName = content.slice(start, i)
    while (i < content.length && /\s/.test(content[i])) i++
    let value = null
    if (content[i] === '=') {
      i++
      while (i < content.length && /\s/.test(content[i])) i++
      const q = content[i]
      if (q === '"' || q === "'") {
        i++
        const s = i
        while (i < content.length && content[i] !== q) i++
        value = content.slice(s, i)
        i++
      } else {
        const s = i
        while (i < content.length && !/\s/.test(content[i])) i++
        value = content.slice(s, i)
      }
    }
    if (attrName) attrs.push({ name: attrName, value })
  }
  return { name, attrs }
}

function escapeAttr(v) {
  return String(v).replace(/&/g, '&amp;').replace(/"/g, '&quot;')
}

/** 将 a.b 转为 a?.b，避免小程序里对 null 取属性在 Vue 中抛错 */
function safeExpr(expr) {
  let out = ''
  let quote = null
  for (let i = 0; i < expr.length; i++) {
    const c = expr[i]
    if (quote) {
      out += c
      if (c === quote && expr[i - 1] !== '\\') quote = null
      continue
    }
    if (c === '"' || c === "'") {
      quote = c
      out += c
      continue
    }
    if (c === '.') {
      const prev = expr[i - 1] || ''
      const next = expr[i + 1] || ''
      const prevOk = /[\w$\]\)]/.test(prev)
      const nextOk = /[A-Za-z_$]/.test(next)
      const already = expr.slice(i - 2, i) === '?.'
      if (prevOk && nextOk && !already) {
        out += '?.'
        continue
      }
    }
    out += c
  }
  return out
}

function bindAttr(name, value) {
  if (value == null) return name
  if (!value.includes('{{')) return `${name}="${escapeAttr(value)}"`
  const parts = value.split(/(\{\{[\s\S]*?\}\})/).filter((p) => p !== '')
  if (parts.length === 1 && parts[0].startsWith('{{')) {
    return `:${name}="${escapeAttr(safeExpr(parts[0].slice(2, -2).trim()))}"`
  }
  const expr = parts
    .map((p) => (p.startsWith('{{') ? `(${safeExpr(p.slice(2, -2).trim())})` : JSON.stringify(p)))
    .join(' + ')
  return `:${name}="${escapeAttr(expr)}"`
}

function convertTag(name, attrs) {
  const attrMap = {}
  for (const a of attrs) attrMap[a.name] = a.value

  const outName = TAG_MAP[name] || name
  const out = []
  let forExpr = null
  let forItem = 'item'
  let forIndex = 'index'
  let keyExpr = null

  if (attrMap['wx:for'] != null) {
    const raw = attrMap['wx:for']
    forExpr = raw.includes('{{') ? raw.slice(raw.indexOf('{{') + 2, raw.lastIndexOf('}}')).trim() : raw
    if (attrMap['wx:for-item']) forItem = attrMap['wx:for-item']
    if (attrMap['wx:for-index']) forIndex = attrMap['wx:for-index']
    if (attrMap['wx:key'] != null) {
      const k = attrMap['wx:key']
      if (k === '*this') keyExpr = forItem
      else if (k.startsWith('{{')) keyExpr = k.slice(2, -2).trim()
      else keyExpr = `${forItem}.${k}`
    }
  }

  if (attrMap['wx:if'] != null) {
    const raw = attrMap['wx:if']
    const expr = raw.includes('{{') ? raw.slice(raw.indexOf('{{') + 2, raw.lastIndexOf('}}')).trim() : raw
    out.push(`v-if="${escapeAttr(safeExpr(expr))}"`)
  } else if (attrMap['wx:elif'] != null) {
    const raw = attrMap['wx:elif']
    const expr = raw.includes('{{') ? raw.slice(raw.indexOf('{{') + 2, raw.lastIndexOf('}}')).trim() : raw
    out.push(`v-else-if="${escapeAttr(safeExpr(expr))}"`)
  } else if ('wx:else' in attrMap) {
    out.push('v-else')
  }

  if (forExpr != null) {
    out.push(`v-for="(${forItem}, ${forIndex}) in ${escapeAttr(safeExpr(forExpr))}"`)
    if (keyExpr) out.push(`:key="${escapeAttr(keyExpr)}"`)
  }

  for (const { name: an, value } of attrs) {
    if (['wx:for', 'wx:for-item', 'wx:for-index', 'wx:if', 'wx:elif', 'wx:else', 'wx:key'].includes(an)) continue

    // 事件
    const evMatch = /^(capture-)?(bind|catch):?([a-zA-Z]+)$/.exec(an)
    if (evMatch) {
      const isCatch = evMatch[2] === 'catch'
      const evName = EVENT_MAP[evMatch[3]] || evMatch[3]
      if (value == null) continue
      let handler = value.trim()
      if (handler.startsWith('{{') && handler.endsWith('}}')) {
        const expr = handler.slice(2, -2).trim()
        out.push(`@${evName}${isCatch ? '.stop' : ''}="mpH((${escapeAttr(expr)}), $event)"`)
        continue
      }
      out.push(`@${evName}${isCatch ? '.stop' : ''}="mpH('${handler}', $event)"`)
      continue
    }

    if (DROP_ATTRS.has(an)) continue

    // 行内样式中的 rpx 需要转换（静态用 CSS 变量，动态交给运行时的 __rpx）
    if (an === 'style') {
      if (value == null) continue
      if (value.includes('{{')) {
        const parts = value.split(/(\{\{[\s\S]*?\}\})/).filter((p) => p !== '')
        const expr = parts
          .map((p) => (p.startsWith('{{') ? `(${safeExpr(p.slice(2, -2).trim())})` : JSON.stringify(p)))
          .join(' + ')
        out.push(`:style="mpRpx(${escapeAttr(expr)})"`)
      } else {
        out.push(`style="${escapeAttr(value.replace(/(-?\d*\.?\d+)rpx/g, (_m, n) => `calc(var(--rpx) * ${n})`))}"`)
      }
      continue
    }

    // 特殊属性
    if (an === 'mode' && name === 'image') {
      if (value) out.push(`data-mode="${escapeAttr(value)}"`)
      continue
    }
    if (an === 'password') {
      out.push(`:type="${value && value.includes('{{') ? escapeAttr(value.slice(2, -2).trim()) : `'password'`}"`)
      continue
    }
    if (an === 'auto-height') continue
    if (an === 'checked' && name === 'checkbox') {
      out.push(bindAttr(':checked', value))
      continue
    }
    if (an === 'color' && (name === 'checkbox' || name === 'switch')) continue
    if (an === 'scroll-y' || an === 'scroll-x') continue

    out.push(bindAttr(an, value))
  }

  if (name === 'scroll-view') out.push('data-scroll')
  if (name === 'checkbox') out.push('type="checkbox"')

  return { tag: outName, attrs: out }
}

function convertWxml(src) {
  // 提取 wxs
  const wxs = {}
  src = src.replace(/<wxs\s+module="([^"]+)"\s*>([\s\S]*?)<\/wxs>/g, (_m, mod, code) => {
    wxs[mod] = code.trim()
    return ''
  })

  // 注释
  src = src.replace(/<!--[\s\S]*?-->/g, '')

  let out = ''
  let i = 0
  while (i < src.length) {
    const lt = src.indexOf('<', i)
    if (lt === -1) {
      out += src.slice(i)
      break
    }
    out += src.slice(i, lt)
    const next = src[lt + 1] || ''
    // 非标签起始的 '<'（如文本里的 a < b 比较）原样输出
    if (!/[a-zA-Z/!]/.test(next)) {
      out += '<'
      i = lt + 1
      continue
    }
    if (src.startsWith('</', lt)) {
      const gt = src.indexOf('>', lt)
      const name = src.slice(lt + 2, gt).trim()
      const mapped = TAG_MAP[name] || name
      out += `</${mapped}>`
      i = gt + 1
      continue
    }
    const gt = findTagEnd(src, lt)
    if (gt === -1) {
      out += src.slice(lt)
      break
    }
    const inner = src.slice(lt + 1, gt)
    const selfClose = /\/\s*$/.test(inner)
    const parsed = parseTagContent(inner.replace(/\/\s*$/, ''))
    if (!parsed) {
      out += src.slice(lt, gt + 1)
      i = gt + 1
      continue
    }
    const { tag, attrs } = convertTag(parsed.name, parsed.attrs)
    let tagStr = `<${tag}${attrs.length ? ' ' + attrs.join(' ') : ''}`
    if (selfClose) tagStr += ' />'
    else tagStr += '>'
    out += tagStr
    i = gt + 1
  }

  // 文本插值也做空值保护
  out = out.replace(/\{\{([\s\S]*?)\}\}/g, (_m, expr) => `{{${safeExpr(expr)}}}`)

  return { template: out, wxs }
}

/** ---------- WXSS 转换 ---------- */
function rpxToUnit(css) {
  // --rpx 由 base.css 定义：min(1/750 * 100vw, 0.64px)，桌面端限制在 480px 手机框内
  return css.replace(/(-?\d*\.?\d+)rpx/g, (_m, n) => `calc(var(--rpx) * ${n})`)
}

function convertWxss(filePath, seen = new Set()) {
  if (seen.has(filePath)) return ''
  seen.add(filePath)
  let css = fs.readFileSync(filePath, 'utf8')

  // 处理 @import
  css = css.replace(/@import\s+["']([^"']+)["']\s*;/g, (_m, p) => {
    const target = path.resolve(path.dirname(filePath), p)
    if (!fs.existsSync(target)) return ''
    return convertWxss(target, seen)
  })

  css = rpxToUnit(css)
  // 100vw 在手机框内应相对 #app，而不是整个浏览器视口
  css = css.replace(/(?<![\d.])100vw/g, '100%')
  // page 选择器 -> body
  css = css.replace(/(^|[\s,}])page(\s*[{,])/g, '$1body$2')
  return css
}

/** ---------- 入口 ---------- */
function main() {
  const appJson = JSON.parse(fs.readFileSync(path.join(MP_ROOT, 'app.json'), 'utf8'))
  const pages = appJson.pages || []
  const tabPages = new Set((appJson.tabBar?.list || []).map((t) => t.pagePath))

  emptyDir(OUT_MP)
  emptyDir(OUT_GEN)

  // 1. 复制并转换 utils
  const utilsDir = path.join(MP_ROOT, 'utils')
  const utilsOut = path.join(OUT_MP, 'utils')
  ensureDir(utilsOut)
  for (const f of fs.readdirSync(utilsDir)) {
    if (!f.endsWith('.js')) continue
    const src = fs.readFileSync(path.join(utilsDir, f), 'utf8')
    fs.writeFileSync(path.join(utilsOut, f), convertJs(src), 'utf8')
  }

  // 2. 页面
  const views = {}
  const pageOptions = {}

  for (const page of pages) {
    const name = page.split('/').pop()
    const dir = path.join(MP_ROOT, path.dirname(page))
    const jsPath = path.join(dir, `${name}.js`)
    const wxmlPath = path.join(dir, `${name}.wxml`)
    const wxssPath = path.join(dir, `${name}.wxss`)
    const jsonPath = path.join(dir, `${name}.json`)

    // JS
    if (fs.existsSync(jsPath)) {
      const src = fs.readFileSync(jsPath, 'utf8')
      const outDir = path.join(OUT_MP, path.dirname(page))
      ensureDir(outDir)
      fs.writeFileSync(path.join(OUT_MP, page + '.js'), convertJs(src), 'utf8')
    }

    // WXML
    let template = ''
    let wxs = {}
    if (fs.existsSync(wxmlPath)) {
      const src = fs.readFileSync(wxmlPath, 'utf8')
      const r = convertWxml(src)
      template = r.template
      wxs = r.wxs
    }

    // WXSS
    let styles = ''
    if (fs.existsSync(wxssPath)) styles = convertWxss(wxssPath)

    // JSON
    let title = '创享引擎'
    if (fs.existsSync(jsonPath)) {
      try {
        const j = JSON.parse(fs.readFileSync(jsonPath, 'utf8'))
        if (j.navigationBarTitleText) title = j.navigationBarTitleText
      } catch (e) { /* ignore */ }
    }

    views['/' + page] = { template, styles, wxs, title, tab: tabPages.has(page) }
  }

  // 3. 生成 views.js
  const viewsOut = ['// 由 tools/convert-mp.mjs 自动生成，请勿手动修改']
  viewsOut.push('export const views = ' + JSON.stringify(views, null, 0))
  fs.writeFileSync(path.join(OUT_GEN, 'views.js'), viewsOut.join('\n'), 'utf8')

  // 4. 生成 pages.js（页面选项）
  const imports = []
  const entries = []
  for (const page of pages) {
    const varName = 'p' + imports.length
    imports.push(`import ${varName} from '../mp/${page}.js'`)
    entries.push(`  '/${page}': ${varName}`)
  }
  const pagesOut = [
    '// 由 tools/convert-mp.mjs 自动生成，请勿手动修改',
    ...imports,
    '',
    'export const pageOptions = {',
    entries.join(',\n'),
    '}'
  ]
  fs.writeFileSync(path.join(OUT_GEN, 'pages.js'), pagesOut.join('\n'), 'utf8')

  // 5. 全局样式
  const appWxss = path.join(MP_ROOT, 'app.wxss')
  const tabWxss = path.join(MP_ROOT, 'custom-tab-bar', 'index.wxss')
  let globalCss = convertWxss(appWxss)
  if (fs.existsSync(tabWxss)) globalCss += '\n/* custom tab bar */\n' + convertWxss(tabWxss)
  const stylesDir = path.resolve(WEB_ROOT, 'src', 'styles')
  ensureDir(stylesDir)
  fs.writeFileSync(path.join(stylesDir, 'mp-global.css'), globalCss, 'utf8')

  // 6. 图片资源
  emptyDir(path.join(OUT_PUBLIC, 'images'))
  fs.cpSync(path.join(MP_ROOT, 'images'), path.join(OUT_PUBLIC, 'images'), { recursive: true })

  console.log(`转换完成：${pages.length} 个页面`)

  // 7. 模板编译校验
  const errors = []
  for (const [p, v] of Object.entries(views)) {
    try {
      compile(v.template, {
        onError: (e) => errors.push(`${p}: ${e.message}`),
        onWarn: () => {}
      })
    } catch (e) {
      errors.push(`${p}: ${e.message}`)
    }
  }
  if (errors.length) {
    console.log(`模板校验发现 ${errors.length} 个问题：`)
    errors.forEach((e) => console.log('  - ' + e))
  } else {
    console.log('模板编译校验通过')
  }
}

main()
