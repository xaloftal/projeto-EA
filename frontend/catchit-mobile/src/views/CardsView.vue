<template>
  <div class="container">
    <header class="app-header">
      <router-link to="/home" class="back-btn" aria-label="Back"><ArrowLeft class="icon-md" /></router-link>
      <h1>Shopping Cart</h1>
      <div style="width: 1rem"></div>
    </header>

    <div class="tabs">
      <button
        :class="['tab', { active: activeTab === 'cards' }]"
        @click="activeTab = 'cards'"
      >
        Buy Cards
      </button>
      <button
        :class="['tab', { active: activeTab === 'tickets' }]"
        @click="activeTab = 'tickets'"
      >
        Buy Tickets
      </button>
    </div>

    <div v-if="activeTab === 'cards'" class="cards-list">
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

          <button
            v-if="actionLabel(card.tier)"
            class="price-btn"
            @click="purchasePlan(card.id, card.tier)"
          >
            {{ actionLabel(card.tier) }}
          </button>
        </div>
      </article>
    </div>

    <div v-else class="tickets-section">
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
              <button class="ticket-buy-btn" @click="buyTicket(result)">Buy Ticket</button>
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

    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><House class="nav-icon" /></router-link>
      <router-link to="/map" class="nav-item"><Map class="nav-icon" /></router-link>
      <router-link to="/cards" class="nav-item active"><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/notifications" class="nav-item"><Bell class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item"><User class="nav-icon" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft, Bell, House, Map, MapPin, ShoppingCart, Ticket, User } from 'lucide-vue-next'
import { useCardViewModel, useTicketViewModel, useTravelViewModel } from '../viewmodels'
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
const ticketViewModel = useTicketViewModel()
const travelViewModel = useTravelViewModel()
const activeTab = ref<'cards' | 'tickets'>('cards')
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

const buyTicket = async (result: RouteResult) => {
  ticketMessage.value = ''
  const bookedTrip = await travelViewModel.bookTravel(result.routeId, `trip_${Date.now()}`)
  const tripId = bookedTrip?.id ?? `trip_${Date.now()}`
  const success = await ticketViewModel.purchaseTickets(tripId, 1, result.price)
  ticketMessage.value = success ? 'Ticket purchased successfully.' : 'Unable to complete ticket purchase.'
}

onMounted(async () => {
  await cardViewModel.fetchUserCards()
  await cardViewModel.fetchAvailableCards()

  const stopsResponse = await mockAPI.getStops()
  if (stopsResponse.success && stopsResponse.data) {
    availableStops.value = stopsResponse.data
  }
})

const purchasePlan = async (cardId: string, tier?: CardTier) => {
  if (!tier) return
  const success = await cardViewModel.purchaseCard(cardId, tier)
  if (success) {
    await cardViewModel.fetchUserCards()
  }
}
</script>

<style scoped>
.container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}

.app-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.app-header h1 {
  font-size: 1.2rem;
  margin: 0;
  flex: 1;
  text-align: center;
}

.back-btn {
  cursor: pointer;
  text-decoration: none;
  color: white;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.tabs {
  display: flex;
  background: white;
  border-bottom: 2px solid #e0e0e0;
  padding: 0 1rem;
}

.tab {
  flex: 1;
  padding: 1rem;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 600;
  color: #999;
  transition: all 0.3s;
}

.tab.active {
  border-bottom-color: #667eea;
  color: #667eea;
}

.cards-list {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.card-option {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
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
  color: #667eea;
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
  background: #667eea;
  color: white;
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
  color: #667eea;
}

.status-hidden {
  color: #9ca3af;
}

.tickets-section {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.ticket-option {
  background: white;
  border-radius: 12px;
  padding: 1.25rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
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
  color: #667eea;
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
  background: #667eea;
  color: #fff;
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
  display: flex;
  justify-content: space-around;
  background: white;
  border-top: 1px solid #e0e0e0;
  padding: 0.5rem 0;
  margin-top: auto;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  padding: 0.75rem 1.5rem;
  text-decoration: none;
  color: #999;
  font-size: 1.5rem;
  transition: color 0.3s;
}

.icon-md {
  width: 1.25rem;
  height: 1.25rem;
}

.nav-item.active {
  color: #667eea;
}
</style>
