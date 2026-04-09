<template>
  <div class="home-container">
    <!-- Header -->
    <header class="app-header">
      <h1>CatchIt</h1>
      <router-link to="/profile" class="profile-icon" aria-label="Profile">
        <User class="icon-md" />
      </router-link>
    </header>

    <!-- Tabs -->
    <div class="tabs">
      <button
        :class="['tab', { active: activeTab === 'cards' }]"
        @click="activeTab = 'cards'"
      >
        My Cards
      </button>
      <button
        :class="['tab', { active: activeTab === 'tickets' }]"
        @click="activeTab = 'tickets'"
      >
        My Tickets
      </button>
    </div>

    <!-- My Cards Tab -->
    <div v-if="activeTab === 'cards'" class="tab-content">
      <div v-if="(cardViewModel.userCards as any).value?.length === 0" class="empty-state">
        <p><CreditCard class="empty-icon" /> No cards yet</p>
        <router-link to="/cards" class="btn-primary">Browse Cards</router-link>
      </div>

      <div v-else class="cards-grid">
        <div v-for="card in (cardViewModel.userCards as any)" :key="(card as any).id" class="card-item">
          <div class="card-content">
            <h3>{{ (card as any).name }}</h3>
            <p class="card-price">€{{ ((card as any).price)?.toFixed(2) || '0.00' }}/mo</p>
          </div>
          <button class="btn-renew">Renew</button>
        </div>
      </div>

      <div class="card-display">
        <div class="card-visual">
          <div class="card-logo">TUB</div>
          <div class="card-details">
            <p>TUB Card</p>
            <p class="date">Expires on 12/02/2026</p>
          </div>
        </div>
        <button class="btn-qr"><QrCode class="icon-sm" /> View QR Code</button>
      </div>
    </div>

    <!-- My Tickets Tab -->
    <div v-if="activeTab === 'tickets'" class="tab-content">
      <div v-if="(ticketViewModel.tickets as any).value?.length === 0" class="empty-state">
        <p><Ticket class="empty-icon" /> No tickets yet</p>
        <router-link to="/search-tickets" class="btn-primary">Buy Tickets</router-link>
      </div>

      <div v-else>
        <div v-for="ticket in (ticketViewModel.tickets as any)" :key="(ticket as any).id" class="ticket-item">
          <div class="ticket-header">
            <h3>TUB Ticket</h3>
            <span class="status-badge" :class="((ticket as any).status).toLowerCase()">
              {{ formatStatus((ticket as any).status) }}
            </span>
          </div>
          <p class="expiry">Expires on {{ formatDate((ticket as any).validUntil) }}</p>
          <div class="ticket-stops">
            <p><MapPin class="icon-sm" /> {{ ((ticket as any).trip?.stops?.[0]?.name) || 'Stop 1' }}</p>
            <p><MapPin class="icon-sm" /> {{ ((ticket as any).trip?.stops?.at(-1)?.name) || 'Stop 2' }}</p>
          </div>
          <div class="qr-code"><QrCode class="icon-md" /></div>
        </div>
      </div>
    </div>

    <!-- Bottom Navigation -->
    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item active">
        <House class="nav-icon" />
      </router-link>
      <router-link to="/search-tickets" class="nav-item">
        <Search class="nav-icon" />
      </router-link>
      <router-link to="/cart" class="nav-item">
        <ShoppingCart class="nav-icon" />
      </router-link>
      <router-link to="/notifications" class="nav-item">
        <Bell class="nav-icon" />
      </router-link>
      <router-link to="/profile" class="nav-item">
        <User class="nav-icon" />
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Bell, CreditCard, House, MapPin, QrCode, Search, ShoppingCart, Ticket, User } from 'lucide-vue-next'
import { useTicketViewModel, useCardViewModel } from '../viewmodels'

const activeTab = ref<'cards' | 'tickets'>('cards')
const ticketViewModel = useTicketViewModel()
const cardViewModel = useCardViewModel()

onMounted(async () => {
  await ticketViewModel.fetchUserTickets()
  await cardViewModel.fetchUserCards()
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
</script>

<style scoped>
.home-container {
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
  font-size: 1.5rem;
  margin: 0;
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

.tab-content {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
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

.btn-primary {
  display: inline-block;
  background: #667eea;
  color: white;
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  text-decoration: none;
  border: none;
  cursor: pointer;
  font-weight: 600;
  transition: background 0.3s;
}

.btn-primary:hover {
  background: #5568d3;
}

.cards-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
  margin-bottom: 2rem;
}

.card-item {
  background: white;
  border-radius: 12px;
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.icon-md {
  width: 1.25rem;
  height: 1.25rem;
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

.card-content h3 {
  margin: 0 0 0.25rem 0;
  font-size: 1.1rem;
}

.card-price {
  color: #667eea;
  margin: 0;
  font-weight: 600;
}

.btn-renew {
  background: #667eea;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

.card-display {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  margin-top: 1rem;
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

.btn-qr {
  width: 100%;
  padding: 0.75rem;
  background: #e8e8e8;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
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

.nav-item.active {
  color: #667eea;
}

.nav-item:hover {
  color: #667eea;
}
</style>
