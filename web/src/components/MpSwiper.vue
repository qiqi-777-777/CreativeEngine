<template>
  <div class="mp-swiper">
    <div class="mp-swiper-track" ref="track" @scroll="onScroll">
      <slot />
    </div>
    <div v-if="indicatorDots" class="mp-swiper-dots">
      <i
        v-for="n in count"
        :key="n"
        class="mp-swiper-dot"
        :class="{ active: n - 1 === active }"
        :style="{ background: n - 1 === active ? indicatorActiveColor : indicatorColor }"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'

const props = defineProps({
  indicatorDots: { type: Boolean, default: false },
  indicatorColor: { type: String, default: 'rgba(255,255,255,0.4)' },
  indicatorActiveColor: { type: String, default: '#ffffff' },
  autoplay: { type: Boolean, default: false },
  interval: { type: Number, default: 4000 },
  duration: { type: Number, default: 500 },
  circular: { type: Boolean, default: false },
  vertical: { type: Boolean, default: false }
})

const track = ref(null)
const count = ref(0)
const active = ref(0)
let timer = null

function onScroll() {
  const el = track.value
  if (!el) return
  const w = el.clientWidth || 1
  active.value = Math.round(el.scrollLeft / w)
}

function next() {
  const el = track.value
  if (!el) return
  const w = el.clientWidth
  if (!w) return
  const max = el.scrollWidth - w
  const target = el.scrollLeft + w
  el.scrollTo({ left: target > max + 4 ? 0 : target, behavior: 'smooth' })
}

onMounted(() => {
  const el = track.value
  if (!el) return
  count.value = el.children.length
  if (props.autoplay && count.value > 1) {
    timer = setInterval(next, Math.max(props.interval || 4000, 1000))
  }
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>
