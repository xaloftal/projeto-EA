<template>
  <div class="search-container">
    <!-- Header -->
    <header class="app-header">
      <router-link to="/home" class="back-btn" aria-label="Back">
        <ArrowLeft class="icon-md" />
      </router-link>
      <h1>Buy Tickets</h1>
      <div style="width: 2rem"></div>
    </header>

    <!-- Search Form -->
    <div class="search-form">
      <div class="tabs">
        <button :class="['tab', { active: activeTab === 'tickets' }]" @click="activeTab = 'tickets'">
          Buy Tickets
        </button>
        <button :class="['tab', { active: activeTab === 'cards' }]" @click="activeTab = 'cards'">
          Buy Cards
        </button>
      </div>

      <!-- Ticket Search Tab -->
      <div v-if="activeTab === 'tickets'" class="tab-content">
        <div class="form-group">
          <label>From</label>
          <input v-model="searchForm.from" type="text" placeholder="Vila Nova de Famalicão" class="form-input" />
        </div>

        <div class="form-group">
          <label>To</label>
          <input v-model="searchForm.to" type="text" placeholder="Lisboa" class="form-input" />
        </div>

        <div class="form-group">
          <label>Departure</label>
          <input v-model="searchForm.departure" type="date" class="form-input" />
        </div>

        <div class="form-group">
          <label>Arrival</label>
          <input v-model="searchForm.arrival" type="date" class="form-input" />
        </div>

        <button @click="searchTickets" class="btn-primary">
          {{ isLoading ? 'Searching...' : 'Search' }}
        </button>

        <!-- Search Results -->
        <div v-if="(searchResults as any).length > 0" class="results">
          <h3>Results - 99 results</h3>
          <div class="filter-buttons">
            <button class="filter-btn active">Filter</button>
            <button class="filter-btn">Sort</button>
          </div>

          <div v-for="result in (searchResults as any)" :key="(result as any).routeId" class="result-item">
            <div class="result-header">
              <div class="route">
                <p class="location">{{ ((result as any).fromStop.name) }}</p>
                <p class="time">{{ ((result as any).departureTime) }}</p>
              </div>
              <div class="arrow"><ArrowRight class="icon-sm" /></div>
              <div class="route">
                <p class="location">{{ ((result as any).toStop.name) }}</p>
                <p class="time">{{ ((result as any).arrivalTime) }}</p>
              </div>
            </div>
            <div class="result-details">
              <span class="provider">TUB</span>
              <span class="price">€{{ ((result as any).price.toFixed(2)) }}</span>
            </div>
            <button @click="selectRoute(result)" class="btn-select">Select</button>
          </div>
        </div>
      </div>

      <!-- Card Search Tab -->
      <div v-if="activeTab === 'cards'" class="tab-content">
        <div class="cards-list">
          <div v-for="card in (cardViewModel.availableCards as any)" :key="(card as any).id" class="card-option">
            <div class="card-info">
              <h3>{{ ((card as any).name) }}</h3>
              <p class="description">{{ ((card as any).description) }}</p>
            </div>
            <div class="card-prices">
              <button class="price-btn">
                <span class="label">Monthly</span>
                <span class="price">€{{ ((card as any).monthlyPrice) }}</span>
              </button>
              <button class="price-btn">
                <span class="label">Annual</span>
                <span class="price">€{{ ((card as any).annualPrice) }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bottom Navigation -->
    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><House class="nav-icon" /></router-link>
      <router-link to="/search-tickets" class="nav-item active"><Search class="nav-icon" /></router-link>
      <router-link to="/cart" class="nav-item"><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/notifications" class="nav-item"><Bell class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item"><User class="nav-icon" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ArrowLeft, ArrowRight, Bell, House, Search, ShoppingCart, User } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useTravelViewModel, useCardViewModel } from '../viewmodels'

const router = useRouter()
const travelViewModel = useTravelViewModel()
const cardViewModel = useCardViewModel()

const activeTab = ref<'tickets' | 'cards'>('tickets')
const searchForm = ref({
  from: '',
  to: '',
  departure: '',
  arrival: '',
})
const searchResults = ref<unknown[]>([])
const isLoading = ref(false)

onMounted(async () => {
  await cardViewModel.fetchAvailableCards()
})

const searchTickets = async () => {
  isLoading.value = true
  try {
    const results = await travelViewModel.searchRoutes(
      'stop_1',
      'stop_3',
      searchForm.value.departure
    )
    searchResults.value = results
  } finally {
    isLoading.value = false
  }
}

const selectRoute = (route: unknown) => {
  // Store selected route and navigate to confirmation
  sessionStorage.setItem('selectedRoute', JSON.stringify(route))
  void router.push('/ticket-confirmation')
}
</script>

<style scoped>
.search-container {
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
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
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

.search-form {
  flex: 1;
  overflow-y: auto;
}

.tabs {
  display: flex;
  background: white;
  border-bottom: 2px solid #e0e0e0;
}

.tab {
  flex: 1;
  padding: 1rem;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  cursor: pointer;
  font-weight: 600;
  color: #999;
  transition: all 0.3s;
}

.tab.active {
  border-bottom-color: #667eea;
  color: #667eea;
}

.tab-content {
  padding: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.form-group label {
  font-weight: 600;
  color: #333;
  font-size: 0.9rem;
}

.form-input {
  padding: 0.75rem;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 1rem;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.btn-primary {
  width: 100%;
  padding: 0.75rem;
  background: #000;
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.3s;
}

.btn-primary:hover {
  background: #333;
}

.results {
  margin-top: 2rem;
}

.results h3 {
  margin: 0 0 1rem 0;
  color: #333;
}

.filter-buttons {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.filter-btn {
  padding: 0.5rem 1rem;
  background: #e8e8e8;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 600;
}

.filter-btn.active {
  background: #000;
  color: white;
}

.result-item {
  background: white;
  border-radius: 12px;
  padding: 1rem;
  margin-bottom: 1rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.route {
  flex: 1;
}

.location {
  margin: 0;
  font-weight: 600;
  font-size: 0.9rem;
}

.time {
  margin: 0.25rem 0 0 0;
  color: #999;
  font-size: 0.85rem;
}

.arrow {
  margin: 0 0.5rem;
  color: #999;
}

.result-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0.75rem 0;
  font-size: 0.9rem;
}

.provider {
  background: #f0f0f0;
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
}

.price {
  font-weight: 600;
  font-size: 1.1rem;
}

.btn-select {
  width: 100%;
  padding: 0.5rem;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

.cards-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.card-option {
  background: white;
  border-radius: 12px;
  padding: 1rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.card-info h3 {
  margin: 0 0 0.25rem 0;
}

.description {
  color: #999;
  font-size: 0.9rem;
  margin: 0;
}

.card-prices {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
}

.price-btn {
  flex: 1;
  padding: 0.75rem;
  background: #f0f0f0;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
}

.price-btn .label {
  font-size: 0.75rem;
  color: #999;
}

.price-btn .price {
  font-weight: 600;
  font-size: 1.1rem;
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
  align-items: center;
  justify-content: center;
  padding: 0.75rem 1.5rem;
  text-decoration: none;
  color: #999;
  transition: color 0.3s;
}

.icon-md {
  width: 1.25rem;
  height: 1.25rem;
}

.icon-sm {
  width: 1rem;
  height: 1rem;
}

.nav-item.active {
  color: #667eea;
}

.nav-item:hover {
  color: #667eea;
}
</style>
