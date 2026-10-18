import { createRouter, createWebHashHistory } from 'vue-router'
import { views } from './generated/views.js'
import { pageOptions } from './generated/pages.js'
import { createPage } from './wx/page'
import { setNav } from './wx/nav'

const routes = Object.keys(views).map((path) => ({
  path,
  component: createPage(pageOptions[path] || {}, { ...views[path], path })
}))

routes.push({ path: '/', redirect: '/pages/index/index' })
routes.push({ path: '/:pathMatch(.*)*', redirect: '/pages/index/index' })

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

setNav(router)

export default router
