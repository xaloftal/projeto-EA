<template>
  <div class="checkout-container">
    <!-- Header -->
    <header class="app-header">
      <router-link to="/map" class="back-btn" aria-label="Back"><ArrowLeft class="icon-md" /></router-link>
      <h1>Checkout</h1>
      <div style="width: 2rem"></div>
    </header>

    <div class="checkout-content">
      <!-- Payment Section -->
      <div class="section">
        <h2>PAYMENT</h2>
        <p>Choose your payment method</p>

        <div class="payment-methods">
          <label class="payment-option">
            <input v-model="selectedPayment" value="mbway" type="radio" />
            <span class="payment-label">
              <span class="payment-icon"><CreditCard class="icon-sm" /></span>
              <span>MB Way</span>
            </span>
          </label>

          <label class="payment-option">
            <input v-model="selectedPayment" value="card" type="radio" checked />
            <span class="payment-label">
              <span class="payment-icon"><CreditCard class="icon-sm" /></span>
              <span>Credit Card</span>
            </span>
          </label>
        </div>

        <!-- Card Form (shown when Credit Card selected) -->
        <div v-if="selectedPayment === 'card'" class="card-form">
          <div class="form-group">
            <label>Card Number</label>
            <input v-model="cardForm.number" type="text" placeholder="1234 1234 1234 1234" class="form-input" />
          </div>

          <div class="form-row">
            <div class="form-group">
              <label>Expiration Date</label>
              <input v-model="cardForm.expiration" type="text" placeholder="MM/YY" class="form-input" />
            </div>
            <div class="form-group">
              <label>CVV</label>
              <input v-model="cardForm.cvv" type="text" placeholder="123" class="form-input" />
            </div>
          </div>

          <div class="form-group">
            <label>Card Owner</label>
            <input v-model="cardForm.owner" type="text" placeholder="Owner Name" class="form-input" />
          </div>
        </div>
      </div>

      <!-- Reservation Details -->
      <div class="section">
        <h2>RESERVATION DETAILS</h2>
        <a href="#" class="see-details">See final reservation details ></a>

        <div class="reservation-item">
          <span class="day">Thursday, 11/02/2026</span>
          <span class="provider">TUB</span>
        </div>

        <div class="route-info">
          <div class="stop">
            <span class="stop-icon"><MapPin class="icon-sm" /></span>
            <span>Vila Nova de Famalicão (Estação de Autocarros)</span>
          </div>
          <span class="time">06:35</span>
        </div>

        <div class="route-info">
          <div class="stop">
            <span class="stop-icon"><MapPin class="icon-sm" /></span>
            <span>Lisboa (Oriente)</span>
          </div>
          <span class="time">10:30</span>
        </div>

        <div class="quantity">
          <span>Quantity: 01</span>
        </div>
      </div>

      <!-- Pricing Summary -->
      <div class="pricing-summary">
        <div class="price-row">
          <span>Subtotal</span>
          <span>€{{ (checkoutViewModel.subtotal as any).toFixed(2) }}</span>
        </div>
        <div class="price-row">
          <span>Taxes</span>
          <span>€{{ (checkoutViewModel.taxes as any).toFixed(2) }}</span>
        </div>
        <div class="price-row total">
          <span>Total</span>
          <span>€{{ (checkoutViewModel.total as any).toFixed(2) }}</span>
        </div>
      </div>
    </div>

    <!-- Confirm Button -->
    <div class="checkout-footer">
      <button @click="confirmCheckout" class="btn-confirm">
        {{ isProcessing ? 'Processing...' : 'Checkout' }}
      </button>
    </div>

    <!-- Bottom Navigation -->
    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><House class="nav-icon" /></router-link>
      <router-link to="/map" class="nav-item"><Map class="nav-icon" /></router-link>
      <router-link to="/cards" class="nav-item active"><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/notifications" class="nav-item"><Bell class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item"><User class="nav-icon" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ArrowLeft, Bell, CreditCard, House, MapPin, Map, ShoppingCart, User } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useCheckoutViewModel } from '../viewmodels'

const router = useRouter()
const checkoutViewModel = useCheckoutViewModel()

const selectedPayment = ref('card')
const isProcessing = ref(false)

const cardForm = ref({
  number: '',
  expiration: '',
  cvv: '',
  owner: '',
})

const confirmCheckout = async () => {
  isProcessing.value = true
  try {
    // Create checkout session and then confirm
    const sessionId = await checkoutViewModel.createCheckoutSession()
    if (sessionId) {
      // Use first payment method or create one
      const paymentMethodId = 'payment_1'
      const result = await checkoutViewModel.confirmCheckout(sessionId, paymentMethodId)
      if (result) {
        void router.push({
          name: 'checkout-success',
          params: { orderId: result.orderId },
        })
      }
    }
  } finally {
    isProcessing.value = false
  }
}
</script>

<style scoped>
.checkout-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}

.app-header {
  background: #667eea;
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

.checkout-content {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
}

.section {
  background: white;
  border-radius: 12px;
  padding: 1rem;
  margin-bottom: 1rem;
}

.section h2 {
  font-size: 0.85rem;
  margin: 0 0 0.5rem 0;
  color: #666;
  letter-spacing: 1px;
}

.section > p {
  margin: 0 0 1rem 0;
  color: #999;
  font-size: 0.9rem;
}

.payment-methods {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.payment-option {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  cursor: pointer;
  transition: border-color 0.3s;
}

.payment-option:hover {
  border-color: #667eea;
}

.payment-option input {
  cursor: pointer;
}

.payment-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.payment-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.card-form {
  margin-top: 1rem;
  padding: 1rem;
  background: #f9f9f9;
  border-radius: 6px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.form-group label {
  font-weight: 600;
  font-size: 0.85rem;
  color: #333;
}

.form-input {
  padding: 0.75rem;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 0.95rem;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.see-details {
  display: block;
  margin-bottom: 1rem;
  color: #667eea;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 600;
}

.reservation-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  background: #f9f9f9;
  border-radius: 6px;
  margin-bottom: 1rem;
  font-size: 0.9rem;
}

.day {
  font-weight: 600;
}

.provider {
  background: #e8e8e8;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
}

.route-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  margin-bottom: 0.5rem;
  font-size: 0.85rem;
}

.stop {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.stop-icon {
  font-size: 1rem;
}

.time {
  color: #999;
}

.quantity {
  padding: 0.75rem;
  background: #f9f9f9;
  border-radius: 6px;
  font-size: 0.9rem;
}

.pricing-summary {
  background: white;
  border-radius: 12px;
  padding: 1rem;
  margin-bottom: 1rem;
}

.price-row {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  font-size: 0.9rem;
  border-bottom: 1px solid #f0f0f0;
}

.price-row.total {
  border: none;
  border-top: 2px solid #e0e0e0;
  margin-top: 0.5rem;
  padding-top: 0.75rem;
  font-weight: 600;
  font-size: 1.1rem;
}

.checkout-footer {
  padding: 1rem;
  border-top: 1px solid #e0e0e0;
  background: white;
}

.btn-confirm {
  width: 100%;
  padding: 0.75rem;
  background: #000;
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.3s;
}

.btn-confirm:hover {
  background: #333;
}

.bottom-nav {
  display: flex;
  justify-content: space-around;
  background: white;
  border-top: 1px solid #e0e0e0;
  padding: 0.5rem 0;
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
