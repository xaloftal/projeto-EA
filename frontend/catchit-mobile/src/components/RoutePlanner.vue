<template>
  <div class="route-planner" :class="[`route-planner--${layout}`]">
    <div v-if="showPickers && isModalOpen" class="route-planner__modal-backdrop" @click.self="isModalOpen = false">
      <div class="route-planner__modal">
        <div class="route-planner__modal-header">
          <h3>Plan Trip</h3>
          <button v-if="hasPlan" class="route-planner__close" @click="isModalOpen = false"><X class="icon-sm" /></button>
        </div>
        <div class="route-planner__pickers">
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
          <div class="route-planner__datetime">
            <label class="route-planner__label">
              <span>Date</span>
              <input v-model="departureDate" type="date" :min="todayDateString" class="route-planner__input" />
            </label>
            <label class="route-planner__label">
              <span>Time</span>
              <input v-model="departureTime" type="time" class="route-planner__input" />
            </label>
          </div>
          <q-btn
            color="primary"
            unelevated
            no-caps
            class="route-planner__plan-btn"
            :disable="!canPlan || routingStore.isLoading"
            @click="planRoute"
          >
            Plan trip
          </q-btn>
        </div>
      </div>
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
      No route found between the selected stops, at this given time. Try again later.
    </q-banner>

    <div class="route-planner__content">
      <div v-if="routingStore.isLoading" class="route-planner__loading-overlay">
        <q-spinner color="primary" size="42px" />
        <p>Planning your trip...</p>
      </div>

      <q-splitter
        v-if="layout === 'split'"
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
            :hide-cart-button="hideCartButton"
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
        <div ref="mapContainerStacked" class="route-planner__map route-planner__map--stacked">
          <button v-if="hasPlan && !isModalOpen" class="route-planner__edit-fab" @click="isModalOpen = true">
            Edit Trip
          </button>
        </div>
        <ItineraryPanel
          v-if="hasPlan"
          :plan="routingStore.currentPlan"
          :from-label="fromLabel"
          :to-label="toLabel"
          :hide-cart-button="hideCartButton"
          class="route-planner__stacked-panel"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { storeToRefs } from 'pinia'
import { X } from 'lucide-vue-next'
import StopSearchPicker from './StopSearchPicker.vue'
import ItineraryPanel from './ItineraryPanel.vue'
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
    hideCartButton?: boolean
  }>(),
  {
    layout: 'split',
    initialFromStop: null,
    initialToStop: null,
    hidePickers: false,
    autoPlan: false,
    fromLabel: '',
    toLabel: '',
    hideCartButton: false,
  },
)

const routingStore = useRoutingStore()
const { currentPlan, currentError } = storeToRefs(routingStore)

const fromStop = ref<Stop | null>(props.initialFromStop)
const toStop = ref<Stop | null>(props.initialToStop)
const mapContainer = ref<HTMLElement | null>(null)
const mapContainerStacked = ref<HTMLElement | null>(null)
const splitterModel = ref(42)

const todayDateString = new Date().toISOString().split('T')[0]
const departureDate = ref(todayDateString)
const departureTime = ref(new Date().toTimeString().substring(0, 5))

const isModalOpen = ref(true)

const portoCenter: [number, number] = [41.1579, -8.6291]
const portugalNorthBounds = L.latLngBounds([40.5, -9.0], [42.0, -7.5])

let map: L.Map | null = null
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
  const activeContainer = props.layout === 'split' ? mapContainer.value : mapContainerStacked.value
  if (!activeContainer || map) return

  map = L.map(activeContainer, {
    zoomControl: true,
    minZoom: 10,
    maxZoom: 18,
    maxBounds: portugalNorthBounds,
    maxBoundsViscosity: 1.0,
  }).setView(portoCenter, 13)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map)
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

  setTimeout(() => {
    if (!map || !itineraryLayer) return
    map.invalidateSize()
    const bounds = itineraryLayer.getBounds()
    if (bounds.isValid()) {
      map.fitBounds(bounds, { padding: [40, 40] })
    }
  }, 150)
}

const planRoute = async () => {
  if (!fromStop.value || !toStop.value) return

  isModalOpen.value = false

  await routingStore.fetchPlan({
    fromLat: fromStop.value.latitude,
    fromLon: fromStop.value.longitude,
    toLat: toStop.value.latitude,
    toLon: toStop.value.longitude,
    date: departureDate.value,
    time: departureTime.value + ':00'
  })

  await nextTick()
  await initMap()
  map?.invalidateSize()
  if (routingStore.currentPlan && !routingStore.currentPlan.error) {
    renderItineraryLayer(routingStore.currentPlan)
  }
}

watch(departureDate, (newDate) => {
  if (newDate === todayDateString) {
    const nowTime = new Date().toTimeString().substring(0, 5)
    if (departureTime.value < nowTime) {
      departureTime.value = nowTime
    }
  }
})

watch(departureTime, (newTime) => {
  if (departureDate.value === todayDateString) {
    const nowTime = new Date().toTimeString().substring(0, 5)
    if (newTime < nowTime) {
      departureTime.value = nowTime
    }
  }
})

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
        date: departureDate.value,
        time: departureTime.value + ':00',
      }
    : null

  if (request) {
    const cached = routingStore.getCachedPlan(request)
    if (cached) {
      routingStore.currentPlan = cached
      routingStore.currentError = cached.error ?? null
      if (!cached.error) {
        isModalOpen.value = false
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
})
</script>

<style scoped>
.route-planner {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 0;
}

.route-planner__modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  z-index: 1500;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 0;
}

.route-planner__modal {
  background: white;
  width: 100%;
  border-radius: 20px 20px 0 0;
  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: scroll;
  padding-bottom: env(safe-area-inset-bottom, 1rem);

}

.route-planner__modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #f1f5f9;
}

.route-planner__modal-header h3 {
  margin: 0;
  font-size: 1.15rem;
  color: #0f172a;
}

.route-planner__close {
  background: none;
  border: none;
  font-size: 1.5rem;
  line-height: 1;
  color: #64748b;
  cursor: pointer;
  padding: 0;
}

.route-planner__pickers {
  display: grid;
  gap: 0.75rem;
  padding: 1.25rem;
}

.route-planner__datetime {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.route-planner__label {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: #4b5563;
}

.route-planner__input {
  width: 100%;
  padding: 0.65rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 0.95rem;
  background: white;
  color: #111827;
}

.route-planner__plan-btn {
  width: 100%;
}

.route-planner__content {
  position: relative;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.route-planner__loading-overlay {
  position: absolute;
  inset: 0;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.85);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  color: #6b7280;
  border-radius: 12px;
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
  height: 100%;
  overflow: hidden;
}

.route-planner__map {
  width: 100%;
  height: 100%;
  min-height: 280px;
  background: #eef1f5;
  position: relative;
}

.route-planner__edit-fab {
  position: sticky;
  top: 1rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  background: white;
  color: var(--color-brand, #4f46e5);
  font-weight: 600;
  border: 1px solid #e5e7eb;
  padding: 0.6rem 1.25rem;
  border-radius: 24px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  cursor: pointer;
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
