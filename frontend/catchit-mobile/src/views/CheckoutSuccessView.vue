<template>
  <div class="success-container app-screen">
    <header class="app-header">
      <h1>Checkout</h1>
    </header>

    <div class="success-content">
      <div class="success-card receipt-card">
        <div class="receipt-header">
          <h3 v-if="isValid">Payment successful</h3>
          <h3 v-else-if="validationChecked">Payment not validated</h3>
          <h3 v-else>Validating payment...</h3>
          <p v-if="isValid">Your items were added to your account.</p>
          <p v-else-if="validationChecked">We could not confirm this payment with the backend.</p>
          <p v-else>Please wait while we validate your order.</p>
        </div>

        <div class="receipt-divider"></div>

        <div class="receipt-details">
          <div class="receipt-row">
            <span class="label">Order Number</span>
            <span class="value">{{ orderId }}</span>
          </div>
          <div class="receipt-row">
            <span class="label">Status</span>
            <span class="value">{{ paymentStatus }}</span>
          </div>
          <div class="receipt-row">
            <span class="label">Payment Method</span>
            <span class="value">{{ paymentMethod }}</span>
          </div>
        </div>

        <div v-if="purchasedItems.length > 0">
          <div class="receipt-divider dashed"></div>
          
          <div class="receipt-details purchased-items-list">
            <div class="receipt-row label-row">
              <span class="label item-col">Item</span>
              <span class="label qty-col">Qty</span>
              <span class="label price-col">Price</span>
            </div>
            
            <div v-for="item in purchasedItems" :key="item.id" class="receipt-row item-row">
              <div class="item-info item-col">
                <span class="item-title">{{ item.title }}</span>
              </div>
              <span class="item-qty qty-col">{{ item.quantity }}</span>
              <span class="item-price price-col">€{{ item.totalPrice.toFixed(2) }}</span>
            </div>
          </div>
        </div>

        <div class="receipt-divider dashed"></div>

        <div class="receipt-row tax-row">
          <span>Taxes</span>
          <span>€{{ tax }}</span>
        </div>

        <div class="receipt-total">
          <span>Total Paid</span>
          <span>€{{ total }}</span>
        </div>
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
const total = computed(() => String(route.query.total ?? '0.00'))
const tax = computed(() => String(route.query.tax ?? '0.00'))
const paymentMethod = computed(() => String(route.query.payment ?? 'Selected payment method'))

const isValid = ref(false)
const validationChecked = ref(false)
const paymentStatus = ref('PENDING')
const validationMessage = ref('')
const purchasedItems = ref<any[]>([])

onMounted(async () => {
  if (orderId.value && orderId.value !== 'N/A') {
    const itemsJson = sessionStorage.getItem(`order_${orderId.value}_items`)
    if (itemsJson) {
      try {
        purchasedItems.value = JSON.parse(itemsJson)
      } catch (e) {
        console.error('Failed to parse purchased items: ', e)
      }
    }
  }

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
  padding: 0;
  text-align: center;
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.receipt-card {
  position: relative;
  background: #fff;
  border-top-left-radius: 12px;
  border-top-right-radius: 12px;
  border-bottom-left-radius: 4px;
  border-bottom-right-radius: 4px;
  filter: drop-shadow(0 4px 10px rgba(0, 0, 0, 0.08));
  overflow: scroll;
}

.receipt-card::after {
  content: "";
  position: absolute;
  bottom: -8px;
  left: 0;
  right: 0;
  height: 8px;
  background-image: radial-gradient(circle at 10px 0, transparent 10px, #fff 11px);
  background-size: 20px 10px;
  background-repeat: repeat-x;
}

.receipt-header {
  padding: 2rem 1.5rem 1.5rem;
  background: #f8fafc;
}

.success-card h2 {
  margin: 0 0 0.75rem 0;
  color: var(--color-text-strong);
}

.receipt-header p {
  margin: 0;
  color: var(--color-text-muted);
}

.receipt-divider {
  height: 1px;
  background: #e2e8f0;
  margin: 0 1.5rem;
}

.receipt-divider.dashed {
  background: transparent;
  border-bottom: 2px dashed #e2e8f0;
}

.receipt-details {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.receipt-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.95rem;
}

.receipt-row .label {
  color: #64748b;
  text-transform: uppercase;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.receipt-row .value {
  color: #1e293b;
  font-weight: 600;
}

.purchased-items-list {
  padding-top: 1rem;
  padding-bottom: 1rem;
}

.item-row {
  align-items: flex-start;
  margin-bottom: 0.25rem;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  text-align: left;
}

.item-title {
  font-weight: 700;
  color: #334155;
  font-size: 0.9rem;
}

.item-desc {
  font-size: 0.75rem;
  color: #94a3b8;
}

.item-col {
  flex: 1;
}

.qty-col {
  width: 40px;
  text-align: center;
}

.price-col {
  width: 70px;
  text-align: right;
  font-weight: 600;
}

.label-row {
  margin-bottom: 0.5rem;
}

.item-qty {
  font-weight: 600;
  color: #475569;
}

.tax-row {
  padding: 1rem 1.5rem 0;
  color: #64748b;
  font-weight: 500;
}

.receipt-total {
  padding: 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
}

.meta {
  margin: 0.75rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
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
