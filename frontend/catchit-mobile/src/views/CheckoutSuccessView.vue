<template>
  <div class="success-container">
    <!-- Header -->
    <header class="app-header">
      <h1>Checkout</h1>
    </header>

    <div class="success-content">
      <!-- Payment Confirmation -->
      <div class="section payment-info">
        <h2>PAYMENT</h2>
        <div class="payment-detail">
          <span>Visa *1234</span>
          <span class="chevron">></span>
        </div>
      </div>

      <!-- Reservation Details -->
      <div class="section">
        <h2>RESERVATION DETAILS</h2>

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

      <!-- Pricing -->
      <div class="pricing-summary">
        <div class="price-row">
          <span>Subtotal</span>
          <span>€19.98</span>
        </div>
        <div class="price-row">
          <span>Taxes</span>
          <span>€2.00</span>
        </div>
        <div class="price-row total">
          <span>Total</span>
          <span>€21.98</span>
        </div>
      </div>

      <!-- Confirm Button -->
      <button class="btn-confirm">Checkout</button>
    </div>

    <!-- Bottom Navigation -->
    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><House class="nav-icon" /></router-link>
      <router-link to="/search-tickets" class="nav-item"><Map class="nav-icon" /></router-link>
      <router-link to="/cart" class="nav-item"><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/notifications" class="nav-item"><Bell class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item"><User class="nav-icon" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { Bell, House, MapPin, Map, ShoppingCart, User } from 'lucide-vue-next'
</script>

<style scoped>
.success-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f5f5;
}

.app-header {
  background: #667eea;
  color: white;
  padding: 1rem;
  text-align: center;
}

.app-header h1 {
  font-size: 1.2rem;
  margin: 0;
}

.success-content {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
}

.section {
  background: white;
  border-radius: 12px;
  padding: 1rem;
  margin-bottom: 1rem;
}

.section h2 {
  font-size: 0.85rem;
  margin: 0 0 0.75rem 0;
  color: #666;
  letter-spacing: 1px;
}

.payment-info {
  margin-bottom: 1rem;
}

.payment-detail {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  background: #f9f9f9;
  border-radius: 6px;
}

.chevron {
  color: #999;
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
  margin-top: auto;
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

.btn-confirm {
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

.icon-sm {
  width: 1rem;
  height: 1rem;
}

.nav-item:hover {
  color: #667eea;
}
</style>
