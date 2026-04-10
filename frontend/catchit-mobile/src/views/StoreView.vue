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
            <article
              v-for="card in sortedPlans"
              :key="card.id"
              class="card-option"
              :class="cardClass(card.tier)"
            >
              <div class="card-content">
                <div class="card-head">
                  <p class="card-tier">{{ card.name }}</p>
                  <p class="price">€{{ card.price.toFixed(2) }}</p>
                </div>

                <p>{{ card.description }}</p>

                <p v-if="currentTier === card.tier" class="status status-owned">Current plan</p>
                <p v-else-if="canUpgrade(card.tier)" class="status status-upgrade">Upgrade available</p>
                <p v-else-if="!currentTier" class="status status-buy">Available to buy</p>
                <p v-else class="status status-hidden">Not available from your current tier</p>

                <button v-if="actionLabel(card.tier)" class="price-btn" @click="addCardToCart(card)">
                  Add to cart
                </button>
              </div>
            </article>
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
                  <select v-model="fromStopId" class="ticket-select">
                    <option disabled value="">Select origin</option>
                    <option v-for="stop in availableStops" :key="stop.id" :value="stop.id">
                      {{ stop.name }}
                    </option>
                  </select>
                </label>

                <label>
                  <span>To</span>
                  <select v-model="toStopId" class="ticket-select">
                    <option disabled value="">Select destination</option>
                    <option v-for="stop in availableStops" :key="stop.id" :value="stop.id">
                      {{ stop.name }}
                    </option>
                  </select>
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
                    <p class="route-name">{{ result.fromStop.name }} → {{ result.toStop.name }}</p>
                    <p class="route-meta">{{ result.departureTime }} - {{ result.arrivalTime }}</p>
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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ArrowLeft, House, Map, MapPin, ShoppingCart, Ticket, User } from 'lucide-vue-next'
import { useCardViewModel, useCheckoutViewModel, useTravelViewModel } from '../viewmodels'
import { mockAPI } from '../services/api/mockAPI'
import type { CardTier, Stop, Vehicle } from '../models'

type RouteResult = {
  routeId: string
  fromStop: Stop
  toStop: Stop
  departureTime: string
  arrivalTime: string
  price: number
  vehicle: Vehicle
}

const cardViewModel = useCardViewModel()
const travelViewModel = useTravelViewModel()
const checkoutViewModel = useCheckoutViewModel()
const activeTab = ref<'cards' | 'tickets'>('cards')
const swipeViewport = ref<HTMLElement | null>(null)
const viewportWidth = ref(0)
const touchStartX = ref(0)
const currentDragX = ref(0)
const isDragging = ref(false)
const availableStops = ref<Stop[]>([])
const fromStopId = ref('')
const toStopId = ref('')
const departureDate = ref(new Date().toISOString().split('T')[0])
const ticketMessage = ref('')

const tierOrder: CardTier[] = ['weekly', 'monthly', 'yearly']

const sortedPlans = computed(() =>
  [...cardViewModel.availableCards.value].sort(
    (left, right) => tierOrder.indexOf(left.tier ?? 'weekly') - tierOrder.indexOf(right.tier ?? 'weekly')
  )
)

const currentTier = computed<CardTier | null>(() => {
  const ownedTiers = cardViewModel.userCards.value
    .map((card) => card.tier)
    .filter((tier): tier is CardTier => !!tier)
  return ownedTiers.sort((left, right) => tierOrder.indexOf(right) - tierOrder.indexOf(left))[0] ?? null
})

const tierIndex = (tier: CardTier) => tierOrder.indexOf(tier)

const canUpgrade = (tier?: CardTier) => {
  if (!tier || !currentTier.value) return false
  return tierIndex(tier) > tierIndex(currentTier.value)
}

const actionLabel = (tier?: CardTier) => {
  if (!tier) return ''
  if (!currentTier.value) return 'Buy'
  if (tier === currentTier.value) return 'Renew'
  if (canUpgrade(tier)) return 'Upgrade'
  return ''
}

const cardClass = (tier?: CardTier) => ({
  'card-option-owned': tier && tier === currentTier.value,
  'card-option-disabled': !!tier && !!currentTier.value && tierIndex(tier) < tierIndex(currentTier.value),
})

const canSearchTickets = computed(() =>
  !!fromStopId.value && !!toStopId.value && fromStopId.value !== toStopId.value && !!departureDate.value
)

const searchResults = computed(() => travelViewModel.searchResults.value as RouteResult[])

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
    ticketMessage.value = 'Please select different origin and destination stops.'
    return
  }

  ticketMessage.value = ''
  const results = await travelViewModel.searchRoutes(fromStopId.value, toStopId.value, departureDate.value)
  if (!results.length) {
    ticketMessage.value = 'No routes found for your selection.'
  }
}

const addCardToCart = (card: { id: string; name: string; price: number; description?: string; tier?: CardTier }) => {
  checkoutViewModel.addCardToCart(card as any)
  ticketMessage.value = ''
}

const addTicketToCart = (result: RouteResult) => {
  checkoutViewModel.addTicketToCart(result)
  ticketMessage.value = 'Added to cart.'
}

onMounted(async () => {
  updateViewportWidth()
  window.addEventListener('resize', updateViewportWidth)

  await cardViewModel.fetchUserCards()
  await cardViewModel.fetchAvailableCards()

  const stopsResponse = await mockAPI.getStops()
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
