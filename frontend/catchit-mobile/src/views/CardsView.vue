<template>
  <div class="container">
    <header class="app-header">
      <router-link to="/home" class="back-btn" aria-label="Back"><ArrowLeft class="icon-md" /></router-link>
      <h1>Shopping Cart</h1>
      <div style="width: 2rem"></div>
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
          <h2>Buy by Route</h2>
        </div>
        <p>Select departure and destination stops on the map to buy your next ticket.</p>
        <router-link to="/search-tickets" class="ticket-btn">Go to Ticket Search</router-link>
      </article>

      <article class="ticket-option ticket-option-muted">
        <div class="ticket-head">
          <MapPin class="ticket-icon" />
          <h2>Live Stop Search</h2>
        </div>
        <p>Use stop search and route timing to pick the best option before purchasing.</p>
        <router-link to="/search-tickets" class="ticket-btn">Open Map</router-link>
      </article>
    </div>

    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><House class="nav-icon" /></router-link>
      <router-link to="/search-tickets" class="nav-item"><Map class="nav-icon" /></router-link>
      <router-link to="/cards" class="nav-item active"><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/notifications" class="nav-item"><Bell class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item"><User class="nav-icon" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft, Bell, House, Map, MapPin, ShoppingCart, Ticket, User } from 'lucide-vue-next'
import { useCardViewModel } from '../viewmodels'
import type { CardTier } from '../models'

const cardViewModel = useCardViewModel()
const activeTab = ref<'cards' | 'tickets'>('cards')

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

onMounted(async () => {
  await cardViewModel.fetchUserCards()
  await cardViewModel.fetchAvailableCards()
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
