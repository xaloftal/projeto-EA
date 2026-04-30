<template>
  <div class="notifications-container app-screen">
    <!-- Header -->
    <header class="app-header">
      <router-link to="/home" class="back-btn" aria-label="Back"><ArrowLeft class="icon-md" /></router-link>
      <div class="title-block">
        <h1>Notifications</h1>
        <p class="subtitle">Updates from stops you follow</p>
      </div>
      <div style="width: 2rem"></div>
    </header>

    <div class="notifications-content">
      <div v-if="isLoading" class="loading-state">Loading notifications...</div>

      <div v-else-if="error" class="error-state">
        <p>Could not load notifications</p>
        <button class="btn-primary" @click="refreshNotifications">Try again</button>
      </div>

      <div v-else-if="notifications.length === 0" class="empty-state">
        <p><Bell class="empty-icon" /></p>
        <p>No notifications yet</p>
        <p class="empty-hint">When a bus arrives at one of your saved stops, it will appear here.</p>
      </div>

      <div v-else class="notifications-list">
        <article v-for="notification in notifications" :key="notification.id" class="notification-card">
          <div class="notification-icon">
            <Bell class="notification-bell" />
          </div>
          <div class="notification-body">
            <div class="notification-head">
              <h2>{{ notification.stopName }}</h2>
              <div class="notification-actions">
                <span class="notification-time">{{ formatDate(notification.createdAt) }}</span>
                <button
                  class="delete-btn"
                  :disabled="deletingIds.includes(notification.id)"
                  @click="removeNotification(notification.id)"
                >
                  {{ deletingIds.includes(notification.id) ? '...' : '' }}
                  <Trash2 v-if="!deletingIds.includes(notification.id)" class="delete-icon" />
                </button>
              </div>
            </div>
            <p class="notification-message">{{ notification.message }}</p>
          </div>
        </article>
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
import { onBeforeUnmount, onMounted } from 'vue'
import { ArrowLeft, Ticket, House, Map, ShoppingCart, User, Trash2, Bell } from 'lucide-vue-next'
import { useNotificationViewModel } from '../viewmodels'

const { notifications, isLoading, error, deletingIds, fetchNotifications, deleteNotification } = useNotificationViewModel()

let pollingHandle: number | undefined

const refreshNotifications = async () => {
  await fetchNotifications()
}

const removeNotification = async (notificationId: string) => {
  await deleteNotification(notificationId)
}

const formatDate = (value: string) =>
  new Intl.DateTimeFormat('pt-PT', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))

onMounted(async () => {
  await refreshNotifications()
  pollingHandle = window.setInterval(refreshNotifications, 15000)
})

onBeforeUnmount(() => {
  if (pollingHandle) {
    window.clearInterval(pollingHandle)
  }
})
</script>

<style scoped>

.title-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.15rem;
}

.subtitle {
  margin: 0;
  font-size: 0.75rem;
  color: #7c7c7c;
}

.notifications-content {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
}

.loading-state,
.error-state,
.empty-state {
  min-height: 55vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #999;
}

.empty-state {
  gap: 0.75rem;
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

.empty-hint {
  max-width: 18rem;
  font-size: 0.9rem;
  line-height: 1.4;
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.notification-card {
  display: flex;
  gap: 0.9rem;
  padding: 1rem;
  border-radius: 1rem;
  background: linear-gradient(135deg, rgba(12, 74, 110, 0.08), rgba(15, 23, 42, 0.04));
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
}

.notification-icon {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 999px;
  background: #0f172a;
  color: white;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
}

.notification-bell {
  width: 1.1rem;
  height: 1.1rem;
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.35rem;
}

.notification-head h2 {
  margin: 0;
  font-size: 1rem;
}

.notification-time {
  font-size: 0.8rem;
  color: #718096;
  white-space: nowrap;
}

.notification-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.delete-btn {
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  width: 1.8rem;
  height: 1.8rem;
  border-radius: 999px;
  display: grid;
  place-items: center;
}

.delete-btn:hover:enabled {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}

.delete-btn:disabled {
  opacity: 0.5;
  cursor: default;
}

.delete-icon {
  width: 1rem;
  height: 1rem;
}

.notification-message {
  margin: 0;
  color: #334155;
  line-height: 1.4;
}
</style>
