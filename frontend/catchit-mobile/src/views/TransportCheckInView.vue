<template>
  <div class="container app-screen">
    <header class="app-header">
      <router-link to="/home" class="back-btn" aria-label="Back"><ArrowLeft class="icon-md" /></router-link>
      <h1>Check In</h1>
      <div style="width: 1rem"></div>
    </header>

    <div class="content">
      <!-- Estado: checkIn bem sucedido -->
      <div v-if="checkInSuccess" class="success-state">
        <div class="success-icon">✅</div>
        <h2>Check In successful!</h2>
        <p>You are now on a trip. Click below when you exit.</p>
        <button class="btn-checkout" @click="handleCheckOut" :disabled="isCheckingOut">
          {{ isCheckingOut ? 'Processing...' : 'Check Out' }}
        </button>
        <p v-if="checkOutMessage" :class="checkOutMessage.success ? 'msg-success' : 'msg-error'">
          {{ checkOutMessage.text }}
        </p>
      </div>

      <!-- Estado: formulário de checkIn -->
      <div v-else class="form-state">

        <!-- Título selecionado -->
        <section class="section">
          <h2>Your title</h2>
          <div class="title-info">
            <p class="option-title">{{ titleLabel }}</p>
          </div>
        </section>

        <!-- Selecionar trip -->
        <section class="section">
          <h2>Select trip</h2>
          <p v-if="isLoadingTrips" class="loading">Loading trips...</p>
          <p v-else-if="activeTrips.length === 0" class="empty">No active trips available.</p>
          <div v-else class="options-list">
            <label
              v-for="trip in activeTrips"
              :key="trip.id"
              class="option-item"
              :class="{ selected: selectedTripId === trip.id }"
            >
              <input type="radio" :value="trip.id" v-model="selectedTripId" />
              <div class="option-info">
                <p class="option-title">🚌 {{ trip.routeName }}</p>
                <p class="option-meta">Started at {{ trip.startTime }}</p>
              </div>
            </label>
          </div>
        </section>

        <p v-if="errorMessage" class="msg-error">{{ errorMessage }}</p>

        <button
          class="btn-checkin"
          :disabled="!selectedTripId || isCheckingIn"
          @click="handleCheckIn"
        >
          {{ isCheckingIn ? 'Processing...' : 'Check In' }}
        </button>
      </div>
    </div>

    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><House class="nav-icon" /></router-link>
      <router-link to="/map" class="nav-item"><Map class="nav-icon" /></router-link>
      <router-link to="/cart" class="nav-item"><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/cards" class="nav-item"><Ticket class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item"><User class="nav-icon" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ArrowLeft, House, Map, ShoppingCart, Ticket, User } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import { catchitApi } from '../services/api/catchitApi'
import { currentUser } from '../viewmodels'
import { requestJson } from '../services/api/http'

const route = useRoute()
const titleId = route.params.titleId as string

type TripOption = {
  id: string
  routeName: string
  startTime: string
}

const activeTrips = ref<TripOption[]>([])
const selectedTripId = ref('')
const isLoadingTrips = ref(false)
const isCheckingIn = ref(false)
const isCheckingOut = ref(false)
const checkInSuccess = ref(false)
const errorMessage = ref('')
const checkOutMessage = ref<{ success: boolean; text: string } | null>(null)
const titleLabel = ref('Loading...')

const formatTime = (date: string) =>
  new Date(date).toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit' })

const loadTitleInfo = async () => {
  if (!currentUser.value) return

  // Tenta encontrar nos tickets
  const ticketsResponse = await catchitApi.getUserTickets(currentUser.value.id)
  const ticket = ticketsResponse.data?.find(t => t.id === titleId)
  if (ticket) {
    titleLabel.value = `🎟️ Ticket: ${ticket.stopFrom?.name ?? '?'} → ${ticket.stopTo?.name ?? '?'}`
    return
  }

  // Tenta encontrar nos cards
  const cardsResponse = await catchitApi.getUserCards(currentUser.value.id)
  const card = cardsResponse.data?.find(c => c.id === titleId)
  if (card) {
    titleLabel.value = `🎫 Card: ${card.name}`
    return
  }

  titleLabel.value = 'Title not found'
}

const loadActiveTrips = async () => {
  isLoadingTrips.value = true
  try {
    const response = await requestJson<Array<{ id: string; startTime: string; routeName: string }>>('/api/trips/active')
    if (response.success && response.data) {
      activeTrips.value = response.data.map(t => ({
        id: t.id,
        routeName: t.routeName ?? 'Unknown route',
        startTime: formatTime(t.startTime),
      }))
    }
  } finally {
    isLoadingTrips.value = false
  }
}

const handleCheckIn = async () => {
  if (!selectedTripId.value) return
  isCheckingIn.value = true
  errorMessage.value = ''
  try {
    const response = await requestJson<{ success: boolean; message: string }>('/api/checkin', {
      method: 'POST',
      body: JSON.stringify({ titleId, tripId: selectedTripId.value }),
    })
    if (response.success && response.data?.success) {
      checkInSuccess.value = true
    } else {
      errorMessage.value = response.data?.message ?? response.error ?? 'Check in failed'
    }
  } finally {
    isCheckingIn.value = false
  }
}

const handleCheckOut = async () => {
  isCheckingOut.value = true
  checkOutMessage.value = null
  try {
    const response = await requestJson<{ success: boolean; message: string }>('/api/checkout-transport', {
      method: 'POST',
      body: JSON.stringify({ titleId, tripId: selectedTripId.value }),
    })
    if (response.success && response.data) {
      checkOutMessage.value = { success: response.data.success, text: response.data.message }
    }
  } finally {
    isCheckingOut.value = false
  }
}

onMounted(() => {
  void loadTitleInfo()
  void loadActiveTrips()
})
</script>

<style scoped>
.content {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.section {
  background: var(--color-surface);
  border-radius: 12px;
  padding: 1rem;
  box-shadow: var(--shadow-card);
}

.section h2 {
  font-size: 0.9rem;
  font-weight: 700;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0 0 0.75rem 0;
}

.title-info {
  padding: 0.5rem 0;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 220px;
  overflow-y: auto;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.option-item.selected {
  border-color: var(--color-brand);
  background: rgba(102, 126, 234, 0.05);
}

.option-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.option-title {
  margin: 0;
  font-weight: 600;
  font-size: 0.9rem;
  color: #111827;
}

.option-meta {
  margin: 0;
  font-size: 0.8rem;
  color: #6b7280;
}

.btn-checkin {
  width: 100%;
  padding: 0.85rem;
  background: var(--color-brand);
  color: white;
  border: none;
  border-radius: 10px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
}

.btn-checkin:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.success-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 2rem 1rem;
  text-align: center;
}

.success-icon {
  font-size: 4rem;
}

.success-state h2 {
  margin: 0;
  color: #111827;
}

.success-state p {
  margin: 0;
  color: #6b7280;
}

.btn-checkout {
  width: 100%;
  padding: 0.85rem;
  background: #111827;
  color: white;
  border: none;
  border-radius: 10px;
  font-weight: 700;
  font-size: 1rem;
  cursor: pointer;
}

.btn-checkout:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading, .empty {
  color: #6b7280;
  font-size: 0.9rem;
  margin: 0;
}

.msg-error {
  color: #dc2626;
  font-size: 0.875rem;
  font-weight: 600;
  margin: 0;
}

.msg-success {
  color: #16a34a;
  font-size: 0.875rem;
  font-weight: 600;
  margin: 0;
}

.bottom-nav {
  margin-top: auto;
}
</style>