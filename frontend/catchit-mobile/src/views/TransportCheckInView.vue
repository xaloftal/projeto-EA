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
                <p class="option-title"> {{ trip.routeName }}</p>
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
import { onMounted, watch } from 'vue'
import { ArrowLeft, House, Map, ShoppingCart, Ticket, User } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import { useTransportViewModel } from '../viewmodels'

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
  loadTitleInfo,
  loadActiveTrips,
  handleCheckIn,
  handleCheckOut
} = transport

onMounted(() => {
  void loadTitleInfo()
  void loadActiveTrips()
})
watch(() => checkOutMessage.value, (msg) => {
  if (msg && msg.success) {
    router.push('/home')
  }
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

.route-planner-section {
  min-height: 460px;
  display: flex;
  flex-direction: column;
}

.bottom-nav {
  margin-top: auto;
}
</style>