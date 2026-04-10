<template>
  <div class="search-container" :style="containerStyle">
    <div ref="mapContainer" class="map" :style="mapStyle"></div>

    <section ref="topOverlayRef" class="top-overlay">
      <div class="search-box">
        <Search class="icon-sm" />
        <input
          v-model="stopQuery"
          type="text"
          class="search-input"
          placeholder="Search stops"
          @focus="onSearchFocus"
          @input="onSearchInput"
          @keyup.enter="focusFirstMatch"
        />
      </div>

      <ul v-if="visibleSuggestions" class="suggestions-list">
        <li v-for="stop in filteredStops" :key="stop.id" class="suggestion-item">
          <button type="button" class="suggestion-btn" @click="focusStop(stop.id)">
            <span>{{ stop.name }}</span>
            <small>{{ stop.id }}</small>
          </button>
        </li>
      </ul>

      <div class="chip-row">
        <button class="chip-btn">Filter</button>
        <button class="chip-btn">Sort</button>
        <p class="results-count">{{ resultCount }} results</p>
      </div>

    </section>

    <section
      v-if="stops.length > 0"
      ref="bottomSheetRef"
      class="bottom-sheet"
      :class="{ 'is-dragging': isDragging }"
      :style="bottomSheetStyle"
      @pointerdown="onSheetPointerDown"
    >
      <div class="drag-handle"></div>

      <div v-if="sheetMode === 'stop' && selectedStop" class="sheet-header">
        <h2>{{ selectedStop.name }}</h2>
        <span class="provider-badge">TUB</span>
      </div>

      <template v-if="sheetMode === 'stop' && selectedStop">
        <p class="line-name">Bus stop information</p>
        <p class="ids-line">Stop ID: {{ selectedStop.id }}</p>
        <p class="next-stop">Next Stop: {{ nextStopName }}</p>

        <h3 class="route-title">Routes at this stop</h3>
        <div
          v-for="route in stopRouteInfo"
          :key="route.routeId"
          class="route-item"
        >
          <span>{{ route.lineLabel }} ({{ route.busId }})</span>
          <span class="route-time">
            {{ route.nextTime }}
            <small class="route-eta">{{ route.etaLabel }}</small>
          </span>
        </div>
      </template>

      <template v-else-if="sheetMode === 'bus' && selectedBus">
        <div class="sheet-header">
          <h2>Bus {{ selectedBus.lineLabel }}</h2>
          <span class="provider-badge">TUB</span>
        </div>

        <p class="line-name">Bus information</p>
        <p class="ids-line">Bus ID: {{ selectedBus.busId }}</p>
        <p class="next-stop">Next Stop: {{ selectedBus.nextStopName }}</p>

        <h3 class="route-title">Arrival Estimate</h3>
        <div class="route-item">
          <span>{{ selectedBus.nextStopName }}</span>
          <span>{{ selectedBus.etaLabel }}</span>
        </div>
      </template>

    </section>

    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><House /></router-link>
      <router-link to="/map" class="nav-item active"><MapIcon /></router-link>
      <router-link to="/cards" class="nav-item"><ShoppingCart /></router-link>
      <router-link to="/notifications" class="nav-item"><Bell /></router-link>
      <router-link to="/profile" class="nav-item"><User /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { Bell, House, Map as MapIcon, Search, ShoppingCart, User } from 'lucide-vue-next'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

type StopFeature = {
  id: string
  name: string
  latitude: number
  longitude: number
  code: string
}

type RouteDefinition = {
  routeId: string
  lineLabel: string
  busId: string
  stopIds: string[]
  firstDeparture: string
  intervalMinutes: number
  travelBetweenStopsMinutes: number
}

type ActiveBus = {
  busId: string
  lineLabel: string
  routeId: string
  latitude: number
  longitude: number
  nextStopId: string
  nextStopName: string
  etaMinutes: number
  etaLabel: string
}

const mapContainer = ref<HTMLElement | null>(null)
const topOverlayRef = ref<HTMLElement | null>(null)
const bottomSheetRef = ref<HTMLElement | null>(null)
const stopQuery = ref('')
const showSuggestions = ref(false)
const stops = ref<StopFeature[]>([])
const selectedStopId = ref<string>('')
const selectedBusId = ref<string>('')
const sheetMode = ref<'stop' | 'bus'>('stop')
const isDragging = ref(false)
const isSheetExpanded = ref(false)
const sheetHeight = ref(0)
const topOverlayHeight = ref(0)
const dragOffset = ref(0)
const dragStartY = ref(0)
const dragStartOffset = ref(0)
const nowTick = ref(Date.now())

let map: L.Map | null = null
let stopMarkersLayer: L.LayerGroup | null = null
let busMarkersLayer: L.LayerGroup | null = null
let tickIntervalId: number | null = null
const stopMarkers = new Map<string, L.Marker>()
const busMarkers = new Map<string, L.Marker>()

const routeDefinitions: RouteDefinition[] = [
  {
    routeId: 'route_43',
    lineLabel: '43',
    busId: 'bus_43',
    stopIds: ['stop_1', 'stop_6', 'stop_7', 'stop_3'],
    firstDeparture: '06:35',
    intervalMinutes: 30,
    travelBetweenStopsMinutes: 6,
  },
  {
    routeId: 'route_22',
    lineLabel: '22',
    busId: 'bus_22',
    stopIds: ['stop_2', 'stop_5', 'stop_4'],
    firstDeparture: '06:20',
    intervalMinutes: 40,
    travelBetweenStopsMinutes: 8,
  },
  {
    routeId: 'route_68',
    lineLabel: '68',
    busId: 'bus_68',
    stopIds: ['stop_4', 'stop_3'],
    firstDeparture: '06:10',
    intervalMinutes: 35,
    travelBetweenStopsMinutes: 10,
  },
]

const filteredStops = computed(() => {
  const query = stopQuery.value.trim().toLowerCase()
  if (!query) return []
  return stops.value
    .filter((stop) => stop.name.toLowerCase().includes(query))
    .slice(0, 8)
})

const selectedStop = computed(() =>
  stops.value.find((stop) => stop.id === selectedStopId.value) ?? null
)

const stopById = computed(() => {
  const byId = new Map<string, StopFeature>()
  for (const stop of stops.value) {
    byId.set(stop.id, stop)
  }
  return byId
})

const toMinutes = (time: string) => {
  const [hours, minutes] = time.split(':').map((part) => Number(part))
  return hours * 60 + minutes
}

const toClock = (valueInMinutes: number) => {
  const normalized = ((valueInMinutes % 1440) + 1440) % 1440
  const hours = Math.floor(normalized / 60)
  const minutes = normalized % 60
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`
}

const nowMinutes = computed(() => {
  const now = new Date(nowTick.value)
  return now.getHours() * 60 + now.getMinutes()
})

const stopRouteInfo = computed(() => {
  if (!selectedStop.value) {
    return [] as Array<{
      routeId: string
      lineLabel: string
      busId: string
      nextTime: string
      etaLabel: string
    }>
  }

  const selectedId = selectedStop.value.id
  const nowMin = nowMinutes.value

  return routeDefinitions
    .filter((route) => route.stopIds.includes(selectedId))
    .map((route) => {
      const stopIndex = route.stopIds.indexOf(selectedId)
      const firstAtStop = toMinutes(route.firstDeparture) + stopIndex * route.travelBetweenStopsMinutes

      let next = firstAtStop
      while (next < nowMin) {
        next += route.intervalMinutes
      }

      const etaMinutes = next - nowMin
      const etaLabel = etaMinutes <= 0 ? 'due now' : `in ${etaMinutes} min`

      return {
        routeId: route.routeId,
        lineLabel: route.lineLabel,
        busId: route.busId,
        nextTime: toClock(next),
        etaLabel,
      }
    })
})

const activeBuses = computed(() => {
  const nowMin = nowMinutes.value
  const active: ActiveBus[] = []

  for (const route of routeDefinitions) {
    const routeDuration = (route.stopIds.length - 1) * route.travelBetweenStopsMinutes
    const firstStart = toMinutes(route.firstDeparture)

    let start = firstStart
    while (start + routeDuration < nowMin) {
      start += route.intervalMinutes
    }

    if (nowMin < start || nowMin > start + routeDuration) {
      continue
    }

    const elapsed = nowMin - start
    const segmentIndex = Math.min(
      Math.floor(elapsed / route.travelBetweenStopsMinutes),
      route.stopIds.length - 2
    )
    const withinSegment = elapsed - segmentIndex * route.travelBetweenStopsMinutes
    const fraction = Math.min(Math.max(withinSegment / route.travelBetweenStopsMinutes, 0), 1)

    const fromStop = stopById.value.get(route.stopIds[segmentIndex])
    const toStop = stopById.value.get(route.stopIds[segmentIndex + 1])
    if (!fromStop || !toStop) continue

    const latitude = fromStop.latitude + (toStop.latitude - fromStop.latitude) * fraction
    const longitude = fromStop.longitude + (toStop.longitude - fromStop.longitude) * fraction
    const etaMinutes = Math.max(1, Math.ceil((1 - fraction) * route.travelBetweenStopsMinutes))

    active.push({
      busId: route.busId,
      lineLabel: route.lineLabel,
      routeId: route.routeId,
      latitude,
      longitude,
      nextStopId: toStop.id,
      nextStopName: toStop.name,
      etaMinutes,
      etaLabel: `${etaMinutes} min`,
    })
  }

  return active
})

const selectedBus = computed(() =>
  activeBuses.value.find((bus) => bus.busId === selectedBusId.value) ?? null
)

const nextStopName = computed(() => {
  if (!selectedStop.value || stops.value.length < 2) return 'No next stop available'
  const selectedIndex = stops.value.findIndex((stop) => stop.id === selectedStop.value?.id)
  const nextIndex = (selectedIndex + 1) % stops.value.length
  return stops.value[nextIndex]?.name ?? 'No next stop available'
})

const resultCount = computed(() =>
  stopQuery.value.trim() ? filteredStops.value.length : stops.value.length
)

const visibleSuggestions = computed(() =>
  showSuggestions.value && stopQuery.value.trim().length > 0 && filteredStops.value.length > 0
)

const collapsedPeek = 88
const minimumAnimatedSheetHeight = 300

const effectiveSheetHeight = computed(() =>
  Math.max(sheetHeight.value, minimumAnimatedSheetHeight)
)

const collapsedOffset = computed(() =>
  Math.max(0, effectiveSheetHeight.value - collapsedPeek)
)

const settledOffset = computed(() =>
  isSheetExpanded.value ? 0 : collapsedOffset.value
)

const bottomSheetOffset = computed(() => {
  if (!isDragging.value) return settledOffset.value
  return Math.min(Math.max(dragOffset.value, 0), collapsedOffset.value)
})

const bottomSheetStyle = computed(() => ({
  transform: `translateY(${bottomSheetOffset.value}px)`,
}))

const visibleSheetHeight = computed(() =>
  Math.max(0, sheetHeight.value - bottomSheetOffset.value)
)

const leafletControlsBottom = computed(() =>
  Math.max(68, 68 + visibleSheetHeight.value - 12)
)

const containerStyle = computed(() => ({
  '--leaflet-controls-bottom': `${leafletControlsBottom.value}px`,
}))

const mapStyle = computed(() => ({
  top: `${topOverlayHeight.value}px`,
  bottom: '68px',
}))

const measureSheetHeight = () => {
  if (!bottomSheetRef.value) return
  sheetHeight.value = bottomSheetRef.value.offsetHeight
}

const measureTopOverlayHeight = () => {
  if (!topOverlayRef.value) return
  const bounds = topOverlayRef.value.getBoundingClientRect()
  topOverlayHeight.value = Math.ceil(bounds.bottom + 8)
}

const onSheetPointerMove = (event: PointerEvent) => {
  if (!isDragging.value) return
  const deltaY = event.clientY - dragStartY.value
  dragOffset.value = dragStartOffset.value + deltaY
}

const stopDragging = () => {
  if (!isDragging.value) return
  const snapThreshold = collapsedOffset.value * 0.45
  isSheetExpanded.value = bottomSheetOffset.value < snapThreshold

  if (!isSheetExpanded.value) {
    selectedStopId.value = ''
    selectedBusId.value = ''
    sheetMode.value = 'stop'
  }

  isDragging.value = false

  window.removeEventListener('pointermove', onSheetPointerMove)
  window.removeEventListener('pointerup', stopDragging)
  window.removeEventListener('pointercancel', stopDragging)
}

const onSheetPointerDown = (event: PointerEvent) => {
  if (event.button !== 0) return

  isDragging.value = true
  dragStartY.value = event.clientY
  dragStartOffset.value = settledOffset.value
  dragOffset.value = settledOffset.value

  window.addEventListener('pointermove', onSheetPointerMove)
  window.addEventListener('pointerup', stopDragging)
  window.addEventListener('pointercancel', stopDragging)
}

const openSheetWithAnimation = async () => {
  isSheetExpanded.value = false
  await nextTick()
  measureSheetHeight()

  // Force one collapsed frame first, then expand on the next frame for a reliable slide-up transition.
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      isSheetExpanded.value = true
    })
  })
}

const stopMarkerIcon = (code: string, isActive: boolean) =>
  L.divIcon({
    className: '',
    html: `<span class="stop-sign${isActive ? ' is-active' : ''}"><span class="stop-sign-top">${code}</span><span class="stop-sign-pole"></span></span>`,
    iconSize: [34, 52],
    iconAnchor: [17, 44],
  })

const busMarkerIcon = (lineLabel: string, isActive: boolean) =>
  L.divIcon({
    className: '',
    html: `<span class="bus-pill${isActive ? ' is-active' : ''}">BUS ${lineLabel}</span>`,
    iconSize: [66, 28],
    iconAnchor: [33, 14],
  })

const drawStopMarkers = () => {
  if (!map || !stopMarkersLayer) return

  stopMarkersLayer.clearLayers()
  stopMarkers.clear()

  for (const stop of stops.value) {
    const isActive = stop.id === selectedStopId.value
    const marker = L.marker([stop.latitude, stop.longitude], {
      icon: stopMarkerIcon(stop.code, isActive),
    })
      .on('click', async () => {
        sheetMode.value = 'stop'
        selectedStopId.value = stop.id
        selectedBusId.value = ''
        await openSheetWithAnimation()
      })
      .addTo(stopMarkersLayer)
    stopMarkers.set(stop.id, marker)
  }
}

const drawBusMarkers = () => {
  if (!map || !busMarkersLayer) return

  busMarkersLayer.clearLayers()
  busMarkers.clear()

  for (const bus of activeBuses.value) {
    const isActive = bus.busId === selectedBusId.value
    const marker = L.marker([bus.latitude, bus.longitude], {
      icon: busMarkerIcon(bus.lineLabel, isActive),
      zIndexOffset: 300,
    })
      .on('click', async () => {
        sheetMode.value = 'bus'
        selectedBusId.value = bus.busId
        selectedStopId.value = bus.nextStopId
        await openSheetWithAnimation()
      })
      .addTo(busMarkersLayer)
    busMarkers.set(bus.busId, marker)
  }
}

const focusStop = async (stopId: string) => {
  if (!map) return
  const stop = stops.value.find((item) => item.id === stopId)
  if (!stop) return

  sheetMode.value = 'stop'
  selectedStopId.value = stop.id
  selectedBusId.value = ''
  stopQuery.value = stop.name
  showSuggestions.value = false
  await openSheetWithAnimation()
  map.setView([stop.latitude, stop.longitude], 15, { animate: true })
}

const focusFirstMatch = async () => {
  if (filteredStops.value.length === 0) return
  await focusStop(filteredStops.value[0].id)
}

const onSearchFocus = () => {
  if (stopQuery.value.trim()) {
    showSuggestions.value = true
  }
}

const onSearchInput = () => {
  showSuggestions.value = true
}

const loadGeoJsonStops = async () => {
  const response = await fetch('/geo/stops.geojson')
  const geoJson = (await response.json()) as {
    type: 'FeatureCollection'
    features: Array<{
      type: 'Feature'
      geometry: { type: 'Point'; coordinates: [number, number] }
      properties: { id: string; name: string }
    }>
  }

  stops.value = geoJson.features.map((feature) => {
    const numericCode = feature.properties.name.match(/\d{1,4}/)?.[0] ?? feature.properties.id.replace('stop_', '')
    return {
      id: feature.properties.id,
      name: feature.properties.name,
      latitude: feature.geometry.coordinates[1],
      longitude: feature.geometry.coordinates[0],
      code: numericCode,
    }
  })

  selectedStopId.value = ''
  drawStopMarkers()
  drawBusMarkers()
}

watch(selectedStopId, () => {
  drawStopMarkers()
})

watch(activeBuses, () => {
  drawBusMarkers()
}, { deep: true })

watch(selectedBusId, () => {
  drawBusMarkers()
})

watch(selectedStop, async () => {
  await nextTick()
  measureSheetHeight()
})

watch(visibleSuggestions, async () => {
  await nextTick()
  measureTopOverlayHeight()
})

onMounted(async () => {
  await nextTick()
  if (!mapContainer.value) return

  measureTopOverlayHeight()

  map = L.map(mapContainer.value, {
    zoomControl: false,
  }).setView([41.4057, -8.5332], 13)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map)
  L.control.zoom({ position: 'bottomleft' }).addTo(map)

  stopMarkersLayer = L.layerGroup().addTo(map)
  busMarkersLayer = L.layerGroup().addTo(map)
  await loadGeoJsonStops()
  await nextTick()
  measureSheetHeight()
  measureTopOverlayHeight()
  window.addEventListener('resize', measureSheetHeight)
  window.addEventListener('resize', measureTopOverlayHeight)

  tickIntervalId = window.setInterval(() => {
    nowTick.value = Date.now()
  }, 30000)
})

onUnmounted(() => {
  stopDragging()
  window.removeEventListener('resize', measureSheetHeight)
  window.removeEventListener('resize', measureTopOverlayHeight)
  if (tickIntervalId) {
    window.clearInterval(tickIntervalId)
    tickIntervalId = null
  }
  map?.remove()
  map = null
  stopMarkersLayer = null
  busMarkersLayer = null
  stopMarkers.clear()
  busMarkers.clear()
})

</script>

<style scoped>
.search-container {
  position: relative;
  height: 100vh;
  background: #eef1f5;
  overflow: hidden;
}

.map {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 68px;
  left: 0;
}

.top-overlay {
  position: absolute;
  top: 2rem;
  left: 0.75rem;
  right: 0.75rem;
  z-index: 700;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  padding: 0.7rem 0.8rem;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.08);
}

.search-input {
  border: none;
  width: 100%;
  font-size: 1rem;
  background: transparent;
  color: #111827;
}

.search-input:focus {
  outline: none;
}

.suggestions-list {
  list-style: none;
  margin: 0.5rem 0 0;
  padding: 0;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.12);
  overflow: hidden;
}

.suggestion-item + .suggestion-item {
  border-top: 1px solid #f3f4f6;
}

.suggestion-btn {
  width: 100%;
  border: none;
  background: transparent;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.8rem;
  padding: 0.62rem 0.8rem;
  text-align: left;
  color: #111827;
}

.suggestion-btn small {
  color: #6b7280;
  font-size: 0.76rem;
}

.suggestion-btn:active {
  background: #f3f4f6;
}

.chip-row {
  margin-top: 0.55rem;
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.chip-btn {
  border: 1px solid #d1d5db;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 8px;
  padding: 0.45rem 0.65rem;
  font-size: 0.86rem;
  color: #111827;
}

.results-count {
  margin: 0 0 0 auto;
  font-size: 0.95rem;
  color: #374151;
}

.bottom-sheet {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 68px;
  background: rgba(255, 255, 255, 0.97);
  border-radius: 16px 16px 0 0;
  padding: 0.6rem 1rem 1rem;
  box-shadow: 0 -8px 18px rgba(15, 23, 42, 0.12);
  z-index: 650;
  transition: transform 0.22s ease;
  touch-action: none;
}

.bottom-sheet.is-dragging {
  transition: none;
}

.drag-handle {
  width: 42px;
  height: 4px;
  border-radius: 999px;
  background: #d1d5db;
  margin: 0.2rem auto 0.8rem;
}

.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
}

.sheet-header h2 {
  margin: 0;
  font-size: 1.95rem;
  font-weight: 700;
  color: #111827;
}

.provider-badge {
  border-radius: 999px;
  padding: 0.35rem 0.9rem;
  font-size: 0.9rem;
  font-weight: 700;
  background: #e5e7eb;
  color: #4b5563;
}

.line-name,
.next-stop {
  margin: 0.4rem 0;
  color: #6b7280;
}

.ids-line {
  margin: 0.2rem 0 0.35rem;
  color: #374151;
  font-weight: 600;
  font-size: 0.9rem;
}

.route-title {
  margin: 0.85rem 0 0.5rem;
  font-size: 1.1rem;
  color: #111827;
}

.route-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.45rem 0;
  color: #111827;
}

.route-time {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.1;
}

.route-eta {
  margin-top: 0.2rem;
  color: #6b7280;
  font-size: 0.72rem;
  font-weight: 600;
}

.route-item-light {
  color: #4b5563;
}

.bottom-nav {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: space-around;
  background: white;
  border-top: 1px solid #e0e0e0;
  padding: 0.5rem 0;
  z-index: 800;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  padding: 0.75rem;
  padding: 0.75rem 1.5rem;
  text-decoration: none;
  color: #999;
  font-size: 1.5rem;
  transition: color 0.3s;
}

.nav-item.active {
  color: #667eea;
}

.icon-sm {
  width: 1rem;
  height: 1rem;
}

:global(.stop-sign) {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
}

:global(.stop-sign-top) {
  min-width: 28px;
  height: 24px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 0.35rem;
  font-size: 0.85rem;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.98);
  color: #111827;
  border: 1px solid #cbd5e1;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.12);
}

:global(.stop-sign-pole) {
  width: 2px;
  height: 16px;
  background: #64748b;
}

:global(.stop-sign.is-active .stop-sign-top) {
  background: #111827;
  color: #fff;
  border-color: #111827;
}

:global(.bus-pill) {
  height: 26px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 0.55rem;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.02em;
  background: #f8fafc;
  color: #111827;
  border: 1px solid #cbd5e1;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.15);
}

:global(.bus-pill.is-active) {
  background: #0f172a;
  color: #fff;
  border-color: #0f172a;
}

:global(.leaflet-bottom) {
  bottom: var(--leaflet-controls-bottom, 88px);
}

:global(.leaflet-bottom .leaflet-control) {
  margin-bottom: -40px;
}
</style>
