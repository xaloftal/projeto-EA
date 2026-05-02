<template>
  <div class="search-container" :style="containerStyle">
    <div ref="mapContainer" class="map" :style="mapStyle"></div>

    <header ref="topOverlayRef" class="app-header map-search-header">
      <div class="map-toolbar">
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

        <button class="chip-btn">Filter</button>
        <button class="chip-btn">Sort</button>
        <p class="results-count">{{ resultCount }} results</p>
      </div>

      <div v-if="apiError" class="map-error-banner">
        <span>{{ apiError }}</span>
        <button type="button" class="map-error-retry" @click="reloadMapData">Retry</button>
      </div>

      <ul v-if="visibleSuggestions" class="suggestions-list">
        <li v-for="stop in filteredStops" :key="stop.id" class="suggestion-item">
          <button type="button" class="suggestion-btn" @click="focusStop(stop.id)">
            <span>{{ stop.name }}</span>
            <small>{{ stop.code }}</small>
          </button>
        </li>
      </ul>
    </header>

    <section
      v-if="stops.length > 0"
      ref="bottomSheetRef"
      class="bottom-sheet"
      :class="{ 'is-dragging': isDragging }"
      :style="bottomSheetStyle"
    >
      <div class="drag-area" @pointerdown="onSheetPointerDown">
        <div class="drag-handle"></div>
      </div>

      <div v-if="sheetMode === 'stop' && selectedStop" class="sheet-header">
        <h2>{{ selectedStop.name }}</h2>
        <div class="sheet-header-actions">
          <button
            :disabled="isLoadingPOI"
            class="poi-btn"
            :class="{ 'is-added': isStopPOI }"
            @click="toggleStopPOI"
            :title="isStopPOI ? 'Remove from favorites' : 'Add to favorites'"
          >
            <Star :size="20" :fill="isStopPOI ? 'currentColor' : 'none'" />
          </button>
          <span class="provider-badge">{{ selectedStop.stopType || 'STOP' }}</span>
        </div>
      </div>

      <template v-if="sheetMode === 'stop' && selectedStop">
        <p class="line-name">{{ selectedStop.stopType || 'STOP' }} stop information</p>
        <p class="ids-line">Stop ID: {{ selectedStop.code }}</p>
        <p class="next-stop">Next Stop: {{ nextStopName }}</p>

        <h3 class="route-title">Routes at this stop</h3>
        <div
          v-for="route in stopRouteInfo"
          :key="route.routeId"
          class="route-item"
        >
          <span>{{ route.lineLabel }}</span>
          <span class="route-time">
            {{ route.nextTime }}
            <small class="route-eta">{{ route.etaLabel }}</small>
          </span>
        </div>
      </template>

      <template v-else-if="sheetMode === 'bus' && selectedBus">
        <div class="sheet-header">
          <h2>Bus {{ selectedBus.lineLabel }}</h2>
          <span class="provider-badge">{{ selectedBus.lineLabel }}</span>
        </div>

        <p class="line-name">{{ selectedBus.lineLabel }} information</p>
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
      <router-link to="/cart" class="nav-item"><ShoppingCart /></router-link>
      <router-link to="/cards" class="nav-item"><Ticket /></router-link>
      <router-link to="/profile" class="nav-item"><User /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onActivated, onMounted, onUnmounted, ref, watch } from 'vue'
import { House, Map as MapIcon, Search, ShoppingCart, User, Ticket, Star } from 'lucide-vue-next'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { catchitApi } from '../services/api/catchitApi'
import { useAuthViewModel } from '../viewmodels'

defineOptions({ name: 'MapView' })

type StopFeature = {
  id: string
  name: string
  latitude: number
  longitude: number
  code: string
  stopType?: string
}

type BackendStopLocation = {
  latitude?: number
  longitude?: number
}

type BackendRouteSchedule = {
  sequence?: number
  arrivalTime?: string | null
  departureTime?: string | null
  stop?: {
    id: string
    name: string
    location?: BackendStopLocation | null
    latitude?: number
    longitude?: number
  } | null
}

type BackendRoute = {
  id: string
  name?: string
  schedules?: BackendRouteSchedule[]
}

type RouteDefinition = {
  routeId: string
  lineLabel: string
  busId: string
  schedules: Array<{
    stopId: string
    stopName: string
    latitude: number
    longitude: number
    sequence: number
    arrivalTime: Date | null
    departureTime: Date | null
  }>
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
const rawGeoJson = ref<any>(null)
const apiError = ref('')
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
const poiStops = ref<Set<string>>(new Set())
const isLoadingPOI = ref(false)
const { currentUser } = useAuthViewModel()

let map: L.Map | null = null
let stopMarkersLayer: L.LayerGroup | null = null
let busMarkersLayer: L.LayerGroup | null = null
let selectedBusArrowLayer: L.LayerGroup | null = null
let selectedBusDashLine: L.Polyline | null = null
let selectedBusDashAnimationId: number | null = null
let selectedBusDashOffset = 0
let tickIntervalId: number | null = null
const stopMarkers = new Map<string, L.Marker>()
const busMarkers = new Map<string, L.Marker>()
const portoCenter: [number, number] = [41.1579, -8.6291]
const portugalNorthBounds = L.latLngBounds([40.5, -9.0], [42.0, -7.5])

const routeDefinitions = ref<RouteDefinition[]>([])

const toDateOrNull = (value?: string | null) => {
  if (!value) return null
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

const scheduleTime = (schedule: { arrivalTime: Date | null; departureTime: Date | null }) =>
  schedule.departureTime ?? schedule.arrivalTime

const seedRouteDefinitions = (routes: BackendRoute[]) => {
  if (stops.value.length < 2 || routes.length === 0) {
    routeDefinitions.value = []
    return
  }

  routeDefinitions.value = routes
    .map((route) => {
      const schedules = [...(route.schedules ?? [])]
        .filter((schedule): schedule is BackendRouteSchedule & { stop: NonNullable<BackendRouteSchedule['stop']> } => !!schedule.stop)
        .map((schedule) => {
          const latitude = schedule.stop.location?.latitude ?? schedule.stop.latitude ?? 0
          const longitude = schedule.stop.location?.longitude ?? schedule.stop.longitude ?? 0

          return {
            stopId: schedule.stop.id,
            stopName: schedule.stop.name,
            latitude,
            longitude,
            sequence: schedule.sequence ?? 0,
            arrivalTime: toDateOrNull(schedule.arrivalTime),
            departureTime: toDateOrNull(schedule.departureTime),
          }
        })
        .filter((schedule) => Number.isFinite(schedule.latitude) && Number.isFinite(schedule.longitude))
        .sort((left, right) => left.sequence - right.sequence)

      return {
        routeId: route.id,
        lineLabel: route.name?.trim() || route.id.slice(0, 8),
        busId: `bus-${route.id.slice(0, 8)}`,
        schedules,
      }
    })
    .filter((route) => route.schedules.length >= 2)
}

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

const isStopPOI = computed(() =>
  selectedStop.value ? poiStops.value.has(selectedStop.value.id) : false
)

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
  const now = new Date(nowTick.value)

  return routeDefinitions.value
    .filter((route) => route.schedules.some((schedule) => schedule.stopId === selectedId))
    .map((route) => {
      const matchingSchedule = [...route.schedules]
        .filter((schedule) => schedule.stopId === selectedId)
        .map((schedule) => ({
          ...schedule,
          displayTime: scheduleTime(schedule),
        }))
        .filter((schedule) => schedule.displayTime)
        .sort((left, right) => left.displayTime!.getTime() - right.displayTime!.getTime())[0]

      if (!matchingSchedule?.displayTime) {
        return null
      }

      const diffMinutes = Math.round((matchingSchedule.displayTime.getTime() - now.getTime()) / 60000)
      const etaLabel = diffMinutes < -1
        ? `left ${Math.abs(diffMinutes)} min ago`
        : diffMinutes <= 1
          ? 'due now'
          : `in ${diffMinutes} min`

      return {
        routeId: route.routeId,
        lineLabel: route.lineLabel,
        busId: route.busId,
        nextTime: matchingSchedule.displayTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        etaLabel,
      }
    })
    .filter((entry): entry is { routeId: string; lineLabel: string; busId: string; nextTime: string; etaLabel: string } => !!entry)
    .sort((left, right) => left.nextTime.localeCompare(right.nextTime))
})

const activeBuses = computed(() => {
  const now = new Date(nowTick.value)
  const active: ActiveBus[] = []

  for (const route of routeDefinitions.value) {
    const sortedSchedules = [...route.schedules].sort((left, right) => left.sequence - right.sequence)

    for (let index = 0; index < sortedSchedules.length - 1; index += 1) {
      const fromSchedule = sortedSchedules[index]
      const toSchedule = sortedSchedules[index + 1]
      const fromTime = scheduleTime(fromSchedule)
      const toTime = scheduleTime(toSchedule)

      if (!fromTime || !toTime || toTime <= fromTime) {
        continue
      }

      if (now < fromTime || now > toTime) {
        continue
      }

      const totalMinutes = Math.max(1, Math.round((toTime.getTime() - fromTime.getTime()) / 60000))
      const elapsedMinutes = Math.max(0, (now.getTime() - fromTime.getTime()) / 60000)
      const fraction = Math.min(Math.max(elapsedMinutes / totalMinutes, 0), 1)
      const latitude = fromSchedule.latitude + (toSchedule.latitude - fromSchedule.latitude) * fraction
      const longitude = fromSchedule.longitude + (toSchedule.longitude - fromSchedule.longitude) * fraction
      const etaMinutes = Math.max(1, Math.ceil((toTime.getTime() - now.getTime()) / 60000))

      active.push({
        busId: route.busId,
        lineLabel: route.lineLabel,
        routeId: route.routeId,
        latitude,
        longitude,
        nextStopId: toSchedule.stopId,
        nextStopName: toSchedule.stopName,
        etaMinutes,
        etaLabel: `${etaMinutes} min`,
      })

      break
    }
  }

  return active
})

const selectedBus = computed(() =>
  activeBuses.value.find((bus) => bus.busId === selectedBusId.value) ?? null
)

const stopById = computed(() => {
  const map = new Map<string, StopFeature>()
  for (const stop of stops.value) {
    map.set(stop.id, stop)
  }
  return map
})

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
  topOverlayHeight.value = Math.ceil(bounds.bottom)
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

const getBrandColor = () => {
  const fallbackColor = '#4f46e5'
  if (typeof window === 'undefined') return fallbackColor

  const cssBrandColor = getComputedStyle(document.documentElement)
    .getPropertyValue('--color-brand')
    .trim()

  return cssBrandColor || fallbackColor
}

const stopSelectedBusArrowAnimation = () => {
  if (selectedBusDashAnimationId !== null) {
    cancelAnimationFrame(selectedBusDashAnimationId)
    selectedBusDashAnimationId = null
  }
  selectedBusDashLine = null
}

const animateSelectedBusArrow = () => {
  if (!selectedBusDashLine || !selectedBusId.value) {
    stopSelectedBusArrowAnimation()
    return
  }

  selectedBusDashOffset = (selectedBusDashOffset + 0.30) % 100
  selectedBusDashLine.setStyle({ dashOffset: `${-selectedBusDashOffset}` })
  selectedBusDashAnimationId = requestAnimationFrame(animateSelectedBusArrow)
}

const startSelectedBusArrowAnimation = () => {
  if (!selectedBusDashLine) return
  if (selectedBusDashAnimationId !== null) return

  selectedBusDashOffset = 0
  selectedBusDashAnimationId = requestAnimationFrame(animateSelectedBusArrow)
}

const updateSelectedBusArrow = () => {
  if (!selectedBusArrowLayer) return

  selectedBusArrowLayer.clearLayers()
  selectedBusDashLine = null

  if (!selectedBusId.value || !selectedBus.value) {
    stopSelectedBusArrowAnimation()
    return
  }

  const destinationStop = stopById.value.get(selectedBus.value.nextStopId)
  if (!destinationStop) {
    stopSelectedBusArrowAnimation()
    return
  }

  const busPoint = L.latLng(selectedBus.value.latitude, selectedBus.value.longitude)
  const stopPoint = L.latLng(destinationStop.latitude, destinationStop.longitude)
  const brandColor = getBrandColor()

  const glowLine = L.polyline([busPoint, stopPoint], {
    color: brandColor,
    weight: 10,
    opacity: 0.2,
    lineCap: 'round',
    interactive: false,
  })

  const mainLine = L.polyline([busPoint, stopPoint], {
    color: brandColor,
    weight: 4,
    opacity: 0.98,
    dashArray: '10 9',
    dashOffset: '0',
    lineCap: 'round',
    interactive: false,
  })

  const destinationRing = L.circleMarker(stopPoint, {
    radius: 8,
    color: brandColor,
    weight: 2,
    fillColor: '#ffffff',
    fillOpacity: 1,
    interactive: false,
  })

  selectedBusArrowLayer.addLayer(glowLine)
  selectedBusArrowLayer.addLayer(mainLine)
  selectedBusArrowLayer.addLayer(destinationRing)

  selectedBusDashLine = mainLine
  startSelectedBusArrowAnimation()
}

const drawStopMarkers = () => {
  if (!map || !stopMarkersLayer || !rawGeoJson.value) return

  stopMarkersLayer.clearLayers();
  stopMarkers.clear()

  L.geoJSON(rawGeoJson.value, {
    pointToLayer: (feature, latlng) => {
      const isActive = feature.properties.id === selectedStopId.value
      return L.marker(latlng, {
        icon: stopMarkerIcon(feature.properties.code, isActive)
      });
    },
    onEachFeature: (feature, layer) => {
      layer.on('click', async () => {
        sheetMode.value = 'stop'
        selectedStopId.value = feature.properties.id
        selectedBusId.value = ''
        await openSheetWithAnimation()
      });
    }
  }).addTo(stopMarkersLayer);
};

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

  updateSelectedBusArrow()
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

const loadBackendStops = async () => {
  apiError.value = ''

  const geoJsonResponse = await catchitApi.getStopsGeoJson()

  if (!geoJsonResponse.success || !geoJsonResponse.data) {
    apiError.value = geoJsonResponse.error || 'Failed to load stops'
    return
  }

  rawGeoJson.value = geoJsonResponse.data

  stops.value = geoJsonResponse.data.features.map((f: any) => ({
    id: f.properties.id,
    name: f.properties.name,
    latitude: f.properties.latitude,
    longitude: f.properties.longitude,
    code: f.properties.code,
    stopType: f.properties.stopType,
  }))

  selectedStopId.value = ''
  selectedBusId.value = ''
  drawStopMarkers()
  drawBusMarkers()

  void loadRouteData()
}

const loadRouteData = async () => {
  const routesResponse = await catchitApi.getRoutes()

  if (!routesResponse.success || !routesResponse.data) {
    console.warn(routesResponse.error || 'Unable to load routes from backend')
    routeDefinitions.value = []
    drawBusMarkers()
    return
  }

  seedRouteDefinitions(routesResponse.data)
  drawBusMarkers()
}

const reloadMapData = async () => {
  await loadBackendStops()
}

const toggleStopPOI = async () => {
  if (!selectedStop.value) {
    console.warn('No stop selected')
    return
  }

  if (!currentUser.value) {
    console.warn('User not authenticated')
    apiError.value = 'Please log in to add favorites'
    return
  }

  if (isLoadingPOI.value) return

  isLoadingPOI.value = true
  try {
    if (isStopPOI.value) {
      // Remove from POI
      const response = await catchitApi.removePOI(currentUser.value.id, selectedStop.value.id)
      if (response.success) {
        poiStops.value.delete(selectedStop.value.id)
        console.log('Removed from POI:', selectedStop.value.name)
      } else {
        console.error('Error removing POI:', response.error)
        apiError.value = response.error || 'Error removing favorite'
      }
    } else {
      // Add to POI
      const response = await catchitApi.addPOI(currentUser.value.id, selectedStop.value.id)
      if (response.success) {
        poiStops.value.add(selectedStop.value.id)
        console.log('Added to POI:', selectedStop.value.name)
      } else {
        console.error('Error adding POI:', response.error)
        apiError.value = response.error || 'Error adding favorite'
      }
    }
  } catch (error) {
    console.error('Error toggling POI:', error)
    apiError.value = 'Error updating favorite'
  } finally {
    isLoadingPOI.value = false
  }
}

const loadUserPOIs = async () => {
  if (!currentUser.value) {
    console.warn('User not authenticated, skipping POI load')
    return
  }

  try {
    const response = await catchitApi.getUserPOI(currentUser.value.id)
    if (response.success && response.data) {
      poiStops.value.clear()
      response.data.forEach((stop) => {
        poiStops.value.add(stop.id)
      })
      console.log('Loaded POIs:', poiStops.value.size)
    } else {
      console.warn('Error loading POIs:', response.error)
    }
  } catch (error) {
    console.error('Error loading user POIs:', error)
  }
}

watch(selectedStopId, () => {
  drawStopMarkers()
  updateSelectedBusArrow()
})

watch(
  activeBuses,
  () => {
    drawBusMarkers()
  },
  { deep: true }
)

watch(selectedBusId, () => {
  drawBusMarkers()
  updateSelectedBusArrow()
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

  map = L.map(mapContainer.value, {
    zoomControl: false,
    minZoom: 10,
    maxZoom: 18,
    maxBounds: portugalNorthBounds,
    maxBoundsViscosity: 1.0,
  }).setView(portoCenter, 13)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map)

  L.control.zoom({ position: 'bottomleft' }).addTo(map)

  stopMarkersLayer = L.layerGroup().addTo(map)
  busMarkersLayer = L.layerGroup().addTo(map)
  
  // Load user POIs before loading stops
  await loadUserPOIs()
  

  void loadBackendStops()

  measureTopOverlayHeight()
  measureSheetHeight()
  window.addEventListener('resize', measureSheetHeight)
  window.addEventListener('resize', measureTopOverlayHeight)
})



(() => {
  map?.invalidateSize()
})

onUnmounted(() => {
  stopDragging()
  stopSelectedBusArrowAnimation()
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
  selectedBusArrowLayer = null
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

.map-search-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 700;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  box-sizing: border-box;
  height: var(--header-height);
  min-height: var(--header-height);
  padding: 0.7rem 0.75rem 0.65rem;
}

.map-toolbar {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  flex: 1;
  min-width: 0;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.34);
  padding: 0.52rem 0.65rem;
}

.search-input {
  border: none;
  width: 100%;
  font-size: 0.98rem;
  background: transparent;
  color: #f8fafc;
}

.search-input:focus {
  outline: none;
}

.search-input::placeholder {
  color: rgba(248, 250, 252, 0.82);
}

.suggestions-list {
  list-style: none;
  position: absolute;
  left: 0.75rem;
  right: 0.75rem;
  top: calc(var(--header-height) - 0.25rem);
  margin: 0;
  padding: 0;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #dbe2f0;
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

.chip-btn {
  border: 1px solid rgba(255, 255, 255, 0.34);
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  padding: 0.5rem 0.68rem;
  font-size: 0.84rem;
  color: #f8fafc;
}

.results-count {
  margin: 0 0 0 auto;
  font-size: 0.88rem;
  color: rgba(248, 250, 252, 0.9);
  white-space: nowrap;
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
  max-height: 50vh;
  overflow-y: auto;
}

.bottom-sheet.is-dragging {
  transition: none;
}

.drag-area {
  padding: 0.5rem 0;
  margin-top: -0.5rem;
  touch-action: none;
  cursor: grab;
}

.drag-handle {
  width: 42px;
  height: 4px;
  border-radius: 999px;
  background: #d1d5db;
  margin: 0 auto;
}

.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
}

.sheet-header-actions {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.poi-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border: none;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  transition: color 0.2s ease;
  padding: 0;
}

.poi-btn:hover {
  color: #fbbf24;
}

.poi-btn.is-added {
  color: #fbbf24;
}

.poi-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.sheet-header h2 {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 700;
  color: #111827;
  line-height: 1.1;
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
  z-index: 800;
}

.nav-item {
  padding: 0.75rem;
}

.nav-item.active {
  color: var(--color-brand);
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
