<template>
  <div class="notifications-container app-screen">
    <!-- Header -->
    <header class="app-header">
      <router-link to="/home" class="back-btn" aria-label="Back"><ArrowLeft class="icon-md" /></router-link>
      <h1>Notifications</h1>
      <div style="width: 2rem"></div>
    </header>

    <div class="notifications-content">
      <div class="empty-state">
        <p><Bell class="empty-icon" /></p>
        <p>No notifications yet</p>
      </div>
    </div>

    <!-- Bottom Navigation -->
    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item active"><House class="nav-icon" /></router-link>
      <router-link to="/map" class="nav-item"><Map class="nav-icon" /></router-link>
      <router-link to="/cart" class="nav-item"><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/cards" class="nav-item"><Ticket class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item"><User class="nav-icon" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft, Ticket, House, Map, ShoppingCart, User } from 'lucide-vue-next'
</script>

<style scoped>

.notifications-content {
  flex: 1;
  overflow-y: auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-state {
  text-align: center;
  color: #999;
}

.empty-state p:first-child {
  margin-bottom: 1rem;
}

.empty-state p:last-child {
  margin: 0;
}

.empty-icon {
  width: 3rem;
  height: 3rem;
}
</style>
