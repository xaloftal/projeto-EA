<template>
  <div class="profile-container app-screen">
    <!-- Header -->
    <header class="app-header">
      <router-link to="/home" class="back-btn" aria-label="Back">
        <ArrowLeft class="icon-md" />
      </router-link>
      <h1>Profile</h1>
      <router-link to="/profile/edit" class="edit-btn" aria-label="Edit Profile">
        <Pencil class="icon-md" />
      </router-link>
    </header>

    <div class="profile-content">
      <!-- Profile Card -->
      <div class="profile-card">
        <div class="profile-picture">
          <div class="avatar">
            <User class="avatar-icon" />
          </div>
        </div>
        <h2>{{ ((profileViewModel.currentUser as any)?.value?.name) || 'User' }}</h2>
      </div>

      <!-- Menu Items -->
      <nav class="profile-menu">
        <a href="#" class="menu-item">
          <span class="menu-icon">
            <Settings class="icon-md" />
          </span>
          <span>Account Setting</span>
          <span class="chevron">
            <ChevronRight class="icon-sm" />
          </span>
        </a>
        <a href="#" class="menu-item">
          <span class="menu-icon">
            <CircleHelp class="icon-md" />
          </span>
          <span>Support</span>
          <span class="chevron">
            <ChevronRight class="icon-sm" />
          </span>
        </a>
        <a href="#" class="menu-item" @click.prevent="togglePOIMenu">
          <span class="menu-icon">
            <Star class="icon-md" />
          </span>
          <span>Favorite Stops</span>
          <span class="chevron" :style="{ transform: showPOIMenu ? 'rotate(90deg)' : 'rotate(0deg)' }">
            <ChevronRight class="icon-sm" />
          </span>
        </a>
        <div v-if="showPOIMenu" class="poi-submenu">
          <div v-if="poiList.length === 0" class="no-poi">
            <p>No favorite stops yet. Add stops from the map!</p>
          </div>
          <div v-for="stop in poiList" :key="stop.id" class="poi-item">
            <div class="poi-info">
              <p class="poi-name">{{ stop.name }}</p>
              <small class="poi-type">{{ stop.stopType || 'STOP' }}</small>
            </div>
            <button class="poi-remove-btn" @click.prevent="removeStopFromPOI(stop.id)" :disabled="isLoadingRemove"
              title="Remove from favorites">
              <X :size="18" />
            </button>
          </div>
        </div>
        <router-link v-slot="{ href, navigate }" v-if="profileViewModel.currentUser.value?.isAdmin" to="/admin/reports" custom>
          <a :href="href" @click="navigate" class="menu-item">
            <span class="menu-icon">
              <BarChart3 class="icon-md" />
            </span>
            <span>Admin Reports</span>
            <span class="chevron">
              <ChevronRight class="icon-sm" />
            </span>
          </a>
        </router-link>
        <router-link to="/history" class="menu-item">
          <span class="menu-icon">
            <Bus class="icon-md" />
          </span>
          <span>Travel History</span>
          <span class="chevron">
            <ChevronRight class="icon-sm" />
          </span>
        </router-link>
        <router-link to="/schedule" class="menu-item">
          <span class="menu-icon">
            <CalendarDays class="icon-md" />
          </span>
          <span>Routes & Schedule</span>
          <span class="chevron">
            <ChevronRight class="icon-sm" />
          </span>
        </router-link>
        <a href="#" class="menu-item">
          <span class="menu-icon">
            <LogOut class="icon-md" />
          </span>
          <span @click.prevent="handleLogout" class="logout-link">Logout</span>
          <span class="chevron">
            <ChevronRight class="icon-sm" />
          </span>
        </a>
      </nav>
    </div>

    <!-- Bottom Navigation -->
    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item">
        <House class="nav-icon" />
      </router-link>
      <router-link to="/map" class="nav-item"><Map class="nav-icon" /></router-link>
      <router-link to="/cart" class="nav-item">
        <ShoppingCart class="nav-icon" />
      </router-link>
      <router-link to="/cards" class="nav-item">
        <Ticket class="nav-icon" />
      </router-link>
      <router-link to="/profile" class="nav-item active">
        <User class="nav-icon" />
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ArrowLeft, Bus, CalendarDays, ChevronRight, CircleHelp, House, LogOut, Pencil, Map, Settings, ShoppingCart, User, Ticket, Star, X, BarChart3 } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useAuthViewModel, useProfileViewModel } from '../viewmodels'
import { catchitApi } from '../services/api/catchitApi'
import type { Stop } from '../models'

const router = useRouter()
const { logout } = useAuthViewModel()
const profileViewModel = useProfileViewModel()
const showPOIMenu = ref(false)
const poiList = ref<Stop[]>([])
const isLoadingRemove = ref(false)

const togglePOIMenu = async () => {
  showPOIMenu.value = !showPOIMenu.value
  if (showPOIMenu.value && poiList.value.length === 0) {
    await loadPOIs()
  }
}

const loadPOIs = async () => {
  if (!profileViewModel.currentUser?.value?.id) {
    console.warn('User not available, skipping POI load')
    return
  }

  try {
    const response = await catchitApi.getUserPOI(profileViewModel.currentUser.value.id)
    if (response.success && response.data) {
      poiList.value = response.data
      console.log('Loaded', poiList.value.length, 'POIs')
    } else {
      console.warn('Error loading POIs:', response.error)
      poiList.value = []
    }
  } catch (error) {
    console.error('Error loading user POIs:', error)
    poiList.value = []
  }
}

const removeStopFromPOI = async (stopId: string) => {
  if (!profileViewModel.currentUser?.value?.id) return
  if (isLoadingRemove.value) return

  isLoadingRemove.value = true
  try {
    const response = await catchitApi.removePOI(profileViewModel.currentUser.value.id, stopId)
    if (response.success) {
      poiList.value = poiList.value.filter((stop) => stop.id !== stopId)
      console.log('Removed POI:', stopId)
    } else {
      console.error('Error removing POI:', response.error)
    }
  } catch (error) {
    console.error('Error removing POI:', error)
  } finally {
    isLoadingRemove.value = false
  }
}

const handleLogout = async () => {
  await logout()
  void router.push('/login')
}

onMounted(async () => {
  // Load POIs when profile is mounted
  await loadPOIs()
})
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
  transition: transform 0.2s ease;
}

.poi-submenu {
  background: #f9fafb;
  padding: 0;
  border-bottom: 1px solid #e0e0e0;
}

.no-poi {
  padding: 1rem;
  text-align: center;
  color: #6b7280;
  font-size: 0.9rem;
}

.poi-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e5e7eb;
  background: white;
  gap: 0.5rem;
}

.poi-item:last-child {
  border-bottom: none;
}

.poi-info {
  flex: 1;
}

.poi-name {
  margin: 0;
  font-size: 0.95rem;
  color: #111827;
  font-weight: 500;
}

.poi-type {
  margin: 0.2rem 0 0 0;
  display: block;
  color: #6b7280;
  font-size: 0.8rem;
}

.poi-remove-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  border: none;
  background: transparent;
  color: #dc2626;
  cursor: pointer;
  transition: color 0.2s ease;
  padding: 0;
  flex-shrink: 0;
}

.poi-remove-btn:hover {
  color: #b91c1c;
}

.poi-remove-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.bottom-nav {
  margin-top: auto;
}

.nav-item:hover {
  color: #667eea;
}
</style>
