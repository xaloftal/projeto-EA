<template>
  <div class="history-container app-screen">
    <header class="app-header">
      <router-link to="/profile" class="back-btn" aria-label="Back">
        <ArrowLeft class="icon-md" />
      </router-link>
      <h1>Travel History</h1>
      <div class="spacer"></div>
    </header>

    <div class="history-content">
      <div v-if="isLoading" class="loading-state">
        <p>Loading your trips...</p>
      </div>

      <div v-else-if="records.length > 0" class="history-list">
        <div v-for="record in records" :key="record.id" class="history-item">
          <div class="history-info">
            <p class="route-name">
              {{ record.titles?.[0]?.from?.name || 'Origin' }} → {{ record.stop?.name || 'Destination' }}
            </p>
            <small class="timestamp">{{ new Date(record.timestamp).toLocaleString() }}</small>
          </div>
          <div class="status-badge" :class="record.titles?.[0]?.status === 'USED' ? 'status-used' : 'status-other'">
            {{ record.titles?.[0]?.status || 'USED' }}
          </div>
        </div>
      </div>

      <div v-else class="no-data">
        <Bus class="empty-icon" />
        <p>No travel history found.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ArrowLeft, Bus } from 'lucide-vue-next'
import { useAuthViewModel } from '../viewmodels'
import { catchitApi } from '../services/api/catchitApi'

const { currentUser } = useAuthViewModel()
const records = ref<any[]>([])
const isLoading = ref(false)

const loadHistory = async () => {
  const userId = currentUser.value?.id
  if (!userId) return

  isLoading.value = true
  try {
    const response = await catchitApi.getUserHistory(userId)
    if (response.success && response.data) {
      records.value = response.data
    }
  } catch (error) {
    console.error('Error loading history:', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(loadHistory)
</script>

<style scoped>
.history-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f9fafb;
}

.history-content {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  background: white;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.route-name {
  margin: 0;
  font-weight: 600;
  color: #111827;
}

.timestamp {
  color: #6b7280;
}

.status-badge {
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
}

.status-used {
  background: #d1fae5;
  color: #065f46;
}

.no-data {
  text-align: center;
  padding: 3rem 1rem;
  color: #6b7280;
}

.empty-icon {
  margin-bottom: 1rem;
  color: #d1d5db;
}

.spacer { width: 24px; }
</style>