<template>
  <div class="plan-trip-view app-screen">
    <header class="app-header">
      <router-link to="/map" class="back-btn" aria-label="Back">
        <ArrowLeft class="icon-md" />
      </router-link>
      <h1>Plan Trip</h1>
      <div style="width: 1rem"></div>
    </header>

    <div class="plan-trip-content">
      <RoutePlanner layout="stacked" :auto-plan="false" />
    </div>

    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><House class="nav-icon" /></router-link>
      <router-link to="/map" class="nav-item active"><MapIcon class="nav-icon" /></router-link>
      <router-link to="/cart" class="nav-item "><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/cards" class="nav-item"><Ticket class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item"><User class="nav-icon" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft, House, Map as MapIcon, ShoppingCart, Ticket, User } from 'lucide-vue-next'
import RoutePlanner from '../components/RoutePlanner.vue'
</script>

<style scoped>
.plan-trip-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  background: var(--color-background);
}

.plan-trip-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: scroll;
}
</style>
