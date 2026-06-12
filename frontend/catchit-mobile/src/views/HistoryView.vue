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
        <LoaderCircle class="spinner-icon" />
        <p>Loading your trips...</p>
      </div>

      <div v-else-if="records.length > 0" class="history-list">
        <div v-for="record in records" :key="record.id" class="history-item">
          <div class="history-info">
            <!-- Nome da rota: fromStop → toStop -->
            <p class="route-name">
              <span class="from-stop">{{ getFromStopName(record) }}</span>
              <ArrowRight class="arrow-icon" />
              <span class="to-stop">{{ getToStopName(record) }}</span>
            </p>
            
            <div class="meta-info">
              <small class="timestamp">{{ formatDate(record.timestamp) }}</small>
              
              <!-- Badge de saída correta/incorreta -->
              <span v-if="record.correctExit !== null && record.correctExit !== undefined" 
                    :class="['exit-badge', record.correctExit ? 'exit-correct' : 'exit-warning']">
                {{ record.correctExit ? '✓ Saída correta' : '⚠️ Saída após destino' }}
              </span>
              
              <!-- Badge de checkout automático -->
              <span v-if="record.automatedExit" class="exit-auto">
                🤖 Automático
              </span>
            </div>
            
            <!-- Paragem de saída -->
            <div class="stop-info" v-if="record.stop">
              <span class="stop-label">Saída em:</span>
              <span class="stop-name">{{ record.stop.name }}</span>
              <span class="stop-code" v-if="record.stop.stopCode">({{ record.stop.stopCode }})</span>
            </div>
            
            <!-- Preço do bilhete -->
            <div class="price-info" v-if="getTicketPrice(record) !== null">
              <span class="price-label">Preço:</span>
              <span class="price-value">{{ formatPrice(getTicketPrice(record)!) }}</span>
            </div>
          </div>
          
          <div class="status-badge" :class="getStatusClass(record)">
            {{ getStatusText(record) }}
          </div>
        </div>
      </div>

      <div v-else class="no-data">
        <Bus class="empty-icon" />
        <p>No travel history found.</p>
      </div>
    </div>

    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><Home class="nav-icon" /></router-link>
      <router-link to="/map" class="nav-item"><Map class="nav-icon" /></router-link>
      <router-link to="/cart" class="nav-item"><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/cards" class="nav-item"><Ticket class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item"><User class="nav-icon" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ArrowLeft, Bus, LoaderCircle, ArrowRight, Home, Map, ShoppingCart, Ticket, User } from 'lucide-vue-next'
import { useAuthViewModel } from '../viewmodels'
import { catchitApi } from '../services/api/catchitApi'

// Interface para o DTO que vem do backend
interface StopInfo {
  id: string
  name: string
  stopCode: string
  stopType: string
  latitude: number
  longitude: number
  displayCode: string
}

interface TitleDTO {
  id: string
  status: string
  fromStop: StopInfo | null
  toStop: StopInfo | null
  price: number | null
}

interface ExitRecordDTO {
  id: string
  timestamp: string
  correctExit: boolean | null
  automatedExit: boolean
  titles: TitleDTO[]
  stop: StopInfo | null
  trip: { id: string } | null
}

const { currentUser } = useAuthViewModel()
const records = ref<ExitRecordDTO[]>([])
const isLoading = ref(false)

// Formatar data
const formatDate = (timestamp: string) => {
  if (!timestamp) return 'Data desconhecida'
  try {
    return new Date(timestamp).toLocaleString('pt-PT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return timestamp
  }
}

// Obter nome da paragem de origem
const getFromStopName = (record: ExitRecordDTO) => {
  const ticket = record.titles?.[0]
  if (!ticket) return 'Origem'
  return ticket.fromStop?.name || 'Origem'
}

// Obter nome da paragem de destino
const getToStopName = (record: ExitRecordDTO) => {
  const ticket = record.titles?.[0]
  if (!ticket) return 'Destino'
  return ticket.toStop?.name || 'Destino'
}

// Obter preço do bilhete
const getTicketPrice = (record: ExitRecordDTO): number | null => {
  const ticket = record.titles?.[0]
  return ticket?.price !== undefined && ticket?.price !== null ? ticket.price : null
}

// Formatar preço
const formatPrice = (price: number): string => {
  return new Intl.NumberFormat('pt-PT', {
    style: 'currency',
    currency: 'EUR'
  }).format(price)
}

// Obter classe CSS para o status
const getStatusClass = (record: ExitRecordDTO) => {
  const ticket = record.titles?.[0]
  if (!ticket) return 'status-other'
  
  const status = ticket.status?.toUpperCase()
  if (status === 'USED') return 'status-used'
  if (status === 'VALIDATED') return 'status-validated'
  if (status === 'ACTIVE') return 'status-active'
  if (status === 'EXPIRED') return 'status-expired'
  return 'status-other'
}

// Obter texto do status
const getStatusText = (record: ExitRecordDTO) => {
  const ticket = record.titles?.[0]
  if (!ticket) return 'DESCONHECIDO'
  
  switch (ticket.status?.toUpperCase()) {
    case 'USED': return 'UTILIZADO'
    case 'VALIDATED': return 'VALIDADO'
    case 'ACTIVE': return 'ATIVO'
    case 'EXPIRED': return 'EXPIRADO'
    default: return ticket.status || 'DESCONHECIDO'
  }
}

const loadHistory = async () => {
  const userId = currentUser.value?.id
  if (!userId) {
    console.warn('No user ID found')
    return
  }

  isLoading.value = true
  try {
    console.log('Loading history for user:', userId)
    const response = await catchitApi.getUserHistory(userId)
    console.log('History response:', response)
    
    if (response.success && response.data) {
      records.value = response.data
      console.log('Records loaded:', records.value.length)
    } else {
      console.error('Error loading history:', response.error)
    }
  } catch (error) {
    console.error('Error loading history:', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadHistory()
})
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
  align-items: flex-start;
  justify-content: space-between;
  padding: 1rem;
  background: white;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s;
}

.history-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.history-info {
  flex: 1;
}

.route-name {
  margin: 0 0 0.5rem 0;
  font-weight: 600;
  font-size: 0.95rem;
  color: #111827;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.from-stop {
  color: #374151;
}

.to-stop {
  color: #374151;
}

.arrow-icon {
  width: 1rem;
  height: 1rem;
  color: #9ca3af;
}

.meta-info {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.timestamp {
  color: #6b7280;
  font-size: 0.7rem;
}

.exit-badge {
  font-size: 0.7rem;
  padding: 0.125rem 0.375rem;
  border-radius: 4px;
  font-weight: 500;
}

.exit-correct {
  background: #d1fae5;
  color: #065f46;
}

.exit-warning {
  background: #fef3c7;
  color: #92400e;
}

.exit-auto {
  font-size: 0.7rem;
  padding: 0.125rem 0.375rem;
  background: #e0e7ff;
  color: #3730a3;
  border-radius: 4px;
}

.stop-info, .price-info {
  display: flex;
  gap: 0.5rem;
  font-size: 0.75rem;
  margin-top: 0.25rem;
}

.stop-label, .price-label {
  color: #6b7280;
}

.stop-name, .price-value {
  color: #374151;
  font-weight: 500;
}

.stop-code {
  color: #9ca3af;
  font-size: 0.7rem;
}

.status-badge {
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  font-size: 0.7rem;
  font-weight: 600;
  white-space: nowrap;
}

.status-used {
  background: #d1fae5;
  color: #065f46;
}

.status-validated {
  background: #dbeafe;
  color: #1e40af;
}

.status-active {
  background: #fed7aa;
  color: #9a3412;
}

.status-expired {
  background: #fee2e2;
  color: #991b1b;
}

.status-other {
  background: #f3f4f6;
  color: #4b5563;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  gap: 0.75rem;
  color: #6b7280;
}

.spinner-icon {
  width: 2rem;
  height: 2rem;
  color: var(--color-brand, #3b82f6);
  animation: spin 1s linear infinite;
}

.no-data {
  text-align: center;
  padding: 3rem 1rem;
  color: #6b7280;
}

.empty-icon {
  width: 3rem;
  height: 3rem;
  margin-bottom: 1rem;
  color: #d1d5db;
}

.spacer {
  width: 24px;
}

.bottom-nav {
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 0.75rem 1rem;
  background: white;
  border-top: 1px solid #e5e7eb;
  position: sticky;
  bottom: 0;
}

.nav-item {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem;
  color: #6b7280;
  transition: color 0.2s;
}

.nav-item.router-link-active {
  color: var(--color-brand, #3b82f6);
}

.nav-icon {
  width: 1.5rem;
  height: 1.5rem;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>