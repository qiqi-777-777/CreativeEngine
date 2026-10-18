import { wx, getApp } from './index'

// 在页面模块加载前安装全局对象（request.js 在模块顶层调用 getApp()）
globalThis.wx = wx
globalThis.getApp = getApp
globalThis.__wxConfig = getApp().globalData

// 小程序页面栈在 H5 中用一个伪栈表示
globalThis.getCurrentPages = () => {
  const path = location.hash.replace(/^#/, '').split('?')[0]
  return [{ route: path.replace(/^\//, ''), options: {} }]
}
