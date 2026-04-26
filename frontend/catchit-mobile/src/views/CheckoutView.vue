<template>
  <div class="checkout-container app-screen">
    <header class="app-header">
      <router-link to="/cart" class="back-btn" aria-label="Back"><ArrowLeft class="icon-md" /></router-link>
      <h1>Checkout</h1>
      <div style="width: 1rem"></div>
    </header>

    <div v-if="cartItems.length === 0" class="checkout-empty">
      <ShoppingCart class="empty-icon" />
      <h2>No items to pay for</h2>
      <p>Add items in the cart first.</p>
      <router-link to="/cards" class="btn-secondary">Go to store</router-link>
    </div>

    <div v-else class="checkout-content">
      <section class="section">
        <h2>PAYMENT METHOD</h2>
        <p>Select how you want to pay</p>

        <div class="payment-methods">
          <label v-for="method in paymentMethods" :key="method.id" class="payment-option">
            <input v-model="selectedPaymentMethodId" type="radio" :value="method.id" />
            <span class="payment-label">
              <CreditCard class="icon-sm" />
              <span>{{ labelPaymentMethod(method) }}</span>
            </span>
          </label>
        </div>
      </section>

      <section class="section">
        <h2>ORDER SUMMARY</h2>
        <div v-for="item in cartItems" :key="item.id" class="summary-item">
          <div>
            <p class="summary-title">{{ item.title }}</p>
            <p class="summary-meta">{{ item.description }} · Qty {{ item.quantity }}</p>
          </div>
          <strong>€{{ item.totalPrice.toFixed(2) }}</strong>
        </div>
      </section>

      <section class="pricing-summary">
        <div class="price-row">
          <span>Subtotal</span>
          <span>€{{ subtotal.toFixed(2) }}</span>
        </div>
        <div class="price-row">
          <span>Taxes</span>
          <span>€{{ taxes.toFixed(2) }}</span>
        </div>
        <div class="price-row total">
          <span>Total</span>
          <span>€{{ total.toFixed(2) }}</span>
        </div>
      </section>
    </div>

    <div v-if="cartItems.length > 0" class="checkout-footer">
      <button class="btn-confirm" :disabled="isProcessing || !selectedPaymentMethodId" @click="confirmCheckout">
        {{ isProcessing ? 'Processing...' : 'Pay now' }}
      </button>
    </div>

    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><House class="nav-icon" /></router-link>
      <router-link to="/map" class="nav-item"><Map class="nav-icon" /></router-link>
      <router-link to="/cart" class="nav-item active"><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/notifications" class="nav-item"><Bell class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item"><User class="nav-icon" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft, Bell, CreditCard, House, Map, ShoppingCart, User } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useCheckoutViewModel } from '../viewmodels'

const router = useRouter()
const checkoutViewModel = useCheckoutViewModel()
const { cartItems, subtotal, taxes, total, paymentMethods, fetchCart } = checkoutViewModel

const selectedPaymentMethodId = ref('')
const isProcessing = ref(false)

const selectedPaymentMethod = computed(
  () => paymentMethods.value.find((method) => method.id === selectedPaymentMethodId.value) ?? null
)

const labelPaymentMethod = (method: { type: string; cardLast4?: string }) => {
  if (method.type === 'credit_card' && method.cardLast4) {
    return `Card ending ${method.cardLast4}`
  }
  if (method.type === 'debit_card' && method.cardLast4) {
    return `Debit card ending ${method.cardLast4}`
  }
  return 'Digital wallet'
}

onMounted(async () => {
  await fetchCart()
  await checkoutViewModel.fetchPaymentMethods()
  selectedPaymentMethodId.value = paymentMethods.value.find((method) => method.isDefault)?.id ?? paymentMethods.value[0]?.id ?? ''
})

const confirmCheckout = async () => {
  if (!selectedPaymentMethodId.value) return

  isProcessing.value = true
  try {
    const result = await checkoutViewModel.confirmCheckout(selectedPaymentMethodId.value)
    if (result) {
      void router.push({
        name: 'checkout-success',
        params: { orderId: result.orderId },
        query: {
          total: total.value.toFixed(2),
          items: String(cartItems.value.length),
          payment: selectedPaymentMethod.value ? labelPaymentMethod(selectedPaymentMethod.value) : 'Payment method',
        },
      })
    }
  } finally {
    isProcessing.value = false
  }
}
</script>

<style scoped>
.checkout-container {
  background: var(--color-screen-bg);
}

.checkout-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  text-align: center;
  padding: 1rem;
}

.empty-icon {
  width: 3rem;
  height: 3rem;
  color: var(--color-brand);
}

.btn-secondary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-text-strong);
  color: var(--color-on-brand);
  padding: 0.75rem 1.25rem;
  border-radius: var(--radius-md);
  text-decoration: none;
  font-weight: 600;
}

.checkout-content {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: grid;
  gap: 1rem;
}

.section,
.pricing-summary {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 1rem;
}

.section h2 {
  font-size: 0.85rem;
  margin: 0 0 0.5rem 0;
  color: #666;
  letter-spacing: 1px;
}

.section > p {
  margin: 0 0 1rem 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.payment-methods {
  display: grid;
  gap: 0.5rem;
}

.payment-option {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
}

.payment-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid #f0f0f0;
}

.summary-item:last-child {
  border-bottom: none;
}

.summary-title {
  margin: 0;
  font-weight: 700;
  color: var(--color-text-strong);
}

.summary-meta {
  margin: 0.25rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.pricing-summary {
  display: grid;
  gap: 0.25rem;
}

.price-row {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  font-size: 0.95rem;
  border-bottom: 1px solid #f0f0f0;
}

.price-row.total {
  border: none;
  border-top: 2px solid #e0e0e0;
  margin-top: 0.5rem;
  padding-top: 0.75rem;
  font-weight: 700;
  font-size: 1.05rem;
}

.checkout-footer {
  padding: 1rem;
  border-top: 1px solid var(--color-border-strong);
  background: var(--color-surface);
}

.btn-confirm {
  width: 100%;
  padding: 0.85rem;
  background: var(--color-text-strong);
  color: var(--color-on-brand);
  border: none;
  border-radius: 10px;
  font-weight: 700;
  cursor: pointer;
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.icon-sm {
  width: 1.25rem;
  height: 1.25rem;
}
</style>
