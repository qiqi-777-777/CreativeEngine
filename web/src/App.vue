<template>
  <router-view />
  <MpTabBar v-if="showTab" />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import MpTabBar from './components/MpTabBar.vue'
import { views } from './generated/views.js'

const route = useRoute()
const showTab = computed(() => {
  const v = views[route.path]
  return !!(v && v.tab)
})
</script>
