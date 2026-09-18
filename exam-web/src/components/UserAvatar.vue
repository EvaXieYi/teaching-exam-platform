<template>
  <span class="wrap" :class="{ clickable }" :title="clickable ? '点击更换头像' : ''" @click="pick">
    <img v-if="src" :src="src" class="img" :class="size" alt="" />
    <span v-else class="img placeholder" :class="size">{{ initial }}</span>
    <input v-if="clickable" ref="input" type="file" accept="image/jpeg,image/png,image/webp,image/gif" hidden @change="onFile" />
  </span>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useUserStore } from '../stores/user'

const props = defineProps({
  size: { type: String, default: 'md' },
  clickable: { type: Boolean, default: true }
})
const store = useUserStore()
const input = ref()
const src = computed(() => store.avatarUrl || '')
const initial = computed(() => {
  const name = store.displayName || '?'
  return name.slice(0, 1)
})

function pick() {
  if (props.clickable) input.value?.click()
}
async function onFile(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (file) await store.uploadAvatar(file)
}
</script>

<style scoped>
.wrap { display: inline-flex; line-height: 0; position: relative; }
.wrap.clickable { cursor: pointer; }
.img { border-radius: 50%; object-fit: cover; background: #e0f2fe; display: block; flex-shrink: 0; }
.img.sm { width: 28px; height: 28px; }
.img.md { width: 40px; height: 40px; }
.img.lg { width: 72px; height: 72px; box-shadow: 0 0 0 3px rgba(255,255,255,0.35); }
.placeholder {
  display: grid;
  place-items: center;
  color: #fff;
  background: #2563eb;
  font-weight: 700;
  line-height: 1;
}
.placeholder.sm { font-size: 12px; }
.placeholder.md { font-size: 16px; }
.placeholder.lg { font-size: 28px; }
</style>
