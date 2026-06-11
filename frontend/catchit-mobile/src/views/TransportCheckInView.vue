<template>
  <div class="container app-screen">
    <header class="app-header">
      <router-link to="/home" class="back-btn" aria-label="Back"><ArrowLeft class="icon-md" /></router-link>
      <h1>Check In</h1>
      <div style="width: 1rem"></div>
    </header>

    <div class="content">
      <div v-if="checkInSuccess" class="success-state">
        <div class="success-icon"><CheckCircle class="success-icon-svg" /></div>
        <h2>Check In successful!</h2>
        <p>You are now on a trip. Present the QR Code below if requested.</p>
        
        <div class="qr-container">
          <div v-if="isLoadingQr" class="qr-skeleton">
            <LoaderCircle class="spinner-icon" />
            <p>Loading validation code...</p>
          </div>
          <img 
            v-else-if="qrCodeUrl" 
            :src="qrCodeUrl" 
            alt="Validation QR Code" 
            class="qr-image" 
          />
          <div v-else class="qr-error">
            <p>QR Code unavailable for validation</p>
          </div>
        </div>

        <button class="btn-checkout" @click="handleCheckOut" :disabled="isCheckingOut">
          {{ isCheckingOut ? 'Processing...' : 'Check Out' }}
        </button>
        <p v-if="checkOutMessage" :class="checkOutMessage.success ? 'msg-success' : 'msg-error'">
          {{ checkOutMessage.text }}
        </p>
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
            <label
              v-for="trip in displayedTrips"
              :key="trip.id"
              class="option-item"
              :class="{ selected: selectedTripId === trip.id }"
            >
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
            <div v-if="!hasMoreTrips && displayedTrips.length > 0" class="end-of-list">
            </div>
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
import { onMounted, watch, ref, computed, onUnmounted } from 'vue'
import { ArrowLeft, House, Map, ShoppingCart, Ticket, User, LoaderCircle, CheckCircle, CreditCard } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import { useTransportViewModel } from '../viewmodels'
import { catchitApi } from '../services/api/catchitApi' 

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
  loadActiveTrips,
  handleCheckIn,
  handleCheckOut
} = transport

const qrCodeUrl = ref<string>('')
const isLoadingQr = ref<boolean>(false)

const ITEMS_PER_PAGE = 15
const currentPage = ref(1)
const isLoadingMore = ref(false)
const scrollContainer = ref<HTMLElement | null>(null)

const displayedTrips = computed(() => {
  return activeTrips.value.slice(0, currentPage.value * ITEMS_PER_PAGE)
})

const hasMoreTrips = computed(() => {
  return displayedTrips.value.length < activeTrips.value.length
})

const loadMoreTrips = async () => {
  if (isLoadingMore.value || !hasMoreTrips.value) return
  
  isLoadingMore.value = true
  
  await new Promise(resolve => setTimeout(resolve, 300))
  
  currentPage.value++
  isLoadingMore.value = false
}

const handleScroll = (event: Event) => {
  const target = event.target as HTMLElement
  const scrollTop = target.scrollTop
  const scrollHeight = target.scrollHeight
  const clientHeight = target.clientHeight
  
  if (scrollTop + clientHeight >= scrollHeight * 0.8) {
    loadMoreTrips()
  }
}

watch(activeTrips, () => {
  currentPage.value = 1
})

// Função responsável por buscar o endpoint de imagem binária do Java
const fetchQrCode = async () => {
  if (!titleId) return
  isLoadingQr.value = true
  try {
    qrCodeUrl.value = await catchitApi.getTicketQrCode(titleId)
  } catch (err) {
    console.error('Error fetching QR Code:', err)
  } finally {
    isLoadingQr.value = false
  }
}

onMounted(() => {
  void loadTitleInfo()
  void loadActiveTrips()
  
  if (checkInSuccess.value) {
    void fetchQrCode()
  }
})

// Monitoriza o sucesso do check-in
watch(checkInSuccess, (isSuccess) => {
  if (isSuccess) {
    void fetchQrCode()
  }
})

watch(() => checkOutMessage.value, (msg) => {
  if (msg && msg.success) {
    router.push('/home')
  }
})

onUnmounted(() => {
  // Cleanup se necessário
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

.loading, .empty {
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

.msg-success {
  color: #16a34a;
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
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>