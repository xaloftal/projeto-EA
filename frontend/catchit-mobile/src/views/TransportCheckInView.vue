<template>
  <div class="container app-screen">
    <header class="app-header">
      <router-link to="/home" class="back-btn" aria-label="Back">
        <ArrowLeft class="icon-md" />
      </router-link>
      <h1>Check In</h1>
      <div style="width: 1rem"></div>
    </header>

    <div class="content">
      <div v-if="checkInSuccess" class="success-state">
        <div class="success-icon">
          <CheckCircle class="success-icon-svg" />
        </div>
        <h2>You are now on a trip!</h2>
        <p>Present the QR Code below if requested.</p>

        <!-- Badge para identificar o tipo -->
        <div class="title-type-badge">
          <span :class="['badge', isTicketTitle ? 'badge-ticket' : 'badge-card']">
            {{ isTicketTitle ? 'Ticket' : 'Card' }}
          </span>
        </div>

        <!-- QR Code para ambos (ticket e card) -->
        <div class="qr-container">
          <div v-if="isLoadingQr" class="qr-skeleton">
            <LoaderCircle class="spinner-icon" />
            <p>Loading validation code...</p>
          </div>
          <img v-else-if="qrCodeUrl" :src="qrCodeUrl" alt="Validation QR Code" class="qr-image" />
          <div v-else class="qr-error">
            <p>QR Code unavailable for validation</p>
          </div>
        </div>

        <!-- Informação adicional do card -->
        <div v-if="!isTicketTitle && cardInfo" class="card-info">
          <p><strong>Zone:</strong> {{ cardInfo.zoneName || 'All zones' }}</p>
          <p><strong>Valid until:</strong> {{ formatDate(cardInfo.validUntil) }}</p>
        </div>

        <!-- Banner de situação do checkout (apenas para Tickets) -->
        <div v-if="isTicketTitle && checkoutSituation" :class="['situation-banner', getSituationClass(checkoutSituation.situation)]">
          <AlertTriangle v-if="checkoutSituation.situation === 'AFTER_DESTINATION'" class="banner-icon" />
          <InfoIcon v-else-if="checkoutSituation.situation === 'BEFORE_DESTINATION'" class="banner-icon" />
          <CheckCircle v-else class="banner-icon" />
          <div class="banner-content">
            <h3>{{ getSituationTitle(checkoutSituation.situation) }}</h3>
            <p>{{ checkoutSituation.message }}</p>
            <p v-if="checkoutSituation.currentStopName" class="stop-info">
              <strong>Current Stop:</strong> {{ checkoutSituation.currentStopName }}
            </p>
            <p v-if="checkoutSituation.destinationStopName" class="stop-info">
              <strong>Destiny:</strong> {{ checkoutSituation.destinationStopName }}
            </p>
          </div>
        </div>

        <button class="btn-checkout" @click="openCheckoutModal" :disabled="isCheckingOut">
          {{ isCheckingOut ? 'Processing...' : 'Check Out' }}
        </button>

        <div v-if="checkOutMessage" :class="['checkout-message', checkOutMessage.success ? 'msg-success' : 'msg-warning']">
          <AlertCircle v-if="!checkOutMessage.success" class="icon-sm" />
          <CheckCircle v-else class="icon-sm" />
          <p>{{ checkOutMessage.text }}</p>
        </div>
        <p v-if="errorMessage" class="msg-error">{{ errorMessage }}</p>
      </div>

      <div v-else class="form-state">
        <section class="section">
          <h2>Your title</h2>
          <div class="title-info flex-center">
            <Ticket v-if="isTicketTitle" class="icon-sm" />
            <CreditCard v-else class="icon-sm" />
            <p class="option-title">{{ titleLabel }}</p>
          </div>
        </section>

        <section class="section route-planner-section">
          <h2>Select trip</h2>
          <p v-if="isLoadingTrips && displayedTrips.length === 0" class="loading">Loading trips...</p>
          <p v-else-if="activeTrips.length === 0 && !hasMoreTrips" class="empty">No active trips available.</p>
          <div v-else class="options-list" ref="scrollContainer" @scroll="handleScroll">
            <label v-for="trip in displayedTrips" :key="trip.id" class="option-item"
              :class="{ selected: selectedTripId === trip.id }">
              <input type="radio" :value="trip.id" v-model="selectedTripId" />
              <div class="option-info">
                <p class="option-title">{{ trip.routeName }}</p>
                <p class="option-meta">Next trip at {{ trip.startTime }}</p>
              </div>
            </label>
            <div v-if="isLoadingMore" class="loading-more">
              <LoaderCircle class="spinner-icon-small" />
              <span>Loading more trips...</span>
            </div>
            <div v-if="!hasMoreTrips && displayedTrips.length > 0" class="end-of-list"></div>
          </div>
        </section>

        <button class="btn-checkin" :disabled="!selectedTripId || isCheckingIn" @click="openCheckinModal">
          {{ isCheckingIn ? 'Processing...' : 'Check In' }}
        </button>
      </div>
    </div>

    <!-- Modal de Confirmação de Check-in -->
    <div v-if="showCheckinModal" class="modal-overlay" @click.self="closeModals">
      <div class="modal-container">
        <div class="modal-header">
          <h3>Confirm Check-in</h3>
          <button class="modal-close" @click="closeModals">×</button>
        </div>
        <div class="modal-body">
          <AlertCircle class="modal-icon" />
          <p>Are you sure you want to check in for this trip?</p>
          <div class="trip-details">
            <p><strong>Route:</strong> {{ selectedTripDetails?.routeName || 'N/A' }}</p>
            <p><strong>Departure:</strong> {{ selectedTripDetails?.startTime || 'N/A' }}</p>
          </div>
          <div v-if="!isTicketTitle && cardInfo?.zoneName" class="zone-warning">
            <p><strong>Card Zone:</strong> {{ cardInfo.zoneName }}</p>
            <p v-if="tripZones.length > 0" :class="isZoneValid ? 'zone-valid' : 'zone-invalid'">
              <span v-if="isZoneValid">✅ This trip passes through your card's zone</span>
              <span v-else>⚠️ This trip does NOT pass through zone {{ cardInfo.zoneName }}</span>
            </p>
          </div>
          <p class="warning-text">After the check-in, your title will be validated and cannot be canceled.</p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeModals">Cancel</button>
          <button class="btn-confirm" @click="confirmCheckIn" :disabled="isCheckingIn">
            {{ isCheckingIn ? 'Processing...' : 'Confirm Check-in' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal de Confirmação de Check-out -->
    <div v-if="showCheckoutModal" class="modal-overlay" @click.self="closeModals">
      <div class="modal-container">
        <div class="modal-header">
          <h3>Confirm Check-out</h3>
          <button class="modal-close" @click="closeModals">×</button>
        </div>
        <div class="modal-body">
          <AlertCircle class="modal-icon" />
          <p>Are you sure you want to check out?</p>
          <div class="trip-details">
            <p><strong>Destiny:</strong> {{ checkoutSituation?.destinationStopName || 'N/A' }}</p>
            <p><strong>Current Stop:</strong> {{ checkoutSituation?.currentStopName || 'N/A' }}</p>
          </div>
          <p v-if="checkoutSituation?.situation === 'AFTER_DESTINATION'" class="warning-text danger">
            ⚠️ Attention: You have already passed the destination stop. The check-out will be registered as outside the allowed zone.
          </p>
          <p v-else class="warning-text">
            After the check-out, your title will be marked as used.
          </p>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="closeModals">Cancel</button>
          <button class="btn-confirm" @click="confirmCheckOut" :disabled="isCheckingOut">
            {{ isCheckingOut ? 'Processing...' : 'Confirm Check-out' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Toast de feedback -->
    <div v-if="toastMessage" :class="['toast', toastType]">
      <CheckCircle v-if="toastType === 'success'" class="toast-icon" />
      <AlertCircle v-else class="toast-icon" />
      <span>{{ toastMessage }}</span>
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
import { onMounted, computed, ref, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  House,
  ShoppingCart,
  Ticket,
  User,
  LoaderCircle,
  CheckCircle,
  CreditCard,
  AlertTriangle,
  AlertCircle,
  InfoIcon
} from 'lucide-vue-next'
import { useTransportViewModel } from '../viewmodels/index'
import { catchitApi } from '../services/api/catchitApi'

// Definir interfaces
interface CheckoutSituation {
  situation: string
  currentStopName: string | null
  destinationStopName: string | null
  message: string
}

interface CardInfo {
  zoneName: string | null
  validUntil: string
}

interface TripStop {
  id: string
  name: string
  stopCode: string
  sequence: number
  zoneName: string
  zoneId: string
  latitude: number
  longitude: number
}

const route = useRoute()
const router = useRouter()
const titleId = route.params.titleId as string

const transport = useTransportViewModel(titleId)
const {
  activeTrips,
  selectedTripId,
  isLoadingTrips,
  isCheckingIn,
  isCheckingOut,
  checkInSuccess,
  errorMessage,
  checkOutMessage,
  titleLabel,
  isTicketTitle,
  loadTitleInfo,
  handleCheckIn: originalHandleCheckIn,
  handleCheckOut: originalHandleCheckOut
} = transport

// Estado local
const isLoadingQr = ref<boolean>(false)
const qrCodeUrl = ref<string | null>(null)
const checkoutSituation = ref<CheckoutSituation | null>(null)
const cardInfo = ref<CardInfo | null>(null)
const cardZoneName = ref<string | null>(null)
const isLoadingMore = ref<boolean>(false)
const currentPage = ref<number>(1)
const scrollContainer = ref<HTMLElement | null>(null)
const ITEMS_PER_PAGE = 15

// Estado dos modais
const showCheckinModal = ref<boolean>(false)
const showCheckoutModal = ref<boolean>(false)

// Estado do toast
const toastMessage = ref<string>('')
const toastType = ref<'success' | 'error'>('success')

// Estado para stops da trip e validação de zona
const tripStops = ref<TripStop[]>([])
const isLoadingStops = ref(false)
const tripZones = ref<string[]>([])
const isZoneValid = ref(false)

// Trip selecionada detalhes
const selectedTripDetails = computed(() => {
  return activeTrips.value.find(trip => trip.id === selectedTripId.value)
})

const displayedTrips = computed(() => {
  return activeTrips.value.slice(0, currentPage.value * ITEMS_PER_PAGE)
})

const hasMoreTrips = computed(() => {
  return displayedTrips.value.length < activeTrips.value.length
})

// Formatar data
const formatDate = (dateString: string) => {
  if (!dateString) return 'N/A'
  try {
    return new Date(dateString).toLocaleDateString('pt-PT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    })
  } catch {
    return dateString
  }
}

const formatTime = (date?: string) => (date ? new Date(date).toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit' }) : '')

const getSituationClass = (situation: string) => {
  switch (situation) {
    case 'BEFORE_DESTINATION': return 'situation-info'
    case 'AT_DESTINATION': return 'situation-success'
    case 'AFTER_DESTINATION': return 'situation-warning'
    default: return ''
  }
}

const getSituationTitle = (situation: string) => {
  switch (situation) {
    case 'BEFORE_DESTINATION': return 'You are on the way to the destination'
    case 'AT_DESTINATION': return 'You are at the destination'
    case 'AFTER_DESTINATION': return 'Attention: Passed the destination!'
    default: return 'Checkout Situation'
  }
}

const showToast = (message: string, type: 'success' | 'error') => {
  toastMessage.value = message
  toastType.value = type
  setTimeout(() => { toastMessage.value = '' }, 3000)
}

// Buscar stops da trip
const fetchTripStops = async (tripId: string) => {
  isLoadingStops.value = true
  try {
    const response = await catchitApi.getTripStopsWithZones(tripId)
    if (response.success && response.data) {
      // Remover duplicados por sequence - usando objeto simples
      const uniqueStopsMap: { [key: number]: TripStop } = {}
      for (const stop of response.data) {
        if (!uniqueStopsMap[stop.sequence]) {
          uniqueStopsMap[stop.sequence] = stop
        }
      }
      tripStops.value = Object.values(uniqueStopsMap)
      const zones: string[] = []
      for (const stop of tripStops.value) {
        if (stop.zoneName && !zones.includes(stop.zoneName)) {
          zones.push(stop.zoneName)
        }
      }
      tripZones.value = zones
      
      if (!isTicketTitle.value && cardZoneName.value) {
        isZoneValid.value = zones.includes(cardZoneName.value)
      }
      return true
    }
    return false
  } catch (error) {
    console.error('Error fetching trip stops:', error)
    return false
  } finally {
    isLoadingStops.value = false
  }
}

// Filtrar trips por zona do cartão
const filterTripsByZone = async (trips: any[]): Promise<any[]> => {
  if (isTicketTitle.value || !cardZoneName.value) {
    return trips
  }
  
  const filtered: any[] = []
  for (const trip of trips) {
    try {
      const response = await catchitApi.getTripStopsWithZones(trip.id)
      if (response.success && response.data) {
        // Remover duplicados por sequence - usando objeto simples
        const uniqueStopsMap: { [key: number]: any } = {}
        for (const stop of response.data) {
          if (!uniqueStopsMap[stop.sequence]) {
            uniqueStopsMap[stop.sequence] = stop
          }
        }
        const stops = Object.values(uniqueStopsMap)
        const tripZonesSet: string[] = []
        for (const s of stops) {
          if (s.zoneName && !tripZonesSet.includes(s.zoneName)) {
            tripZonesSet.push(s.zoneName)
          }
        }
        
        if (tripZonesSet.includes(cardZoneName.value)) {
          filtered.push(trip)
        }
      }
    } catch (error) {
      console.error('Error filtering trip:', trip.id, error)
    }
  }
  return filtered
}

// Load active trips com filtro por zona
const loadActiveTripsFiltered = async () => {
  isLoadingTrips.value = true
  try {
    const response = titleId
      ? await catchitApi.getActiveTripsForTitle(titleId)
      : await catchitApi.getActiveTrips()

    if (response.success && response.data) {
      const now = new Date().getTime()
      const routeBestTrip = new Map<string, { trip: any; diff: number }>()

      for (const t of response.data) {
        const rName = t.routeName ?? 'Unknown route'
        const tTime = new Date(t.startTime || '').getTime()
        const diff = isNaN(tTime) ? Number.MAX_VALUE : Math.abs(tTime - now)

        if (!routeBestTrip.has(rName)) {
          routeBestTrip.set(rName, { trip: t, diff })
        } else {
          const currentBest = routeBestTrip.get(rName)!
          if (diff < currentBest.diff) {
            routeBestTrip.set(rName, { trip: t, diff })
          }
        }
      }

      const uniqueTrips = Array.from(routeBestTrip.values()).map(({ trip: t }) => ({
        id: t.id,
        routeName: t.routeName ?? 'Unknown route',
        startTime: formatTime(t.startTime),
        zoneName: t.zoneName
      }))
      
      const filtered = await filterTripsByZone(uniqueTrips)
      activeTrips.value = filtered
      
      if (filtered.length === 0 && cardZoneName.value) {
        showToast(`No trips available for your card's zone: ${cardZoneName.value}`, 'error')
      }
    }
  } finally {
    isLoadingTrips.value = false
  }
}

const validateCardBeforeCheckin = (): boolean => {
  if (isTicketTitle.value) return true
  if (!cardZoneName.value) return true
  
  if (!isZoneValid.value) {
    const availableZones = tripZones.value.length > 0 ? tripZones.value.join(', ') : 'unknown'
    showToast(`Card zone mismatch! Your card is for zone ${cardZoneName.value}. This trip serves: ${availableZones}`, 'error')
    return false
  }
  return true
}

const openCheckinModal = () => {
  if (!selectedTripId.value) {
    showToast('Please select a trip first', 'error')
    return
  }
  showCheckinModal.value = true
}

const openCheckoutModal = () => {
  if (!selectedTripId.value) {
    showToast('Please select a trip first', 'error')
    return
  }
  showCheckoutModal.value = true
}

const closeModals = () => {
  showCheckinModal.value = false
  showCheckoutModal.value = false
}

const fetchCardInfo = async () => {
  if (!titleId || isTicketTitle.value) return
  
  try {
    const token = localStorage.getItem('authToken')
    const response = await fetch(`/api/cards/${titleId}`, {
      headers: { 'Authorization': token ? `Bearer ${token}` : '' }
    })
    
    if (response.ok) {
      const card = await response.json()
      cardInfo.value = {
        zoneName: card.zone?.name || null,
        validUntil: card.validUntil
      }
      cardZoneName.value = card.zone?.name || null
      
      if (cardZoneName.value) {
        await loadActiveTripsFiltered()
      } else {
        await loadActiveTripsFiltered()
      }
    }
  } catch (err) {
    console.error('Error fetching card info:', err)
  }
}

const fetchQrCode = async () => {
  if (!titleId) return
  isLoadingQr.value = true
  try {
    const imageUrl = isTicketTitle.value 
      ? await catchitApi.getTicketQrCode(titleId)
      : await catchitApi.getCardQrCode(titleId)
    qrCodeUrl.value = imageUrl
  } catch (err) {
    console.error('Error fetching QR Code:', err)
  } finally {
    isLoadingQr.value = false
  }
}

const fetchCheckoutSituation = async () => {
  if (!titleId || !selectedTripId.value || !isTicketTitle.value) return
  try {
    const response = await catchitApi.getCheckoutSituation(titleId, selectedTripId.value)
    if (response.success && response.data) {
      checkoutSituation.value = response.data
    }
  } catch (err) {
    console.error('Error fetching checkout situation:', err)
  }
}

const confirmCheckIn = async () => {
  if (!selectedTripId.value) return
  closeModals()
  
  await fetchTripStops(selectedTripId.value)
  if (!validateCardBeforeCheckin()) return
  
  await originalHandleCheckIn()
  
  if (checkInSuccess.value) {
    showToast('Check-in completed successfully!', 'success')
    await fetchQrCode()
    if (isTicketTitle.value) {
      await fetchCheckoutSituation()
    }
  } else if (errorMessage.value) {
    showToast(errorMessage.value, 'error')
  }
}

const confirmCheckOut = async () => {
  closeModals()
  await originalHandleCheckOut()
  setTimeout(() => {
    if (checkOutMessage.value) {
      if (checkOutMessage.value.success) {
        showToast(checkOutMessage.value.text, 'success')
        setTimeout(() => router.push('/profile?tab=tickets'), 1500)
      } else {
        showToast(checkOutMessage.value.text, 'error')
      }
    } else if (errorMessage.value) {
      showToast(errorMessage.value, 'error')
    }
  }, 500)
}

const handleScroll = () => {
  const target = scrollContainer.value
  if (!target) return
  
  if (target.scrollTop + target.clientHeight >= target.scrollHeight * 0.8) {
    if (hasMoreTrips.value && !isLoadingMore.value) {
      isLoadingMore.value = true
      setTimeout(() => {
        currentPage.value++
        isLoadingMore.value = false
      }, 500)
    }
  }
}

onUnmounted(() => {
  if (qrCodeUrl.value) URL.revokeObjectURL(qrCodeUrl.value)
})

watch(selectedTripId, async () => {
  if (selectedTripId.value && checkInSuccess.value && isTicketTitle.value) {
    await fetchCheckoutSituation()
  }
  tripStops.value = []
  tripZones.value = []
  isZoneValid.value = false
})

onMounted(async () => {
  await loadTitleInfo()
  await fetchCardInfo()
  if (!cardZoneName.value) {
    await loadActiveTripsFiltered()
  }
})
</script>

<style scoped>
/* ... manter todos os estilos existentes ... */

/* Adicionar estilos para validação de zona */
.zone-warning {
  background: #f3f4f6;
  border-radius: 8px;
  padding: 0.75rem;
  margin: 0.75rem 0;
  text-align: left;
}

.zone-valid {
  color: #10b981;
  font-weight: 500;
}

.zone-invalid {
  color: #ef4444;
  font-weight: 500;
}

/* Adicionar qualquer estilo existente que possa faltar */
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

.flex-center {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.success-icon-svg {
  width: 3rem;
  height: 3rem;
  color: var(--color-primary);
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
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
  gap: 1.25rem;
  padding: 2rem 1rem;
  text-align: center;
}

.success-icon {
  font-size: 3.5rem;
}

.success-state h2 {
  margin: 0;
  color: #111827;
}

.success-state p {
  margin: 0;
  color: #6b7280;
  font-size: 0.95rem;
}

.title-type-badge {
  margin-top: -0.5rem;
}

.badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.7rem;
  font-weight: 600;
}

.badge-ticket {
  background: #dbeafe;
  color: #1e40af;
}

.badge-card {
  background: #fef3c7;
  color: #92400e;
}

.qr-container {
  background: white;
  border: 2px dashed #e5e7eb;
  border-radius: 16px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 240px;
  min-height: 240px;
  margin: 0.5rem 0;
}

.qr-image {
  width: 200px;
  height: 200px;
  object-fit: contain;
  image-rendering: pixelated;
}

.qr-skeleton {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  color: #9ca3af;
  font-size: 0.85rem;
}

.spinner-icon {
  width: 2rem;
  height: 2rem;
  color: var(--color-brand);
  animation: spin 1s linear infinite;
}

.spinner-icon-small {
  width: 1rem;
  height: 1rem;
  color: var(--color-brand);
  animation: spin 1s linear infinite;
}

.qr-error {
  color: #dc2626;
  font-size: 0.9rem;
  font-weight: 500;
}

.card-info {
  background: #f3f4f6;
  border-radius: 12px;
  padding: 0.75rem 1rem;
  text-align: left;
  width: 100%;
  margin: 0.5rem 0;
}

.card-info p {
  margin: 0.25rem 0;
  font-size: 0.8rem;
  color: #374151;
}

.situation-banner {
  padding: 1rem;
  border-radius: 12px;
  display: flex;
  gap: 0.75rem;
  margin: 1rem 0;
  text-align: left;
  width: 100%;
}

.situation-info {
  background: #e0f2fe;
  border-left: 4px solid #0284c7;
}

.situation-success {
  background: #d1fae5;
  border-left: 4px solid #059669;
}

.situation-warning {
  background: #fef3c7;
  border-left: 4px solid #d97706;
}

.banner-icon {
  width: 1.5rem;
  height: 1.5rem;
  flex-shrink: 0;
}

.banner-content {
  flex: 1;
}

.banner-content h3 {
  margin: 0 0 0.25rem 0;
  font-size: 0.9rem;
  font-weight: 600;
}

.banner-content p {
  margin: 0;
  font-size: 0.8rem;
}

.stop-info {
  margin-top: 0.25rem !important;
  font-size: 0.75rem !important;
  color: #4b5563;
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
  margin-top: 0.5rem;
}

.btn-checkout:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.checkout-message {
  margin-top: 1rem;
  padding: 0.75rem;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.msg-success {
  background: #d1fae5;
  color: #065f46;
  border: 1px solid #a7f3d0;
}

.msg-warning {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fde68a;
}

.icon-sm {
  width: 1rem;
  height: 1rem;
}

.loading,
.empty {
  color: #6b7280;
  font-size: 0.9rem;
  margin: 0;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem;
  color: #6b7280;
  font-size: 0.875rem;
}

.end-of-list {
  text-align: center;
  padding: 0.75rem;
  color: #9ca3af;
  font-size: 0.75rem;
  font-style: italic;
}

.msg-error {
  color: #dc2626;
  font-size: 0.875rem;
  font-weight: 600;
  margin: 0;
}

.route-planner-section {
  min-height: 460px;
  display: flex;
  flex-direction: column;
}

.bottom-nav {
  margin-top: auto;
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 0.75rem 1rem;
  background: white;
  border-top: 1px solid #e5e7eb;
  position: sticky;
  bottom: 0;
}

.nav-item {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem;
  color: #6b7280;
  transition: color 0.2s;
}

.nav-item.router-link-active {
  color: var(--color-brand, #3b82f6);
}

.nav-icon {
  width: 1.5rem;
  height: 1.5rem;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-container {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 400px;
  overflow: hidden;
  animation: modalSlideIn 0.3s ease;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
}

.modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #6b7280;
}

.modal-body {
  padding: 1.5rem;
  text-align: center;
}

.modal-icon {
  width: 3rem;
  height: 3rem;
  color: var(--color-brand);
  margin-bottom: 1rem;
}

.trip-details {
  background: #f3f4f6;
  padding: 0.75rem;
  border-radius: 8px;
  margin: 1rem 0;
  text-align: left;
}

.trip-details p {
  margin: 0.25rem 0;
  font-size: 0.85rem;
}

.warning-text {
  font-size: 0.8rem;
  color: #6b7280;
  margin-top: 0.75rem;
}

.warning-text.danger {
  color: #dc2626;
  font-weight: 500;
}

.modal-footer {
  display: flex;
  gap: 0.75rem;
  padding: 1rem;
  border-top: 1px solid #e5e7eb;
}

.btn-cancel,
.btn-confirm {
  flex: 1;
  padding: 0.75rem;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel {
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  color: #374151;
}

.btn-confirm {
  background: var(--color-brand);
  border: none;
  color: white;
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.toast {
  position: fixed;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border-radius: 50px;
  z-index: 1100;
  animation: toastSlideUp 0.3s ease;
}

.toast.success {
  background: #10b981;
  color: white;
}

.toast.error {
  background: #ef4444;
  color: white;
}

.toast-icon {
  width: 1.25rem;
  height: 1.25rem;
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes toastSlideUp {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>