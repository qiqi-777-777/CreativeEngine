import './wx/install'
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

import './styles/base.css'
import './styles/mp-global.css'

import MpSwiper from './components/MpSwiper.vue'
import MpSwiperItem from './components/MpSwiperItem.vue'
import MpSwitch from './components/MpSwitch.vue'

const app = createApp(App)
app.component('mp-swiper', MpSwiper)
app.component('mp-swiper-item', MpSwiperItem)
app.component('mp-switch', MpSwitch)
app.use(router)
app.mount('#app')
