<template>
  <div class="itinerary-page app-screen">
    <header class="app-header">
      <router-link to="/home" class="back-btn" aria-label="Back">
        <ArrowLeft class="icon-md" />
      </router-link>
      <h1>Suggested itinerary</h1>
      <div style="width: 1rem" />
    </header>

    <div class="itinerary-page__content">
      <div v-if="isResolvingStops" class="itinerary-page__loading">
        <q-spinner color="primary" size="42px" />
        <p>Loading trip details...</p>
      </div>

      <RoutePlanner
        v-else-if="fromStop && toStop"
        layout="stacked"
        :initial-from-stop="fromStop"
        :initial-to-stop="toStop"
        :from-label="fromStop.name"
        :to-label="toStop.name"
        hide-pickers
        auto-plan
        hide-cart-button
      />

      <q-banner v-else class="bg-warning text-dark" rounded>
        Missing origin or destination. Go back and select a ticket with valid stops.
      </q-banner>
    </div>

    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item active"><House class="nav-icon" /></router-link>
      <router-link to="/map" class="nav-item"><Map class="nav-icon" /></router-link>
      <router-link to="/cart" class="nav-item"><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/cards" class="nav-item"><Ticket class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item"><User class="nav-icon" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, House, Map, ShoppingCart, Ticket, User } from 'lucide-vue-next'
import RoutePlanner from '../components/RoutePlanner.vue'
import { catchitApi } from '../services/api/catchitApi'
import type { Stop } from '../models'

const route = useRoute()
const fromStop = ref<Stop | null>(null)
const toStop = ref<Stop | null>(null)
const isResolvingStops = ref(true)

const parseCoordinate = (value: unknown): number | null => {
  if (typeof value !== 'string' && typeof value !== 'number') return null
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

onMounted(async () => {
  const fromLat = parseCoordinate(route.query.fromLat)
  const fromLon = parseCoordinate(route.query.fromLon)
  const toLat = parseCoordinate(route.query.toLat)
  const toLon = parseCoordinate(route.query.toLon)
  const fromName = typeof route.query.fromName === 'string' ? route.query.fromName : 'Origin'
  const toName = typeof route.query.toName === 'string' ? route.query.toName : 'Destination'

  if (fromLat !== null && fromLon !== null && toLat !== null && toLon !== null) {
    fromStop.value = {
      id: typeof route.query.fromStopId === 'string' ? route.query.fromStopId : 'from',
      name: fromName,
      latitude: fromLat,
      longitude: fromLon,
    }
    toStop.value = {
      id: typeof route.query.toStopId === 'string' ? route.query.toStopId : 'to',
      name: toName,
      latitude: toLat,
      longitude: toLon,
    }
    isResolvingStops.value = false
    return
  }

  const fromStopId = typeof route.query.fromStopId === 'string' ? route.query.fromStopId : ''
  const toStopId = typeof route.query.toStopId === 'string' ? route.query.toStopId : ''

  if (fromStopId && toStopId) {
    const response = await catchitApi.getStops()
    if (response.success && response.data) {
      fromStop.value = response.data.find((stop) => stop.id === fromStopId) ?? null
      toStop.value = response.data.find((stop) => stop.id === toStopId) ?? null
    }
  }

  isResolvingStops.value = false
})
</script>

<style scoped>
.itinerary-page__content {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.itinerary-page__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 3rem 1rem;
  color: #6b7280;
}

.bottom-nav {
  margin-top: auto;
}
</style>
