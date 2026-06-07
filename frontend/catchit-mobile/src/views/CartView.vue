<template>
  <div class="cart-container app-screen">
    <header class="app-header">
      <router-link to="/home" class="back-btn" aria-label="Back">
        <ArrowLeft class="icon-md" />
      </router-link>
      <h1>Cart</h1>
      <div style="width: 1rem"></div>
    </header>

    <div class="cart-content">
      <div v-if="cartItems.length === 0" class="empty-state">
        <ShoppingCart class="empty-icon" />
        <h2>Your cart is empty</h2>
        <p>Add cards or tickets to continue.</p>
        <router-link to="/cards" class="btn-primary">Browse store</router-link>
      </div>

      <template v-else>
        <div class="cart-list">
          <article v-for="item in cartItems" :key="item.id" class="cart-item">
            <div class="cart-item-main">
              <p class="cart-item-title">{{ item.title }}</p>
              <p class="cart-item-description">{{ item.description }}</p>
              <p class="cart-item-quantity">Qty {{ item.quantity }}</p>
            </div>
            <div class="cart-item-price">
              <span>€{{ item.totalPrice.toFixed(2) }}</span>
              <button class="remove-btn" @click="handleRemoveItem(item.id)">Remove</button>
            </div>
          </article>
        </div>

        <div class="cart-summary">
          <div class="summary-row">
            <span>Subtotal</span>
            <span>€{{ cartSubtotal.toFixed(2) }}</span>
          </div>
          <div class="summary-row">
            <span>Taxes</span>
            <span>€{{ serverSummary?.taxes?.toFixed(2) || '0.00' }}</span>
          </div>
          <div v-if="cartDiscount > 0" class="summary-row discount-row">
            <span>Pack Discount</span>
            <span>-€{{ cartDiscount.toFixed(2) }}</span>
          </div>

          <div class="summary-row total-row">
            <span>Total</span>
            <span>€{{ total.toFixed(2) }}</span>
          </div>

          <button class="btn-primary checkout-btn" @click="goToCheckout">
            Proceed to checkout
          </button>
        </div>
      </template>


    </div>

    <!-- Bottom Navigation -->
    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item">
        <House class="nav-icon" />
      </router-link>
      <router-link to="/map" class="nav-item">
        <Map class="nav-icon" />
      </router-link>
      <router-link to="/cart" class="nav-item active">
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
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft, ShoppingCart, House, Map, Ticket, User } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useCheckoutViewModel } from '../viewmodels'
import { requestJson } from '../services/api/http'

const router = useRouter()
const checkoutViewModel = useCheckoutViewModel()

const { cartItems, removeFromCart } = checkoutViewModel

const serverSummary = ref<{ subtotal: number; taxes: number; discount: number; total: number } | null>(null)
const syncPricesWithBackend = async () => {
  if (cartItems.value.length === 0) {
    serverSummary.value = null
    return
  }

  try {
    const response = await requestJson<{ subtotal: number; taxes: number; total: number }>('/api/checkout/session', {
      method: 'POST'
    })

    if (response.success && response.data) {
      const data = response.data
      const calculatedDiscount = data.subtotal + data.taxes - data.total

      serverSummary.value = {
        subtotal: data.subtotal,
        taxes: data.taxes,
        discount: calculatedDiscount > 0 ? calculatedDiscount : 0,
        total: data.total
      }
    }
  } catch (err) {
    console.error('Erro ao sincronizar preços com o backend:', err)
  }
}

onMounted(async () => {
  await syncPricesWithBackend()
})

const handleRemoveItem = async (itemId: string) => {
  await removeFromCart(itemId)
  await syncPricesWithBackend()
}



const cartSubtotal = computed(() => {
  if (serverSummary.value) return serverSummary.value.subtotal
  return cartItems.value.reduce((acc, item) => acc + item.totalPrice, 0)
})

const cartDiscount = computed(() => {
  if (serverSummary.value) return serverSummary.value.discount

  // Salvaguarda local enquanto a API não responde
  const ticketQuantity = cartItems.value
    .filter(item => item.kind === 'ticket')
    .reduce((acc, item) => acc + item.quantity, 0)

  return ticketQuantity >= 5 ? 4.50 : 0
})

const total = computed(() => {
  if (serverSummary.value) return serverSummary.value.total

  const valorFinal = cartSubtotal.value - cartDiscount.value
  return valorFinal < 0 ? 0 : valorFinal
})

const goToCheckout = () => {
  void router.push('/checkout')
}
</script>

<style scoped>
.cart-container {
  background: var(--color-screen-bg);
}

.cart-content {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  text-align: center;
}

.empty-icon {
  width: 3rem;
  height: 3rem;
  color: var(--color-brand);
}

.cart-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.cart-item {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  box-shadow: var(--shadow-card);
}

.cart-item-main {
  min-width: 0;
}

.cart-item-title {
  margin: 0;
  font-weight: 700;
  color: var(--color-text-strong);
}

.cart-item-description,
.cart-item-quantity {
  margin: 0.35rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.cart-item-price {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  gap: 0.5rem;
  flex-shrink: 0;
  color: var(--color-text-strong);
  font-weight: 700;
}

.remove-btn {
  border: none;
  background: transparent;
  color: #ef4444;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.cart-summary {
  margin-top: auto;
  padding-top: 2rem;
  display: grid;
  gap: 0.75rem;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.95rem;
  color: var(--color-text-muted);
}

.discount-row {
  color: #10b981;
  font-weight: 600;
}

.total-row {
  font-size: 1.2rem;
  font-weight: 700;
  color: var(--color-text-strong);
  border-top: 1px solid var(--color-border);
  padding-top: 0.75rem;
  margin-top: 0.25rem;
}

.checkout-btn {
  width: 100%;
  margin-top: 0.5rem;
  padding: 0.85rem;
  text-align: center;
  border: none;
  border-radius: var(--radius-md);
  font-weight: 700;
  cursor: pointer;
}
</style>