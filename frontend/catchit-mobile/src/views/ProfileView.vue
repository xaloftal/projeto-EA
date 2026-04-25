<template>
  <div class="profile-container app-screen">
    <!-- Header -->
    <header class="app-header">
      <router-link to="/home" class="back-btn" aria-label="Back"><ArrowLeft class="icon-md" /></router-link>
      <h1>Profile</h1>
      <router-link to="/profile/edit" class="edit-btn" aria-label="Edit Profile"><Pencil class="icon-md" /></router-link>
    </header>

    <div class="profile-content">
      <!-- Profile Card -->
      <div class="profile-card">
        <div class="profile-picture">
          <div class="avatar"><User class="avatar-icon" /></div>
        </div>
        <h2>{{ ((profileViewModel.currentUser as any)?.value?.name) || 'User' }}</h2>
      </div>

      <!-- Menu Items -->
      <nav class="profile-menu">
        <a href="#" class="menu-item">
          <span class="menu-icon"><Settings class="icon-md" /></span>
          <span>Account Setting</span>
          <span class="chevron"><ChevronRight class="icon-sm" /></span>
        </a>
        <a href="#" class="menu-item">
          <span class="menu-icon"><CircleHelp class="icon-md" /></span>
          <span>Support</span>
          <span class="chevron"><ChevronRight class="icon-sm" /></span>
        </a>
        <a href="#" class="menu-item">
          <span class="menu-icon"><Bus class="icon-md" /></span>
          <span>Travel History</span>
          <span class="chevron"><ChevronRight class="icon-sm" /></span>
        </a>
        <a href="#" class="menu-item">
          <span class="menu-icon"><LogOut class="icon-md" /></span>
          <span @click.prevent="handleLogout" class="logout-link">Logout</span>
          <span class="chevron"><ChevronRight class="icon-sm" /></span>
        </a>
      </nav>
    </div>

    <!-- Bottom Navigation -->
    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><House class="nav-icon" /></router-link>
      <router-link to="/map" class="nav-item"><Map class="nav-icon" /></router-link>
      <router-link to="/cart" class="nav-item"><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/cards" class="nav-item"><Ticket class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item active"><User class="nav-icon" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ArrowLeft, Bus, ChevronRight, CircleHelp, House, LogOut, Pencil, Map, Settings, ShoppingCart, User, Ticket } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useAuthViewModel, useProfileViewModel } from '../viewmodels'

const router = useRouter()
const { logout } = useAuthViewModel()
const profileViewModel = useProfileViewModel()

const handleLogout = async () => {
  await logout()
  void router.push('/login')
}
</script>

<style scoped>


.profile-content {
  flex: 1;
  overflow-y: auto;
}

.profile-container .app-header {
  min-height: 6.3rem;
}

.profile-card {
  background: white;
  padding: 2rem 1rem;
  text-align: center;
  border-bottom: 1px solid #e0e0e0;
}

.profile-picture {
  margin-bottom: 1rem;
}

.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
}

.avatar-icon {
  width: 3rem;
  height: 3rem;
}

.profile-card h2 {
  margin: 1rem 0 0 0;
  font-size: 1.5rem;
  color: #333;
}

.profile-menu {
  display: flex;
  flex-direction: column;
  gap: 1px;
  margin-top: 0.5rem;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: white;
  text-decoration: none;
  color: #333;
  transition: background 0.2s;
  border-bottom: 1px solid #e0e0e0;
}

.menu-item:hover {
  background: var(--color-screen-bg);
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  flex-shrink: 0;
}

.menu-item span:nth-child(2) {
  flex: 1;
}

.logout-link {
  color: #f44336;
  cursor: pointer;
}

.chevron {

  width: 1.25rem;
  height: 1.25rem;
}


  .bottom-nav {
    margin-top: auto;
}

.nav-item:hover {
  color: #667eea;
}
</style>
