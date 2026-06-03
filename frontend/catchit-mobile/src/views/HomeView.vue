<template>
  <div class="home-container app-screen">
    <!-- Header -->
    <header class="app-header">
      <h1>
        <img src="../assets/app-logo-wt.png" alt="CatchIt" class="logo" />
      </h1>
      <router-link to="/notifications" class="profile-icon" aria-label="Profile">
        <Bell class="icon-md" />
      </router-link>
    </header>

    <!-- Tabs -->
    <div class="tabs">
      <button
        :class="['tab', { active: activeTab === 'cards' }]"
        :style="{ color: cardsTabColor }"
        @click="setActiveTab('cards')"
      >
        My Cards
      </button>
      <button
        :class="['tab', { active: activeTab === 'tickets' }]"
        :style="{ color: ticketsTabColor }"
        @click="setActiveTab('tickets')"
      >
        My Tickets
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
        <div class="swipe-pane">
          <div class="tab-content">
            <div class="card-display">
              <div v-if="isCardsLoading" class="card-visual card-visual-skeleton">
                <LoaderCircle class="spinner-icon" />
              </div>
              <template v-else>
                <ZoneCard
                  v-if="currentCard"
                  class="card-visual"
                  :zone-name="currentCard.name"
                  :zone-color="currentCard.zoneColorHexCode || '#111111'"
                  :valid-until="formatDate(currentCard.validUntil)"
                  :owner-name="currentUserName"
                  owned
                />

                <div v-else class="card-visual card-visual-empty">
                  <div class="card-logo">My Card</div>
                  <div class="card-details">
                    <p>No CatchIt card yet</p>
                    <p class="date">Buy one to unlock full access</p>
                  </div>
                </div>

                <div class="card-bottom">
                  <router-link to="/cards" class="btn-primary card-action-btn">{{ currentCard ? 'Renew' : 'Buy Card' }}</router-link>
                  <router-link v-if="currentCard" :to="`/checkin/${currentCard.id}`" class="btn-secondary card-action-btn">Check In</router-link>
                  <div class="card-qr" @click="showQrModal = true" style="cursor: pointer;">
                    <QrCode class="card-qr-icon" />
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>

        <div class="swipe-pane">
          <div class="tab-content">
            <div v-if="isTicketsLoading" class="ticket-item ticket-skeleton">
              <LoaderCircle class="spinner-icon spinner-icon-dark" />
            </div>

            <template v-else>
              <div v-if="tickets.length === 0" class="empty-state">
                <p><Ticket class="empty-icon" /> No tickets yet</p>
                <router-link :to="{ path: '/cards', query: { tab: 'tickets' } }" class="btn-primary">Buy Tickets</router-link>
              </div>

              <div v-else>
                <div v-for="ticket in tickets" :key="ticket.id" class="ticket-item">
                  <div class="ticket-header">
                    <h3>Ticket</h3>
                    <span class="status-badge" :class="ticket.status.toLowerCase()">
                      {{ formatStatus(ticket.status) }}
                    </span>
                    <div class="ticket-actions">
                      <router-link :to="`/checkin/${ticket.id}`" class="btn-checkin-small">Check In</router-link>
                    </div>
                  </div>
                  <p class="expiry">Expires on {{ formatDate(ticket.validUntil) }}</p>
                  <div class="ticket-stops">
                    <p><MapPin class="icon-sm" /> {{ getTicketFromStop(ticket) }}</p>
                    <p><MapPin class="icon-sm" /> {{ getTicketToStop(ticket) }}</p>
                  </div>
                  <div class="qr-code">
                    <img
                      v-if="getQrCodeSrc(ticket.qrCode)"
                      :src="getQrCodeSrc(ticket.qrCode)"
                      alt="Ticket QR code"
                      class="ticket-qr-image"
                    />
                    <span v-else class="qr-fallback">QR code unavailable</span>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- QR Modal -->
    <div v-if="showQrModal" class="qr-modal" @click="showQrModal = false">
      <div class="qr-modal-content">
        <QrCode class="qr-modal-icon" />
        <p>Tap to close</p>
      </div>
    </div>

    <!-- Bottom Navigation -->
    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item active">
        <House class="nav-icon" />
      </router-link>
      <router-link to="/map" class="nav-item">
        <Map class="nav-icon" />
      </router-link>
      <router-link to="/cart" class="nav-item">
        <ShoppingCart class="nav-icon" />
      </router-link>
      <router-link to="/cards" class="nav-item">
        <Ticket class="nav-icon" />
      </router-link>
      <router-link to="/profile" class="nav-item">
        <User class="nav-icon" />
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Bell, House, MapPin, QrCode, Map, ShoppingCart, Ticket, User, LoaderCircle } from 'lucide-vue-next'
import ZoneCard from '../components/ZoneCard.vue'
import { useTicketViewModel, useCardViewModel, currentUser } from '../viewmodels'
import type { Ticket as UserTicket } from '../models'

const activeTab = ref<'cards' | 'tickets'>('cards')
const swipeViewport = ref<HTMLElement | null>(null)
const viewportWidth = ref(0)
const touchStartX = ref(0)
const currentDragX = ref(0)
const isDragging = ref(false)
const ticketViewModel = useTicketViewModel()
const cardViewModel = useCardViewModel()
const tickets = computed(() => ticketViewModel.tickets.value)
const userCards = computed(() => cardViewModel.userCards.value)
const currentCard = computed(() => userCards.value[0] ?? null)
const currentUserName = computed(() => currentUser.value?.name ?? '')
const isInitialLoad = ref(true)
const isCardsLoading = computed(() => isInitialLoad.value || cardViewModel.isLoading.value)
const isTicketsLoading = computed(() => isInitialLoad.value || ticketViewModel.isLoading.value)
const showQrModal = ref(false)

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
  if (activeTab.value === 'cards') return dragRatio
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
  return { transform: `translate3d(${offset}px, 0, 0)` }
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
  if (shouldMoveToTickets) activeTab.value = 'tickets'
  else if (shouldMoveToCards) activeTab.value = 'cards'
  currentDragX.value = 0
  isDragging.value = false
}

onMounted(async () => {
  updateViewportWidth()
  window.addEventListener('resize', updateViewportWidth)
  await Promise.all([
    ticketViewModel.fetchUserTickets(),
    cardViewModel.fetchUserCards(),
  ])
  isInitialLoad.value = false
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewportWidth)
})

const formatDate = (date: Date) => {
  return new Date(date).toLocaleDateString('pt-PT', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
}

const formatStatus = (status: string) => {
  const statusMap: Record<string, string> = {
    PURCHASED_BUT_NOT_VALID: 'Pending',
    VALID: 'Active',
    EXPIRED: 'Expired',
    USED: 'Used',
  }
  return statusMap[status] || status
}

const getTicketFromStop = (ticket: UserTicket) =>
  ticket.stopFrom?.name ?? 'Route details unavailable'

const getTicketToStop = (ticket: UserTicket) =>
  ticket.stopTo?.name ?? 'Route details unavailable'

const getQrCodeSrc = (qrCode: string) => {
  if (!qrCode) return ''
  if (qrCode.startsWith('data:image/')) return qrCode
  if (qrCode.startsWith('http://') || qrCode.startsWith('https://')) return qrCode
  return `data:image/png;base64,${qrCode}`
}
</script>

<style scoped>
.home-container .app-header h1 {
  display: flex;
  align-items: left;
  justify-content: left;
}

.logo {
  width: auto;
  height: 2.25rem;
  display: block;
}

.profile-icon {
  text-decoration: none;
  cursor: pointer;
  transition: transform 0.2s;
  color: white;
}

.profile-icon:hover {
  transform: scale(1.1);
}

.tabs {
  display: flex;
  background: white;
  border-bottom: 2px solid #e0e0e0;
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
  color: #999;
  transition: color 0.25s ease;
  z-index: 1;
}

.tab.active {
  color: #667eea;
}

.tab-indicator {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 50%;
  height: 3px;
  background: #667eea;
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

.tab-content {
  min-height: 100%;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: #999;
  gap: 1rem;
}

.empty-state p {
  font-size: 1.2rem;
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.icon-sm {
  width: 1rem;
  height: 1rem;
}

.empty-icon {
  width: 1.4rem;
  height: 1.4rem;
}

.ticket-stops p {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.qr-code {
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-checkin-small {
  padding: 0.35rem 0.75rem;
  background: var(--color-brand);
  color: white;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 600;
  text-decoration: none;
}

.ticket-actions {
  margin-left: auto;
}

.btn-secondary {
  width: 100%;
  padding: 0.75rem;
  background: #111827;
  color: white;
  border-radius: 8px;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.ticket-qr-image {
  width: 180px;
  height: 180px;
  object-fit: contain;
  image-rendering: pixelated;
}

.qr-fallback {
  color: #999;
  font-size: 0.9rem;
}

.card-bottom {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.card-action-btn {
  border-radius: 8px;
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.card-qr {
  border-radius: 6px;
  border: 1px solid #d4d4d8;
  background: #ededed;
  min-height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.9rem;
}

.card-qr-icon {
  width: 10.5rem;
  height: 10.5rem;
  color: #0b0b0b;
}

.qr-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.qr-modal-content {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.qr-modal-icon {
  width: 18rem;
  height: 18rem;
}

.card-display {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  margin: 0;
}

.card-visual {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 2rem 1rem;
  color: white;
  margin-bottom: 1rem;
  text-align: center;
}

.card-logo {
  font-size: 2rem;
  font-weight: bold;
  margin-bottom: 1rem;
}

.card-details p {
  margin: 0.25rem 0;
}

.date {
  font-size: 0.9rem;
  opacity: 0.9;
}

.card-visual-empty {
  background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%);
}

.ticket-item {
  background: white;
  border-radius: 12px;
  padding: 1rem;
  margin-bottom: 1rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.ticket-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.ticket-header h3 {
  margin: 0;
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
}

.status-badge.valid {
  background: #c8e6c9;
  color: #2e7d32;
}

.status-badge.expired {
  background: #ffccbc;
  color: #d84315;
}

.status-badge.used {
  background: #b3e5fc;
  color: #0277bd;
}

.expiry {
  color: #999;
  font-size: 0.9rem;
  margin: 0.5rem 0;
}

.ticket-stops {
  margin: 1rem 0;
  padding: 0.75rem;
  background: #f9f9f9;
  border-radius: 6px;
}

.ticket-stops p {
  margin: 0.25rem 0;
  font-size: 0.9rem;
}

.qr-code {
  text-align: center;
  font-size: 3rem;
  margin: 1rem 0;
}

.bottom-nav {
  margin-top: auto;
}

.card-visual-skeleton {
  background: linear-gradient(135deg, #d1d5db 0%, #9ca3af 100%);
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0;
}

.ticket-skeleton {
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  width: 100%;
}

.spinner-icon {
  width: 2.5rem;
  height: 2.5rem;
  animation: spin 1s linear infinite;
  color: #ffffff;
}

.spinner-icon-dark {
  color: #9ca3af;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>