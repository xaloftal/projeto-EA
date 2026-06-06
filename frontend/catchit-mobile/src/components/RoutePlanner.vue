<template>
  <div class="route-planner" :class="[`route-planner--${layout}`]">
    <div v-if="showPickers" class="route-planner__pickers">
      <StopSearchPicker
        v-model="fromStop"
        label="From"
        placeholder="Search origin stop"
      />
      <StopSearchPicker
        v-model="toStop"
        label="To"
        placeholder="Search destination stop"
      />
      <q-btn
        color="primary"
        unelevated
        no-caps
        class="route-planner__plan-btn"
        :disable="!canPlan || routingStore.isLoading"
        @click="planRoute"
      >
        Plan route
      </q-btn>
    </div>

    <q-banner
      v-if="showNoRouteBanner"
      class="bg-negative text-white q-mb-sm"
      rounded
      dense
    >
      <template #avatar>
        <q-icon name="warning" />
      </template>
      No route found between the selected stops. Try different stops or departure time.
    </q-banner>

    <div v-if="routingStore.isLoading" class="route-planner__loading">
      <q-spinner color="primary" size="42px" />
      <p>Planning your trip...</p>
    </div>

    <q-splitter
      v-else-if="layout === 'split'"
      v-model="splitterModel"
      class="route-planner__splitter"
      :limits="[28, 72]"
    >
      <template #before>
        <ItineraryPanel
          v-if="hasPlan"
          :plan="routingStore.currentPlan"
          :from-label="fromLabel"
          :to-label="toLabel"
        />
        <div v-else class="route-planner__placeholder">
          <p>Select origin and destination, then plan your route.</p>
        </div>
      </template>
      <template #after>
        <div ref="mapContainer" class="route-planner__map" />
      </template>
    </q-splitter>

    <div v-else class="route-planner__stacked">
      <div ref="mapContainer" class="route-planner__map route-planner__map--stacked" />
      <ItineraryPanel
        v-if="hasPlan"
        :plan="routingStore.currentPlan"
        :from-label="fromLabel"
        :to-label="toLabel"
        class="route-planner__stacked-panel"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { storeToRefs } from 'pinia'
import StopSearchPicker from './StopSearchPicker.vue'
import ItineraryPanel from './ItineraryPanel.vue'
import { catchitApi } from '../services/api/catchitApi'
import { useRoutingStore } from '../stores/routingStore'
import type { RoutingLegFeature, RoutingPlanResponse, TransitMode } from '../types/routing'
import type { Stop } from '../models'

const props = withDefaults(
  defineProps<{
    layout?: 'split' | 'stacked'
    initialFromStop?: Stop | null
    initialToStop?: Stop | null
    hidePickers?: boolean
    autoPlan?: boolean
    fromLabel?: string
    toLabel?: string
  }>(),
  {
    layout: 'split',
    initialFromStop: null,
    initialToStop: null,
    hidePickers: false,
    autoPlan: false,
    fromLabel: '',
    toLabel: '',
  },
)

const routingStore = useRoutingStore()
const { currentPlan, currentError } = storeToRefs(routingStore)

const fromStop = ref<Stop | null>(props.initialFromStop)
const toStop = ref<Stop | null>(props.initialToStop)
const mapContainer = ref<HTMLElement | null>(null)
const splitterModel = ref(42)

const portoCenter: [number, number] = [41.1579, -8.6291]
const portugalNorthBounds = L.latLngBounds([40.5, -9.0], [42.0, -7.5])

let map: L.Map | null = null
let baseStopsLayer: L.LayerGroup | null = null
let itineraryLayer: L.GeoJSON | null = null

const showPickers = computed(() => !props.hidePickers)
const canPlan = computed(
  () =>
    !!fromStop.value &&
    !!toStop.value &&
    fromStop.value.latitude !== 0 &&
    toStop.value.latitude !== 0,
)
const hasPlan = computed(
  () => !!currentPlan.value && currentPlan.value.features.length > 0 && !currentPlan.value.error,
)
const showNoRouteBanner = computed(() => currentError.value === 'no_route_found')

const fromLabel = computed(() => props.fromLabel || fromStop.value?.name || '')
const toLabel = computed(() => props.toLabel || toStop.value?.name || '')

const defaultModeColor = (mode: TransitMode): string => {
  switch (mode) {
    case 'BUS':
      return '#1976d2'
    case 'SUBWAY':
    case 'METRO':
      return '#d32f2f'
    case 'RAIL':
      return '#388e3c'
    case 'TRAM':
      return '#7b1fa2'
    default:
      return '#1976d2'
  }
}

const styleLegFeature = (feature: RoutingLegFeature): L.PathOptions => {
  const { mode, routeColor } = feature.properties
  if (mode === 'WALK') {
    return {
      color: '#777',
      weight: 4,
      opacity: 0.9,
      dashArray: '6, 8',
    }
  }

  const color = routeColor?.trim() ? `#${routeColor.replace(/^#/, '')}` : defaultModeColor(mode)
  return {
    color,
    weight: 5,
    opacity: 0.92,
  }
}

const initMap = async () => {
  await nextTick()
  if (!mapContainer.value || map) return

  map = L.map(mapContainer.value, {
    zoomControl: true,
    minZoom: 10,
    maxZoom: 18,
    maxBounds: portugalNorthBounds,
    maxBoundsViscosity: 1.0,
  }).setView(portoCenter, 13)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map)

  baseStopsLayer = L.layerGroup().addTo(map)
  await loadBaseStopsLayer()
}

const loadBaseStopsLayer = async () => {
  if (!map || !baseStopsLayer) return

  const response = await catchitApi.getStopsGeoJson()
  if (!response.success || !response.data) return

  baseStopsLayer.clearLayers()
  L.geoJSON(response.data, {
    pointToLayer: (_feature, latlng) =>
      L.circleMarker(latlng, {
        radius: 3,
        color: '#94a3b8',
        weight: 1,
        fillColor: '#cbd5e1',
        fillOpacity: 0.7,
      }),
  }).addTo(baseStopsLayer)
}

const renderItineraryLayer = (plan: RoutingPlanResponse) => {
  if (!map) return

  if (itineraryLayer) {
    map.removeLayer(itineraryLayer)
    itineraryLayer = null
  }

  if (!plan.features.length) return

  itineraryLayer = L.geoJSON(plan, {
    style: (feature) => styleLegFeature(feature as RoutingLegFeature),
  }).addTo(map)

  const bounds = itineraryLayer.getBounds()
  if (bounds.isValid()) {
    map.fitBounds(bounds, { padding: [40, 40] })
  }
}

const planRoute = async () => {
  if (!fromStop.value || !toStop.value) return

  await routingStore.fetchPlan({
    fromLat: fromStop.value.latitude,
    fromLon: fromStop.value.longitude,
    toLat: toStop.value.latitude,
    toLon: toStop.value.longitude,
  })

  await nextTick()
  await initMap()
  map?.invalidateSize()
  if (routingStore.currentPlan && !routingStore.currentPlan.error) {
    renderItineraryLayer(routingStore.currentPlan)
  }
}

watch(
  () => [props.initialFromStop, props.initialToStop] as const,
  ([from, to]) => {
    if (from) fromStop.value = from
    if (to) toStop.value = to
  },
)

watch(currentPlan, async (plan) => {
  if (!plan || plan.error) return
  await nextTick()
  await initMap()
  map?.invalidateSize()
  renderItineraryLayer(plan)
})

watch(
  () => routingStore.isLoading,
  async (loading) => {
    if (!loading) {
      await nextTick()
      await initMap()
      map?.invalidateSize()
    }
  },
)

onMounted(async () => {
  if (props.initialFromStop) fromStop.value = props.initialFromStop
  if (props.initialToStop) toStop.value = props.initialToStop

  await nextTick()
  await initMap()

  const request = fromStop.value && toStop.value
    ? {
        fromLat: fromStop.value.latitude,
        fromLon: fromStop.value.longitude,
        toLat: toStop.value.latitude,
        toLon: toStop.value.longitude,
      }
    : null

  if (request) {
    const cached = routingStore.getCachedPlan(request)
    if (cached) {
      routingStore.currentPlan = cached
      routingStore.currentError = cached.error ?? null
      if (!cached.error) {
        renderItineraryLayer(cached)
      }
    } else if (props.autoPlan && canPlan.value) {
      await planRoute()
    }
  }
})

onBeforeUnmount(() => {
  if (itineraryLayer && map) {
    map.removeLayer(itineraryLayer)
  }
  itineraryLayer = null
  map?.remove()
  map = null
  baseStopsLayer = null
})
</script>

<style scoped>
.route-planner {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  min-height: 0;
}

.route-planner__pickers {
  display: grid;
  gap: 0.75rem;
}

.route-planner__plan-btn {
  width: 100%;
}

.route-planner__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 2rem 1rem;
  color: #6b7280;
}

.route-planner__splitter {
  min-height: 420px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
}

.route-planner__stacked {
  display: flex;
  flex-direction: column;
  gap: 0;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
}

.route-planner__map {
  width: 100%;
  height: 100%;
  min-height: 280px;
  background: #eef1f5;
}

.route-planner__map--stacked {
  height: 45vh;
  min-height: 260px;
}

.route-planner__stacked-panel {
  border-top: 1px solid #e5e7eb;
}

.route-planner__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 1.5rem;
  color: #9ca3af;
  text-align: center;
  font-size: 0.9rem;
}

.route-planner--split {
  flex: 1;
}

@media (min-width: 768px) {
  .route-planner__pickers {
    grid-template-columns: 1fr 1fr auto;
    align-items: end;
  }

  .route-planner__plan-btn {
    width: auto;
    min-width: 120px;
  }
}
</style>
