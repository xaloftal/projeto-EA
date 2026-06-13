<template>
  <label class="stop-picker">
    <span v-if="label" class="stop-picker__label">{{ label }}</span>
    <div class="stop-search-wrapper">
      <input
        v-model="searchText"
        type="text"
        class="stop-picker__input"
        :placeholder="placeholder"
        autocomplete="off"
        @focus="isDropdownOpen = true"
        @blur="onInputBlur"
      />
      <ul v-if="isDropdownOpen" class="stop-dropdown">
        <li
          v-for="stop in filteredStops"
          :key="`${label}-${stop.id}`"
          class="stop-dropdown-item"
          @mousedown.prevent="selectStop(stop)"
        >
          <span class="stop-dropdown-name">
            {{ stop.name }}
            <small v-if="stop.code">({{ stop.code }})</small>
          </span>
          <span v-if="getStopTypeLetter(stop.stopType)" :class="['stop-type-badge', `stop-type-${getStopTypeLetter(stop.stopType)}`]">{{ getStopTypeLetter(stop.stopType) }}</span>
        </li>
        <li v-if="!filteredStops.length" class="stop-dropdown-empty">No stops found.</li>
      </ul>
    </div>
  </label>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { catchitApi } from '../services/api/catchitApi'
import type { Stop } from '../models'

const props = withDefaults(
  defineProps<{
    modelValue?: Stop | null
    label?: string
    placeholder?: string
    stops?: Stop[]
  }>(),
  {
    modelValue: null,
    label: '',
    placeholder: 'Search stop by name',
    stops: undefined,
  },
)

const emit = defineEmits<{
  'update:modelValue': [stop: Stop | null]
}>()

const loadedStops = ref<Stop[]>([])
const searchText = ref('')
const isDropdownOpen = ref(false)

const availableStops = computed(() => props.stops ?? loadedStops.value)

const getStopTypeLetter = (type?: string) => {
  if (!type) return ''
  const t = type.toLowerCase()
  if (t.includes('bus')) return 'B'
  if (t.includes('metro')) return 'M'
  return t.charAt(0).toUpperCase()
}

const normalizeStopName = (value: string) =>
  value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()

const filteredStops = computed(() => {
  const searchTerm = normalizeStopName(searchText.value)
  if (!searchTerm) return availableStops.value.slice(0, 50)
  return availableStops.value
    .filter((stop) => normalizeStopName(stop.name).includes(searchTerm))
    .slice(0, 50)
})

watch(
  () => props.modelValue,
  (stop) => {
    searchText.value = stop?.name ?? ''
  },
  { immediate: true },
)

const selectStop = (stop: Stop) => {
  searchText.value = stop.name
  emit('update:modelValue', stop)
  isDropdownOpen.value = false
}

const onInputBlur = () => {
  window.setTimeout(() => {
    isDropdownOpen.value = false
  }, 150)
}

onMounted(async () => {
  if (props.stops?.length) return
  const response = await catchitApi.getStops()
  if (response.success && response.data) {
    loadedStops.value = response.data
  }
})
</script>

<style scoped>
.stop-picker {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.stop-picker__label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #4b5563;
}

.stop-search-wrapper {
  position: relative;
}

.stop-picker__input {
  width: 100%;
  padding: 0.65rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 0.95rem;
}

.stop-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  max-height: 220px;
  overflow-y: auto;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  list-style: none;
  margin: 0;
  padding: 0.25rem 0;
  z-index: 20;
}

.stop-dropdown-item {
  padding: 0.55rem 0.75rem;
  cursor: pointer;
  font-size: 0.9rem;
}

.stop-dropdown-item:hover {
  background: #f3f4f6;
}

.stop-dropdown-item small {
  color: #6b7280;
  margin-left: 0.35rem;
}

.stop-dropdown-empty {
  padding: 0.75rem;
  color: #9ca3af;
  font-size: 0.85rem;
}

.stop-dropdown-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stop-dropdown-name {
  display: flex;
  align-items: baseline;
  gap: 0.25rem;
}

.stop-type-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.25rem;
  height: 1.25rem;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
}

.stop-type-B { background: #0ea5e9; }
.stop-type-M { background: #ec4899; }
.stop-type-T { background: #f97316; }
</style>
