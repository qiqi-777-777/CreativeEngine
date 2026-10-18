<template>
  <div class="tab-bar">
    <div class="tab-bar-border"></div>
    <div
      v-for="(item, index) in list"
      :key="index"
      class="tab-bar-item"
      :class="item.isSpecial ? 'special-item' : ''"
      @click="switchTab(item.pagePath)"
    >
      <template v-if="!item.isSpecial">
        <img class="tab-icon" :src="selected === index ? item.selectedIconPath : item.iconPath" />
        <div class="tab-text" :style="{ color: selected === index ? selectedColor : color }">{{ item.text }}</div>
      </template>
      <template v-else>
        <div class="special-icon-wrapper">
          <img class="special-icon" :src="item.iconPath" />
        </div>
        <div class="tab-text special-text" :style="{ color: selected === index ? selectedColor : color }">{{ item.text }}</div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const color = '#999999'
const selectedColor = '#3B9FFF'
const list = [
  { pagePath: '/pages/index/index', iconPath: '/images/icon/home.png', selectedIconPath: '/images/icon/home-active.png', text: '首页' },
  { pagePath: '/pages/ai-assistant/ai-assistant', iconPath: '/images/icon/ai-plus.png', selectedIconPath: '/images/icon/ai-plus-active.png', text: 'AI助手', isSpecial: true },
  { pagePath: '/pages/mine/mine', iconPath: '/images/icon/user.png', selectedIconPath: '/images/icon/user-active.png', text: '我的' }
]

const route = useRoute()
const router = useRouter()
const selected = computed(() => Math.max(0, list.findIndex((i) => i.pagePath === route.path)))

function switchTab(path) {
  if (path !== route.path) router.push(path)
}
</script>
