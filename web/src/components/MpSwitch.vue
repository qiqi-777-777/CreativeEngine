<template>
  <label class="mp-switch" :class="{ checked }">
    <input type="checkbox" :checked="checked" @change="onChange" />
    <span class="mp-switch-dot" />
  </label>
</template>

<script setup>
const props = defineProps({
  checked: { type: Boolean, default: false },
  color: { type: String, default: '#3B9FFF' }
})
const emit = defineEmits(['change'])

function onChange(e) {
  emit('change', { detail: { value: e.target.checked } })
}
</script>
