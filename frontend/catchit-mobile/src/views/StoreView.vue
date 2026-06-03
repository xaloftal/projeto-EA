<template>
  <div class="container app-screen">
    <header class="app-header">
      <router-link to="/home" class="back-btn" aria-label="Back"><ArrowLeft class="icon-md" /></router-link>
      <h1>Store</h1>
      <div style="width: 1rem"></div>
    </header>

    <div class="tabs">
      <button
        :class="['tab', { active: activeTab === 'cards' }]"
        :style="{ color: cardsTabColor }"
        @click="setActiveTab('cards')"
      >
        Buy Cards
      </button>
      <button
        :class="['tab', { active: activeTab === 'tickets' }]"
        :style="{ color: ticketsTabColor }"
        @click="setActiveTab('tickets')"
      >
        Buy Tickets
      </button>
      <span class="tab-indicator" :style="tabIndicatorStyle"></span>
    </div>

    <div
      ref="swipeViewport"
      class="swipe-viewport"
      @touchstart="onTouchStart"
      @touchmove="onTouchMove"
      @touchend="onTouchEnd"
      @touchcancel="onTouchEnd"
    >
      <div class="swipe-track" :class="{ 'is-dragging': isDragging }" :style="swipeTrackStyle">
        <div class="swipe-pane cards-pane">
          <div class="cards-list">
            <div v-if="cardViewModel.isLoading.value" class="zones-loading">
              <LoaderCircle class="spinner-icon" />
            </div>

            <template v-else>
              <div class="zone-search">
                <Search class="zone-search__icon" />
                <input
                  v-model="zoneSearch"
                  type="text"
                  class="zone-search__input"
                  placeholder="Search zone cards"
                />
              </div>

              <p v-if="filteredZoneCards.length === 0" class="zone-search__empty">
                No zones match your search.
              </p>

              <ZoneCard
                v-for="zone in filteredZoneCards"
                :key="zone.id"
                :zone-name="zone.name"
                :zone-color="zone.zoneColorHexCode || '#111111'"
                :valid-until="''"
              >
                <template #actions>
                  <button class="price-btn" @click="purchaseZone(zone)">
                    Buy card
                  </button>
                </template>
              </ZoneCard>
            </template>
            </div>
          </div>

        <div class="swipe-pane tickets-pane">
          <div class="tickets-section">
            <article class="ticket-option">
              <div class="ticket-head">
                <Ticket class="ticket-icon" />
                <h2>Search Tickets</h2>
              </div>

              <div class="ticket-form">
                <label>
                  <span>From</span>
                  <div class="stop-search-wrapper">
                    <input
                      v-model="fromStopSearch"
                      type="text"
                      class="ticket-select"
                      placeholder="Search origin by stop name"
                      autocomplete="off"
                      @focus="isFromDropdownOpen = true"
                      @blur="onFromInputBlur"
                    />
                    <ul v-if="isFromDropdownOpen" class="stop-dropdown">
                      <li
                        v-for="stop in filteredFromStops"
                        :key="`from-${stop.id}`"
                        class="stop-dropdown-item"
                        @mousedown.prevent="selectFromStop(stop)"
                      >
                        {{ stop.name }}
                      </li>
                      <li v-if="!filteredFromStops.length" class="stop-dropdown-empty">No stops found with that name.</li>
                    </ul>
                  </div>
                </label>

                <label>
                  <span>To</span>
                  <div class="stop-search-wrapper">
                    <input
                      v-model="toStopSearch"
                      type="text"
                      class="ticket-select"
                      placeholder="Search destination by stop name"
                      autocomplete="off"
                      @focus="isToDropdownOpen = true"
                      @blur="onToInputBlur"
                    />
                    <ul v-if="isToDropdownOpen" class="stop-dropdown">
                      <li
                        v-for="stop in filteredToStops"
                        :key="`to-${stop.id}`"
                        class="stop-dropdown-item"
                        @mousedown.prevent="selectToStop(stop)"
                      >
                        {{ stop.name }}
                      </li>
                      <li v-if="!filteredToStops.length" class="stop-dropdown-empty">No stops found with that name.</li>
                    </ul>
                  </div>
                </label>

                <label>
                  <span>Date</span>
                  <input v-model="departureDate" type="date" class="ticket-select" />
                </label>

                <button
                  class="ticket-btn"
                  :disabled="!canSearchTickets || travelViewModel.isLoading.value"
                  @click="searchTickets"
                >
                  {{ travelViewModel.isLoading.value ? 'Searching...' : 'Search Routes' }}
                </button>
              </div>

              <p v-if="ticketMessage" class="ticket-message">{{ ticketMessage }}</p>

              <div v-if="searchResults.length > 0" class="route-results">
                <article v-for="result in searchResults" :key="result.routeId" class="route-result-item">
                  <div>
                    <p class="route-name">{{ result.routeName }} - {{ result.fromStop.code }} → {{ result.toStop.code }}</p>
                    <router-link
                      :to="{ name: 'schedule', query: { routeId: result.routeId } }"
                      class="route-meta route-schedule-link"
                    >
                      See all hours
                    </router-link>
                  </div>
                  <div class="route-actions">
                    <p class="route-price">€{{ result.price.toFixed(2) }}</p>
                    <button class="ticket-buy-btn" @click="addTicketToCart(result)">Add to cart</button>
                  </div>
                </article>
              </div>
            </article>

            <article class="ticket-option ticket-option-muted">
              <div class="ticket-head">
                <MapPin class="ticket-icon" />
                <h2>Map Search</h2>
              </div>
              <p>Prefer visual stop search? Use the map screen for stop-based route exploration.</p>
              <router-link to="/map" class="ticket-btn">Open Map</router-link>
            </article>
          </div>
        </div>
      </div>
    </div>

    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><House class="nav-icon" /></router-link>
      <router-link to="/map" class="nav-item"><Map class="nav-icon" /></router-link>
      <router-link to="/cart" class="nav-item "><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/cards" class="nav-item active"><Ticket class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item"><User class="nav-icon" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ArrowLeft, House, LoaderCircle, Map, MapPin, Search, ShoppingCart, Ticket, User } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import { RouteSearchResult, useCardViewModel, useCheckoutViewModel, useTravelViewModel } from '../viewmodels'
import { catchitApi } from '../services/api/catchitApi'
import ZoneCard from '../components/ZoneCard.vue'
import type { Stop } from '../models'
import { useQuasar } from 'quasar'
const $q = useQuasar()

// type RouteResult = {
//   routeId: string
//   fromStop: Stop
//   toStop: Stop
//   departureTime: string
//   arrivalTime: string
//   price: number
//   vehicle: Vehicle
// }

const cardViewModel = useCardViewModel()
const travelViewModel = useTravelViewModel()
const checkoutViewModel = useCheckoutViewModel()
const route = useRoute()
const activeTab = ref<'cards' | 'tickets'>('cards')
const swipeViewport = ref<HTMLElement | null>(null)
const viewportWidth = ref(0)
const touchStartX = ref(0)
const currentDragX = ref(0)
const isDragging = ref(false)
const availableStops = ref<Stop[]>([])
const fromStopSearch = ref('')
const toStopSearch = ref('')
const isFromDropdownOpen = ref(false)
const isToDropdownOpen = ref(false)
const fromStopId = ref('')
const toStopId = ref('')
const departureDate = ref(new Date().toISOString().split('T')[0])
const ticketMessage = ref('')
const zoneSearch = ref('')

const zoneCards = computed(() =>
  [...cardViewModel.availableCards.value].sort((left, right) => left.name.localeCompare(right.name))
)

const filteredZoneCards = computed(() => {
  const query = zoneSearch.value.trim().toLowerCase()
  if (!query) return zoneCards.value

  return zoneCards.value.filter((zone) => zone.name.toLowerCase().includes(query))
})

const canSearchTickets = computed(() =>
  !!fromStopId.value && !!toStopId.value && fromStopId.value !== toStopId.value && !!departureDate.value
)

const normalizeStopName = (value: string) =>
  value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()

const filterStopsByName = (search: string) => {
  const searchTerm = normalizeStopName(search)
  if (!searchTerm) return availableStops.value
  return availableStops.value.filter((stop) => normalizeStopName(stop.name).includes(searchTerm))
}

const filteredFromStops = computed(() => filterStopsByName(fromStopSearch.value))
const filteredToStops = computed(() => filterStopsByName(toStopSearch.value))

const searchResults = computed(() => travelViewModel.searchResults.value as RouteSearchResult[])

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value))

const blendColor = (from: string, to: string, amount: number) => {
  const progress = clamp(amount, 0, 1)
  const fromRGB = from.match(/[A-Fa-f0-9]{2}/g)?.map((hex) => parseInt(hex, 16))
  const toRGB = to.match(/[A-Fa-f0-9]{2}/g)?.map((hex) => parseInt(hex, 16))

  if (!fromRGB || !toRGB || fromRGB.length !== 3 || toRGB.length !== 3) return to

  const mixed = fromRGB.map((value, index) => Math.round(value + (toRGB[index] - value) * progress))
  return `rgb(${mixed[0]}, ${mixed[1]}, ${mixed[2]})`
}

const updateViewportWidth = () => {
  viewportWidth.value = swipeViewport.value?.clientWidth ?? 0
}

const tabProgress = computed(() => {
  const width = viewportWidth.value || 1
  const dragRatio = clamp(Math.abs(currentDragX.value) / width, 0, 1)

  if (activeTab.value === 'cards') {
    return dragRatio
  }

  return 1 - dragRatio
})

const cardsTabColor = computed(() => blendColor('#9ca3af', '#667eea', 1 - tabProgress.value))
const ticketsTabColor = computed(() => blendColor('#9ca3af', '#667eea', tabProgress.value))

const tabIndicatorStyle = computed(() => ({
  transform: `translateX(${tabProgress.value * 100}%)`,
}))

const swipeTrackStyle = computed(() => {
  const width = viewportWidth.value
  if (!width) {
    return {
      transform: activeTab.value === 'cards' ? 'translate3d(0, 0, 0)' : 'translate3d(-100%, 0, 0)',
    }
  }

  const baseOffset = activeTab.value === 'cards' ? 0 : -width
  const offset = baseOffset + currentDragX.value

  return {
    transform: `translate3d(${offset}px, 0, 0)`,
  }
})

const setActiveTab = (tab: 'cards' | 'tickets') => {
  activeTab.value = tab
  currentDragX.value = 0
}

const resolveStopIdByName = (search: string) => {
  const normalizedSearch = normalizeStopName(search)
  if (!normalizedSearch) return ''

  return availableStops.value.find((stop) => normalizeStopName(stop.name) === normalizedSearch)?.id ?? ''
}

watch(fromStopSearch, (search) => {
  fromStopId.value = resolveStopIdByName(search)
})

watch(toStopSearch, (search) => {
  toStopId.value = resolveStopIdByName(search)
})

const selectFromStop = (stop: Stop) => {
  fromStopSearch.value = stop.name
  fromStopId.value = stop.id
  isFromDropdownOpen.value = false
}

const selectToStop = (stop: Stop) => {
  toStopSearch.value = stop.name
  toStopId.value = stop.id
  isToDropdownOpen.value = false
}

const onFromInputBlur = () => {
  window.setTimeout(() => {
    isFromDropdownOpen.value = false
  }, 120)
}

const onToInputBlur = () => {
  window.setTimeout(() => {
    isToDropdownOpen.value = false
  }, 120)
}

const onTouchStart = (event: TouchEvent) => {
  if (!event.touches.length) return
  touchStartX.value = event.touches[0].clientX
  isDragging.value = true
  currentDragX.value = 0
}

const onTouchMove = (event: TouchEvent) => {
  if (!isDragging.value || !event.touches.length) return

  const deltaX = event.touches[0].clientX - touchStartX.value
  const width = viewportWidth.value || 1
  const maxDrag = width * 0.8

  if (activeTab.value === 'cards') {
    currentDragX.value = clamp(deltaX, -maxDrag, 0)
    return
  }

  currentDragX.value = clamp(deltaX, 0, maxDrag)
}

const onTouchEnd = () => {
  if (!isDragging.value) return

  const width = viewportWidth.value || 1
  const threshold = width * 0.2
  const shouldMoveToTickets = activeTab.value === 'cards' && currentDragX.value <= -threshold
  const shouldMoveToCards = activeTab.value === 'tickets' && currentDragX.value >= threshold

  if (shouldMoveToTickets) {
    activeTab.value = 'tickets'
  } else if (shouldMoveToCards) {
    activeTab.value = 'cards'
  }

  currentDragX.value = 0
  isDragging.value = false
}

const searchTickets = async () => {
  if (!canSearchTickets.value) {
    ticketMessage.value = 'Please choose valid and different origin and destination stops from the suggestions.'
    return
  }

  ticketMessage.value = ''
  const results = await travelViewModel.searchRoutes(fromStopId.value, toStopId.value, departureDate.value)
  if (!results.length) {
    ticketMessage.value = 'No routes found for your selection.'
  }
}

const purchaseZone = async (zone: { id: string; name: string }) => {
  const zoneCard = cardViewModel.availableCards.value.find(c => c.id === zone.id)
  if (!zoneCard) return
    await checkoutViewModel.addCardToCart(zoneCard)
    $q.notify({
      message: `${zone.name} card added to cart successfully`,
      color: 'positive',
      position: 'top',
      timeout: 3000,
    })
  }

  const addTicketToCart = (result: RouteSearchResult) => {
    void checkoutViewModel.addTicketToCart(result)
    $q.notify({
      message: 'Ticket added to cart successfully',
      color: 'positive',
      position: 'top',
      timeout: 3000,
    })
  }

  const applyTabFromRoute = () => {
    if (route.query.tab === 'tickets') {
      activeTab.value = 'tickets'
      currentDragX.value = 0
    }
}

onMounted(async () => {
  applyTabFromRoute()

  updateViewportWidth()
  window.addEventListener('resize', updateViewportWidth)

  await cardViewModel.fetchAvailableCards()

  const stopsResponse = await catchitApi.getStops()
  if (stopsResponse.success && stopsResponse.data) {
    availableStops.value = stopsResponse.data
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewportWidth)
})

</script>

<style scoped>


.tabs {
  display: flex;
  background: var(--color-surface);
  border-bottom: 2px solid var(--color-border);
  padding: 0 1rem;
  position: relative;
}

.tab {
  flex: 1;
  padding: 1rem;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text-subtle);
  transition: color 0.25s ease;
  z-index: 1;
}

.tab.active {
  color: var(--color-brand);
}

.tab-indicator {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 50%;
  height: 3px;
  background: var(--color-brand);
  transition: transform 0.35s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.swipe-viewport {
  flex: 1;
  overflow: hidden;
  touch-action: pan-y;
}

.swipe-track {
  display: flex;
  width: 200%;
  height: 100%;
  transition: transform 0.35s cubic-bezier(0.2, 0.8, 0.2, 1);
  will-change: transform;
}

.swipe-track.is-dragging {
  transition: none;
}

.swipe-pane {
  width: 50%;
  height: 100%;
  overflow-y: auto;
}

.cards-list {
  min-height: 100%;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.zone-search {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.8rem 0.9rem;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.zone-search__icon {
  width: 1rem;
  height: 1rem;
  color: #64748b;
  flex-shrink: 0;
}

.zone-search__input {
  width: 100%;
  border: none;
  background: transparent;
  font-size: 0.96rem;
  color: #111827;
}

.zone-search__input:focus {
  outline: none;
}

.zone-search__input::placeholder {
  color: #94a3b8;
}

.zone-search__empty {
  margin: 0;
  padding: 0.25rem 0.1rem;
  color: #64748b;
  font-size: 0.9rem;
}

.card-option {
  background: var(--color-surface);
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: var(--shadow-card);
  border: 1px solid transparent;
}

.card-option-owned {
  border-color: rgba(102, 126, 234, 0.45);
  box-shadow: 0 6px 18px rgba(102, 126, 234, 0.15);
}

.card-option-disabled {
  opacity: 0.55;
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
}

.card-tier {
  margin: 0;
  font-size: 0.85rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-brand);
  font-weight: 700;
}

.card-content h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.2rem;
}

.card-content p {
  margin: 0 0 0.75rem 0;
  color: #999;
  font-size: 0.9rem;
}

.price {
  margin: 0;
  color: #111827;
  font-size: 1.1rem;
  font-weight: 700;
}

.price-btn {
  width: 100%;
  padding: 0.75rem;
  background: var(--color-brand);
  color: var(--color-on-brand);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

.status {
  margin: 0 0 0.75rem 0;
  font-weight: 600;
}

.status-owned {
  color: #2e7d32;
}

.status-upgrade {
  color: #764ba2;
}

.status-buy {
  color: var(--color-brand);
}

.status-hidden {
  color: #9ca3af;
}

.tickets-section {
  min-height: 100%;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.ticket-option {
  background: var(--color-surface);
  border-radius: 12px;
  padding: 1.25rem;
  box-shadow: var(--shadow-card);
}

.ticket-option-muted {
  background: #f8faff;
}

.ticket-head {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  margin-bottom: 0.4rem;
}

.ticket-head h2 {
  margin: 0;
  font-size: 1.05rem;
  color: #111827;
}

.ticket-icon {
  width: 1rem;
  height: 1rem;
  color: var(--color-brand);
}

.ticket-option p {
  margin: 0.2rem 0 1rem;
  color: #4b5563;
  font-size: 0.92rem;
}

.ticket-form {
  display: grid;
  gap: 0.65rem;
}

.ticket-form label {
  display: grid;
  gap: 0.25rem;
}

.ticket-form span {
  font-size: 0.82rem;
  color: #4b5563;
  font-weight: 600;
}

.ticket-select {
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 0.55rem 0.65rem;
  background: #fff;
  color: #111827;
  width: 100%;
}

.stop-search-wrapper {
  position: relative;
}

.stop-dropdown {
  margin: 0;
  padding: 0.2rem;
  list-style: none;
  position: absolute;
  top: calc(100% + 0.25rem);
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  max-height: 170px;
  overflow-y: auto;
  z-index: 20;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.stop-dropdown-item,
.stop-dropdown-empty {
  padding: 0.5rem 0.55rem;
  border-radius: 6px;
  font-size: 0.86rem;
}

.stop-dropdown-item {
  color: #111827;
  cursor: pointer;
}

.stop-dropdown-item:hover {
  background: #f1f5f9;
}

.stop-dropdown-empty {
  color: #64748b;
}

.ticket-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.65rem 1rem;
  border-radius: 8px;
  text-decoration: none;
  font-weight: 600;
  background: var(--color-brand);
  color: var(--color-on-brand);
  border: none;
  cursor: pointer;
}

.ticket-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ticket-message {
  margin: 0.75rem 0 0;
  color: #334155;
  font-weight: 600;
  font-size: 0.86rem;
}

.route-results {
  margin-top: 0.85rem;
  display: grid;
  gap: 0.65rem;
}

.route-result-item {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  padding: 0.65rem 0.75rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
}

.route-name {
  margin: 0;
  color: #111827;
  font-size: 0.9rem;
  font-weight: 700;
}

.route-meta {
  margin: 0.2rem 0 0;
  color: #6b7280;
  font-size: 0.8rem;
}

.route-schedule-link {
  display: inline-flex;
  width: fit-content;
  text-decoration: none;
  color: #667eea;
  font-weight: 700;
}

.route-schedule-link:hover {
  text-decoration: underline;
}

.route-actions {
  display: grid;
  justify-items: end;
  gap: 0.35rem;
}

.route-price {
  margin: 0;
  color: #111827;
  font-weight: 700;
}

.ticket-buy-btn {
  border: none;
  border-radius: 7px;
  background: #111827;
  color: #fff;
  padding: 0.45rem 0.6rem;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
}

.bottom-nav {
  margin-top: auto;
}


</style>
