<template>
  <div class="success-container app-screen">
    <header class="app-header">
      <h1>Checkout</h1>
    </header>

    <div class="success-content">
      <div class="success-card">
        <h2 v-if="isValid">Payment successful</h2>
        <h2 v-else-if="validationChecked">Payment not validated</h2>
        <h2 v-else>Validating payment...</h2>
        <p v-if="isValid">Your items were added to your account.</p>
        <p v-else-if="validationChecked">We could not confirm this payment with the backend.</p>
        <p v-else>Please wait while we validate your order.</p>
        <p class="meta">Order {{ orderId }}</p>
        <p class="meta">Status: {{ paymentStatus }}</p>
        <p v-if="validationMessage" class="meta">{{ validationMessage }}</p>
        <p class="meta">Items: {{ itemCount }}</p>
        <p class="meta">Total: €{{ total }}</p>
        <p class="meta">Payment: {{ paymentMethod }}</p>
      </div>

      <router-link to="/home" class="btn-confirm">Back to home</router-link>
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
import { computed, onMounted, ref } from 'vue'
import { House, Map, ShoppingCart, Ticket, User } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import { catchitApi } from '../services/api/catchitApi'

const route = useRoute()

const orderId = computed(() => String(route.params.orderId ?? 'N/A'))
const itemCount = computed(() => String(route.query.items ?? '0'))
const total = computed(() => String(route.query.total ?? '0.00'))
const paymentMethod = computed(() => String(route.query.payment ?? 'Selected payment method'))

const isValid = ref(false)
const validationChecked = ref(false)
const paymentStatus = ref('PENDING')
const validationMessage = ref('')

onMounted(async () => {
  if (!orderId.value || orderId.value === 'N/A') {
    validationChecked.value = true
    paymentStatus.value = 'UNKNOWN'
    validationMessage.value = 'Missing order id'
    return
  }

  const response = await catchitApi.validateCheckoutOrder(orderId.value)
  validationChecked.value = true

  if (response.success && response.data) {
    isValid.value = response.data.valid
    paymentStatus.value = response.data.paymentStatus
    validationMessage.value = response.data.message
    return
  }

  isValid.value = false
  paymentStatus.value = 'UNAVAILABLE'
  validationMessage.value = response.error || 'Unable to validate payment'
})
</script>

<style scoped>
.success-container {
  background: var(--color-screen-bg);
}

.success-content {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1rem;
}

.success-card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 1rem;
  text-align: center;
}

.success-card h2 {
  margin: 0 0 0.75rem 0;
  color: var(--color-text-strong);
}

.meta {
  margin: 0.35rem 0 0;
  color: var(--color-text-muted);
}

.btn-confirm {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.85rem 1rem;
  background: var(--color-text-strong);
  color: var(--color-on-brand);
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  text-decoration: none;
  transition: background 0.3s;
}

.btn-confirm:hover {
  background: #333;
}
</style>
