<template>
  <div class="itinerary-panel">
    <header v-if="plan?.summary" class="itinerary-panel__header">
      <div>
        <p class="itinerary-panel__route">{{ fromLabel }} → {{ toLabel }}</p>
        <p class="itinerary-panel__meta">
          <span>{{ formatDuration(plan.summary.durationSeconds) }}</span>
          <span>{{ plan.summary.transfers }} transfer{{ plan.summary.transfers === 1 ? '' : 's' }}</span>
          <span>{{ Math.round(plan.summary.walkDistanceMeters) }} m walking</span>
        </p>
      </div>
    </header>

    <q-timeline v-if="sortedLegs.length" color="primary" layout="comfortable" class="itinerary-panel__timeline">
      <q-timeline-entry
        v-for="leg in sortedLegs"
        :key="leg.properties.legIndex"
        :icon="modeIcon(leg.properties.mode)"
        :color="legColor(leg.properties)"
      >
        <template #title>
          <div class="leg-title">
            <q-chip
              v-if="leg.properties.mode !== 'WALK'"
              dense
              :style="{ backgroundColor: legChipColor(leg.properties), color: '#fff' }"
              class="leg-chip"
            >
              {{ leg.properties.routeShortName || leg.properties.routeLongName || leg.properties.mode }}
            </q-chip>
            <q-chip v-else dense color="grey-6" text-color="white" class="leg-chip">
              Walk {{ Math.round(leg.properties.distanceMeters) }} m
            </q-chip>
          </div>
        </template>

        <template #subtitle>
          <span class="leg-time">
            {{ formatLegTime(leg.properties.startTime) }} – {{ formatLegTime(leg.properties.endTime) }}
          </span>
        </template>

        <p class="leg-stops">
          {{ leg.properties.fromStop }} → {{ leg.properties.toStop }}
        </p>
      </q-timeline-entry>
    </q-timeline>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RoutingLegFeature, RoutingLegProperties, RoutingPlanResponse, TransitMode } from '../types/routing'

const props = defineProps<{
  plan: RoutingPlanResponse | null
  fromLabel?: string
  toLabel?: string
}>()

const sortedLegs = computed(() => {
  if (!props.plan?.features?.length) return []
  return [...props.plan.features].sort(
    (a, b) => a.properties.legIndex - b.properties.legIndex,
  ) as RoutingLegFeature[]
})

const formatDuration = (seconds: number): string => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) return `${hours}h ${minutes}m`
  return `${minutes}m`
}

const formatLegTime = (epochMs: number): string =>
  new Date(epochMs).toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit' })

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
      return '#777777'
  }
}

const legChipColor = (props: RoutingLegProperties): string => {
  if (props.routeColor?.trim()) return `#${props.routeColor.replace(/^#/, '')}`
  return defaultModeColor(props.mode)
}

const legColor = (props: RoutingLegProperties): string => {
  if (props.mode === 'WALK') return 'grey-6'
  return 'primary'
}

const modeIcon = (mode: TransitMode): string => {
  switch (mode) {
    case 'WALK':
      return 'directions_walk'
    case 'BUS':
      return 'directions_bus'
    case 'RAIL':
      return 'train'
    case 'SUBWAY':
    case 'METRO':
      return 'subway'
    case 'TRAM':
      return 'tram'
    default:
      return 'commute'
  }
}
</script>

<style scoped>
.itinerary-panel {
  padding: 1rem;
  overflow-y: auto;
  max-height: 100%;
  background: #fff;
}

.itinerary-panel__header {
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #e5e7eb;
}

.itinerary-panel__route {
  margin: 0 0 0.35rem;
  font-weight: 700;
  font-size: 1rem;
  color: #111827;
}

.itinerary-panel__meta {
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  color: #6b7280;
  font-size: 0.85rem;
}

.itinerary-panel__timeline {
  margin-top: 0.25rem;
}

.leg-title {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.leg-chip {
  font-weight: 700;
}

.leg-time {
  font-size: 0.8rem;
  color: #6b7280;
}

.leg-stops {
  margin: 0.25rem 0 0;
  font-size: 0.9rem;
  color: #374151;
}
</style>
