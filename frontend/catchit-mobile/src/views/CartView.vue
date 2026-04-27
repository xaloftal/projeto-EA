<template>
  <div class="cart-container app-screen">
    <header class="app-header">
      <router-link to="/home" class="back-btn" aria-label="Back"><ArrowLeft class="icon-md" /></router-link>
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
              <button class="remove-btn" @click="removeFromCart(item.id)">Remove</button>
            </div>
          </article>
        </div>

        <div class="cart-summary">
          <div class="summary-row">
            <span>Total</span>
            <strong>€{{ total.toFixed(2) }}</strong>
          </div>
          <button class="checkout-btn" :disabled="!cartItems.length" @click="goToCheckout">Checkout</button>
        </div>
      </template>
    </div>

    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><House class="nav-icon" /></router-link>
      <router-link to="/map" class="nav-item"><Map class="nav-icon" /></router-link>
      <router-link to="/cart" class="nav-item active"><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/cards" class="nav-item"><Ticket class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item"><User class="nav-icon" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { ArrowLeft, Ticket, House, Map, ShoppingCart, User } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useCheckoutViewModel } from '../viewmodels'

const router = useRouter()
const checkoutViewModel = useCheckoutViewModel()
const { cartItems, total, removeFromCart, fetchCart } = checkoutViewModel

onMounted(() => {
  void fetchCart()
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
  text-align: center;
  color: var(--color-text-muted);
  gap: 0.75rem;
}

.empty-state h2 {
  margin: 0;
  color: var(--color-text-strong);
}

.empty-state p {
  margin: 0;
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
  padding-top: 1rem;
  display: grid;
  gap: 0.75rem;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  font-size: 1rem;
  box-shadow: var(--shadow-card);
}

.checkout-btn {
  padding: 0.9rem 1rem;
  border: none;
  border-radius: 10px;
  background: var(--color-text-strong);
  color: var(--color-on-brand);
  font-weight: 700;
  cursor: pointer;
}

.checkout-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.bottom-nav {
  margin-top: auto;
}
</style>
