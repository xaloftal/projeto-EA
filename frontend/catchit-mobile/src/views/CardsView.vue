<template>
  <div class="container">
    <header class="app-header">
      <router-link to="/home" class="back-btn" aria-label="Back"><ArrowLeft class="icon-md" /></router-link>
      <h1>Travel Cards</h1>
      <div style="width: 2rem"></div>
    </header>

    <div class="cards-list">
      <div v-for="card in (cardViewModel.availableCards as any)" :key="(card as any).id" class="card-option">
        <div class="card-content">
          <h3>{{ ((card as any).name) }}</h3>
          <p>{{ ((card as any).description) }}</p>
          <div class="prices">
            <button @click="purchaseCard((card as any).id, 'monthly')" class="price-btn">
              €{{ (card as any).monthlyPrice }}/mo
            </button>
            <button @click="purchaseCard((card as any).id, 'annual')" class="price-btn">
              €{{ (card as any).annualPrice }}/yr
            </button>
          </div>
        </div>
      </div>
    </div>

    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><House class="nav-icon" /></router-link>
      <router-link to="/search-tickets" class="nav-item"><Search class="nav-icon" /></router-link>
      <router-link to="/cards" class="nav-item active"><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/notifications" class="nav-item"><Bell class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item"><User class="nav-icon" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { ArrowLeft, Bell, House, Search, ShoppingCart, User } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useCardViewModel, useCheckoutViewModel } from '../viewmodels'

const router = useRouter()
const cardViewModel = useCardViewModel()
const checkoutViewModel = useCheckoutViewModel()

onMounted(async () => {
  await cardViewModel.fetchAvailableCards()
})

const purchaseCard = async (cardId: string, type: 'monthly' | 'annual') => {
  const success = await cardViewModel.purchaseCard(cardId, type)
  if (success) {
    // Add to cart
    const card = cardViewModel.availableCards.value.find((c: unknown) => (c as { id: string }).id === cardId)
    if (card) {
      checkoutViewModel.addToCart(card)
      void router.push('/cart')
    }
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
}

.card-content h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.2rem;
}

.card-content p {
  margin: 0 0 1rem 0;
  color: #999;
  font-size: 0.9rem;
}

.prices {
  display: flex;
  gap: 0.5rem;
}

.price-btn {
  flex: 1;
  padding: 0.75rem;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

.bottom-nav {
  display: flex;
  justify-content: space-around;
  background: white;
  border-top: 1px solid #e0e0e0;
}

.nav-item {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.75rem 1.5rem;
  text-decoration: none;
  color: #999;
}

.icon-md {
  width: 1.25rem;
  height: 1.25rem;
}

.nav-item.active {
  color: #667eea;
}
</style>
