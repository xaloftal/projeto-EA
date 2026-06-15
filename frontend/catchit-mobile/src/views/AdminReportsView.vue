<template>
  <div class="reports-container app-screen">
    <!-- Header -->
    <header class="app-header">
      <button @click="handleLogout" class="back-btn" aria-label="Logout" style="background: transparent; border: none; color: #ef4444; cursor: pointer;">
        <LogOut class="icon-md" />
      </button>
      <h1>Admin Reports</h1>
      <div class="spacer"></div>
    </header>

    <div class="reports-content">
      <!-- Filter Card -->
      <div class="filter-card">
        <h2 class="section-title">Filters</h2>
        <div class="filter-grid">
          <!-- Transport Type Select -->
          <q-select
            filled
            v-model="selectedType"
            :options="typeOptions"
            label="Transport Type"
            emit-value
            map-options
            class="q-mb-md filter-input"
            @update:model-value="onTypeChange"
          />

          <!-- Vehicle Selection Method Toggle -->
          <div class="selection-toggle">
            <q-btn-toggle
              v-model="selectionMode"
              :options="selectionModeOptions"
              toggle-color="primary"
              rounded
              unelevated
              dense
              class="toggle-buttons"
            />
          </div>

          <!-- Dropdown Selection Mode -->
          <div v-if="selectionMode === 'dropdown'">
            <q-select
              filled
              v-model="selectedVehicle"
              :options="filteredVehicles"
              label="Select Transport"
              option-value="id"
              option-label="label"
              emit-value
              map-options
              :disable="!selectedType"
              class="q-mb-md filter-input"
            />
            <div v-if="selectedVehicle" class="vehicle-hint">
              <small>Selected ID: {{ selectedVehicle }}</small>
            </div>
          </div>

          <!-- Manual ID Input Mode -->
          <div v-else>
            <q-input
              filled
              v-model="manualVehicleId"
              label="Vehicle ID (UUID)"
              placeholder="Ex: 9c0f37c0-cddf-4a3a-ad33-bb03692be752"
              class="q-mb-md filter-input"
              :rules="[
                val => !val || isValidUUID(val) || 'Invalid UUID format'
              ]"
            />
            <div v-if="manualVehicleId && isValidUUID(manualVehicleId)" class="vehicle-hint">
              <small>Using manual ID: {{ manualVehicleId.substring(0, 8) }}...</small>
            </div>
          </div>

          <!-- Month Selector - Simple input -->
          <q-input
            filled
            v-model="selectedMonth"
            label="Month (YYYY-MM)"
            placeholder="2026-06"
            hint="Format: Year-Month (e.g., 2026-06)"
            class="q-mb-md filter-input"
          />
        </div>

        <q-btn
          @click="fetchStats"
          :loading="isLoading"
          :disabled="!isValidVehicleSelected || !selectedMonth"
          class="submit-btn"
          unelevated
          label="Show Report"
        />
      </div>

      <!-- Loading State -->
      <div v-if="isLoading" class="loading-state">
        <q-spinner-dots color="primary" size="40px" />
        <p>Generating statistics...</p>
      </div>

      <!-- No Data State -->
      <div v-else-if="searched && hasNoData" class="no-data-card">
        <AlertTriangle class="warning-icon" />
        <h3>No Data Available</h3>
        <p>No data available for the selected period and transport.</p>
        <p class="hint-text">Hint: The vehicle with ID <strong>9c0f37c0-cddf-4a3a-ad33-bb03692be752</strong> has validations registered.</p>
      </div>

      <!-- Report Content -->
      <div v-else-if="searched && stats && stats.totalValidations > 0" class="stats-report">
        <!-- Quick Stats Grid -->
        <div class="stats-grid">
          <div class="stat-card total-card">
            <div class="stat-header">
              <span>Total Validations</span>
              <Activity class="stat-icon" />
            </div>
            <div class="stat-value">{{ stats.totalValidations }}</div>
            <div class="stat-desc">Checked tickets and passes</div>
          </div>

          <div class="stat-card success-card">
            <div class="stat-header">
              <span>Success Rate</span>
              <TrendingUp class="stat-icon" />
            </div>
            <div class="stat-value">{{ successRate.toFixed(1) }}%</div>
            <div class="stat-desc">{{ stats.successfulValidations }} successful</div>
          </div>

          <div class="stat-card failed-card">
            <div class="stat-header">
              <span>Failed Validations</span>
              <AlertTriangle class="stat-icon" />
            </div>
            <div class="stat-value">{{ stats.failedValidations }}</div>
            <div class="stat-desc">{{ failedRate.toFixed(1) }}% failure rate</div>
          </div>
        </div>

        <!-- Download Buttons -->
        <div class="action-bar">
          <q-btn
            @click="showDownloadDialog = true"
            class="download-trigger-btn"
            unelevated
            icon="download"
            label="Download Report"
          />
        </div>

        <!-- Stop Stats Section -->
        <div class="report-section" v-if="Object.keys(stats.validationsByStop || {}).length > 0">
          <h3 class="section-subtitle">
            <MapPin class="sub-icon" />
            Validations by Stop
          </h3>
          <q-table
            flat
            bordered
            :rows="stopRows"
            :columns="stopColumns"
            row-key="stop"
            :pagination="{ rowsPerPage: 5 }"
            class="stats-table"
          />
        </div>

        <!-- Weekly Distribution -->
        <div class="report-section">
          <h3 class="section-subtitle">
            <Calendar class="sub-icon" />
            Weekly Distribution
          </h3>
          <div class="chart-container">
            <div
              v-for="(val, day) in stats.validationsByDayOfWeek"
              :key="day"
              class="chart-bar-wrapper"
            >
              <div class="chart-bar-value">{{ val }}</div>
              <div
                class="chart-bar"
                :style="{ height: getBarHeight(val) + '%' }"
              ></div>
              <div class="chart-bar-label">{{ String(day).substring(0, 3) }}</div>
            </div>
          </div>
        </div>

        <!-- Hourly Peak Section -->
        <div class="report-section q-mb-xl">
          <h3 class="section-subtitle">
            <Activity class="sub-icon" />
            Hourly Distribution (Peak Hours)
          </h3>
          <div class="hourly-list">
            <div
              v-for="hour in peakHours"
              :key="hour.hour"
              class="hour-row"
            >
              <span class="hour-time">{{ String(hour.hour).padStart(2, '0') }}:00</span>
              <div class="hour-bar-bg">
                <div
                  class="hour-bar-fill"
                  :style="{ width: getHourWidth(hour.count) + '%' }"
                ></div>
              </div>
              <span class="hour-count">{{ hour.count }} val.</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Download Options Dialog -->
    <q-dialog v-model="showDownloadDialog">
      <q-card class="download-dialog">
        <q-card-section class="dialog-header">
          <div class="text-h6 font-weight-bold">Export Report</div>
          <p class="q-mt-xs text-grey-6">Select your preferred download format</p>
        </q-card-section>

        <q-card-section class="q-pt-none select-format-section">
          <div class="format-options">
            <div
              @click="downloadFormat = 'pdf'"
              class="format-box"
              :class="{ selected: downloadFormat === 'pdf' }"
            >
              <FileText class="format-icon pdf" />
              <span>PDF Document</span>
            </div>
            <div
              @click="downloadFormat = 'csv'"
              class="format-box"
              :class="{ selected: downloadFormat === 'csv' }"
            >
              <FileText class="format-icon csv" />
              <span>CSV Spreadsheet</span>
            </div>
          </div>
        </q-card-section>

        <q-card-actions align="right" class="dialog-actions">
          <q-btn flat label="Cancel" color="primary" v-close-popup />
          <q-btn
            unelevated
            label="Download"
            color="primary"
            :loading="isDownloading"
            @click="triggerDownload"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { LogOut, AlertTriangle, TrendingUp, MapPin, Calendar, Activity, FileText } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { useAuthViewModel } from '../viewmodels'
import { catchitApi } from '../services/api/catchitApi'

const router = useRouter()
const { logout } = useAuthViewModel()

// UI State
const isLoading = ref(false)
const isDownloading = ref(false)
const searched = ref(false)
const showDownloadDialog = ref(false)
const downloadFormat = ref<'pdf' | 'csv'>('pdf')

// Data State
const vehicles = ref<any[]>([])
const selectedType = ref<string>('')
const selectedVehicle = ref<string>('')
const manualVehicleId = ref<string>('')
const selectedMonth = ref<string>('2026-06') // Default value
const stats = ref<any>(null)

// Selection mode: 'dropdown' or 'manual'
const selectionMode = ref<'dropdown' | 'manual'>('manual')

const selectionModeOptions = [
  { label: 'Select from list', value: 'dropdown' },
  { label: 'Enter ID manually', value: 'manual' },
]

// Type options
const typeOptions = [
  { label: 'Bus', value: 'BUS' },
  { label: 'Train', value: 'TRAIN' },
  { label: 'Metro', value: 'METRO' },
]

// Validate UUID format
const isValidUUID = (uuid: string): boolean => {
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
  return uuidRegex.test(uuid)
}

// Check if a valid vehicle is selected
const isValidVehicleSelected = computed(() => {
  if (selectionMode.value === 'dropdown') {
    return !!selectedVehicle.value
  } else {
    return !!manualVehicleId.value && isValidUUID(manualVehicleId.value)
  }
})

// Get the current vehicle ID based on selection mode
const getCurrentVehicleId = computed(() => {
  if (selectionMode.value === 'dropdown') {
    return selectedVehicle.value
  } else {
    return manualVehicleId.value
  }
})

// Fetch all vehicles on mount
const loadVehicles = async () => {
  try {
    const response = await catchitApi.getVehicles()
    if (response.success && response.data) {
      vehicles.value = response.data
      console.log('=== VEHICLES LOADED ===')
      console.log('Total vehicles:', vehicles.value.length)
      vehicles.value.forEach(v => {
        console.log(`ID: ${v.id}, Type: ${v.type}, Route: ${v.route?.name || 'No route'}`)
      })
      
      console.log('=== VEHICLE WITH VALIDATIONS ===')
      console.log('Use this ID for testing: 9c0f37c0-cddf-4a3a-ad33-bb03692be752')
    }
  } catch (error) {
    console.error('Error loading vehicles:', error)
  }
}

// Reset vehicle choice when type changes
const onTypeChange = () => {
  selectedVehicle.value = ''
}

// Filtered vehicles based on type
const filteredVehicles = computed(() => {
  if (!selectedType.value) return []
  return vehicles.value
    .filter((v) => v.type === selectedType.value)
    .map((v) => {
      const routeName = v.route?.name || 'No Route'
      const shortId = v.id.substring(0, 8)
      return {
        id: v.id,
        label: `${v.type} [${shortId}...] — ${routeName}`,
      }
    })
})

// Check if stats contains data
const hasNoData = computed(() => {
  return !stats.value || stats.value.totalValidations === 0
})

// Math Stats
const successRate = computed(() => {
  if (!stats.value || stats.value.totalValidations === 0) return 0
  return (stats.value.successfulValidations / stats.value.totalValidations) * 100
})

const failedRate = computed(() => {
  if (!stats.value || stats.value.totalValidations === 0) return 0
  return (stats.value.failedValidations / stats.value.totalValidations) * 100
})

// Stop table configuration
const stopColumns = [
  { name: 'stop', align: 'left' as const, label: 'Paragem', field: 'stop', sortable: true },
  { name: 'count', align: 'right' as const, label: 'Nº Validações', field: 'count', sortable: true },
]

const stopRows = computed(() => {
  if (!stats.value || !stats.value.validationsByStop) return []
  return Object.entries(stats.value.validationsByStop).map(([stop, count]) => ({
    stop,
    count,
  }))
})

// Chart heights
const getBarHeight = (value: number) => {
  if (!stats.value || stats.value.totalValidations === 0) return 0
  const max = Math.max(...Object.values(stats.value.validationsByDayOfWeek) as number[])
  if (max === 0) return 0
  return (value / max) * 100
}

// Peak hours list
const peakHours = computed(() => {
  if (!stats.value || !stats.value.validationsByHour) return []
  return Object.entries(stats.value.validationsByHour)
    .map(([hour, count]) => ({
      hour: Number(hour),
      count: Number(count),
    }))
})

const getHourWidth = (count: number) => {
  const max = Math.max(...peakHours.value.map(h => h.count))
  if (max === 0) return 0
  return (count / max) * 100
}

// Fetch stats for vehicle/month
const fetchStats = async () => {
  const vehicleId = getCurrentVehicleId.value
  if (!vehicleId || !selectedMonth.value) {
    console.error('Missing vehicleId or month')
    return
  }

  console.log('=== FETCHING STATS ===')
  console.log('Selected Vehicle ID:', vehicleId)
  console.log('Selected Month:', selectedMonth.value)

  isLoading.value = true
  searched.value = true
  try {
    const response = await catchitApi.getReportStats(vehicleId, selectedMonth.value)
    console.log('API Response:', response)
    if (response.success && response.data) {
      stats.value = response.data
      console.log('Stats data:', stats.value)
    } else {
      stats.value = null
      console.error('API error:', response.error)
    }
  } catch (error) {
    console.error('Error fetching statistics:', error)
    stats.value = null
  } finally {
    isLoading.value = false
  }
}

// Trigger download format
const triggerDownload = async () => {
  const vehicleId = getCurrentVehicleId.value
  if (!vehicleId || !selectedMonth.value) return

  isDownloading.value = true
  try {
    const blob = await catchitApi.downloadReport(
      vehicleId,
      selectedMonth.value,
      downloadFormat.value
    )
    if (blob) {
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      const shortVehicleId = vehicleId.substring(0, 5).toUpperCase()
      a.download = `relatorio_${selectedMonth.value}_${shortVehicleId}.${downloadFormat.value}`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      window.URL.revokeObjectURL(url)
      showDownloadDialog.value = false
    }
  } catch (error) {
    console.error('Error downloading report:', error)
  } finally {
    isDownloading.value = false
  }
}

onMounted(() => {
  void loadVehicles()
})

const handleLogout = async () => {
  await logout()
  void router.push('/login')
}
</script>

<style scoped>
.reports-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f3f4f6;
}

.reports-content {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem 1rem;
}

.filter-card {
  background: white;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  margin-bottom: 1.5rem;
  border: 1px solid #e5e7eb;
}

.section-title {
  margin: 0 0 1.25rem 0;
  font-size: 1.2rem;
  font-weight: 700;
  color: #1f2937;
}

.filter-grid {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.filter-input :deep(.q-field__control) {
  border-radius: 12px;
}

.selection-toggle {
  margin-bottom: 0.75rem;
}

.toggle-buttons {
  width: 100%;
}

.toggle-buttons :deep(.q-btn) {
  flex: 1;
}

.submit-btn {
  width: 100%;
  padding: 0.75rem;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
  text-transform: none;
  font-size: 1rem;
  margin-top: 0.5rem;
  transition: transform 0.2s, opacity 0.2s;
}

.submit-btn:active {
  transform: scale(0.98);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  color: #4b5563;
}

.loading-state p {
  margin-top: 1rem;
  font-weight: 500;
}

.no-data-card {
  background: white;
  border-radius: 16px;
  padding: 2.5rem 1.5rem;
  text-align: center;
  border: 1px solid #e5e7eb;
  color: #4b5563;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

.no-data-card .hint-text {
  margin-top: 1rem;
  font-size: 0.85rem;
  color: #6b7280;
}

.warning-icon {
  width: 3.5rem;
  height: 3.5rem;
  color: #f59e0b;
  margin-bottom: 1rem;
}

.no-data-card h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.3rem;
  font-weight: 700;
  color: #1f2937;
}

.no-data-card p {
  margin: 0;
  font-size: 0.95rem;
  color: #6b7280;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

@media (min-width: 600px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 1.25rem;
  border: 1px solid #e5e7eb;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #6b7280;
  font-size: 0.85rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat-icon {
  width: 1.25rem;
  height: 1.25rem;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 800;
  color: #111827;
  margin: 0.5rem 0;
}

.stat-desc {
  font-size: 0.8rem;
  color: #6b7280;
}

.total-card {
  border-left: 4px solid #667eea;
}

.success-card {
  border-left: 4px solid #10b981;
}

.success-card .stat-icon {
  color: #10b981;
}

.failed-card {
  border-left: 4px solid #ef4444;
}

.failed-card .stat-icon {
  color: #ef4444;
}

.action-bar {
  margin-bottom: 1.5rem;
}

.download-trigger-btn {
  width: 100%;
  padding: 0.75rem;
  border-radius: 12px;
  border: 1px solid #d1d5db;
  background: white;
  color: #374151;
  font-weight: 600;
  text-transform: none;
  font-size: 0.95rem;
  transition: background 0.2s;
}

.download-trigger-btn:hover {
  background: #f9fafb;
}

.report-section {
  background: white;
  border-radius: 16px;
  padding: 1.5rem;
  border: 1px solid #e5e7eb;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
  margin-bottom: 1.5rem;
}

.section-subtitle {
  margin: 0 0 1.25rem 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.sub-icon {
  width: 1.25rem;
  height: 1.25rem;
  color: #667eea;
}

.stats-table {
  background: transparent;
}

.chart-container {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  height: 150px;
  padding: 1.5rem 0.5rem 0.5rem 0.5rem;
  border-bottom: 2px solid #e5e7eb;
  gap: 0.5rem;
}

.chart-bar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  height: 100%;
  justify-content: flex-end;
  position: relative;
}

.chart-bar {
  width: 100%;
  max-width: 35px;
  background: linear-gradient(to top, #667eea, #764ba2);
  border-radius: 4px 4px 0 0;
  transition: height 0.6s ease-out;
  cursor: pointer;
}

.chart-bar:hover {
  filter: brightness(1.1);
}

.chart-bar-value {
  font-size: 0.75rem;
  font-weight: 700;
  color: #4b5563;
  margin-bottom: 0.25rem;
}

.chart-bar-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #6b7280;
  position: absolute;
  bottom: -1.5rem;
  white-space: nowrap;
}

.hourly-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.hour-row {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.hour-time {
  width: 45px;
  font-size: 0.85rem;
  font-weight: 600;
  color: #4b5563;
}

.hour-bar-bg {
  flex: 1;
  height: 12px;
  background: #f3f4f6;
  border-radius: 6px;
  overflow: hidden;
}

.hour-bar-fill {
  height: 100%;
  background: #667eea;
  border-radius: 6px;
  transition: width 0.6s ease-out;
}

.hour-count {
  font-size: 0.85rem;
  font-weight: 600;
  color: #4b5563;
  width: 50px;
  text-align: right;
}

.download-dialog {
  width: 100%;
  max-width: 360px;
  border-radius: 16px;
  overflow: hidden;
}

.dialog-header {
  padding: 1.25rem;
}

.dialog-header h6 {
  margin: 0;
}

.select-format-section {
  padding: 0 1.25rem 1rem 1.25rem;
}

.format-options {
  display: flex;
  gap: 1rem;
}

.format-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1rem;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.format-box:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

.format-box.selected {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.05);
}

.format-icon {
  width: 2.5rem;
  height: 2.5rem;
  margin-bottom: 0.5rem;
}

.format-icon.pdf {
  color: #ef4444;
}

.format-icon.csv {
  color: #10b981;
}

.format-box span {
  font-size: 0.85rem;
  font-weight: 600;
  color: #374151;
}

.dialog-actions {
  padding: 0.75rem 1.25rem 1.25rem 1.25rem;
}

.vehicle-hint {
  margin-top: -0.5rem;
  margin-bottom: 0.75rem;
  font-size: 0.7rem;
  color: #6b7280;
  word-break: break-all;
}
</style>