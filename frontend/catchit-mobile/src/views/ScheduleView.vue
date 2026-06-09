<template>
  <div class="schedule-container app-screen">
    <header class="app-header">
      <router-link to="/profile" class="back-btn" aria-label="Back">
        <ArrowLeft class="icon-md" />
      </router-link>
      <h1>Routes & Schedule</h1>
      <div style="width: 1rem"></div>
    </header>

    <div class="content">
      <section class="single-line-filter">
        <div class="search-input-wrapper">
          <Search class="icon-search" />
          <input
            id="schedule-search"
            v-model="searchQuery"
            type="text"
            placeholder="Search routes or stops..."
          />
        </div>

        <div class="transport-type-wrapper">
          <SlidersHorizontal class="icon-filter" />
          <select id="transport-type" v-model="selectedTransportType">
            <option v-for="option in transportTypeOptions" :key="option" :value="option">
              {{ transportTypeLabel(option) }}
            </option>
          </select>
        </div>

        <button v-if="searchQuery || selectedTransportType !== 'ALL'" class="clear-icon-btn" @click="clearFilters" title="Clear Filters">
          <X class="icon-clear" />
        </button>
      </section>

      <!-- Suggestions list when searching -->
      <section v-if="searchQuery && (routeOptions.length > 0 || stopOptions.length > 0)" class="suggestions-card">
        <div v-if="routeOptions.length > 0" class="suggestions-section">
          <h3>Routes</h3>
          <div class="suggestions-grid">
            <button
              v-for="routeOpt in routeOptions"
              :key="routeOpt.id"
              class="suggestion-item-btn"
              @click="selectRoute(routeOpt.id); searchQuery = ''"
            >
              {{ routeOpt.name }}
            </button>
          </div>
        </div>
        <div v-if="stopOptions.length > 0" class="suggestions-section">
          <h3>Stops</h3>
          <div class="suggestions-grid">
            <button
              v-for="stopOpt in stopOptions"
              :key="stopOpt.stopId"
              class="suggestion-item-btn"
              @click="selectStop(stopOpt.stopId); searchQuery = ''"
            >
              {{ stopOpt.stopCode ??  stopOpt.stopName }}
            </button>
          </div>
        </div>
      </section>

      <div v-if="selectedRoute || selectedStop" class="subtle-focus-container">
        <div v-if="selectedRoute" class="subtle-focus-item">
          <span class="focus-kicker">Route</span>
          <span class="focus-value">{{ selectedRoute.name }}</span>
          <span class="focus-meta">({{ selectedRoute.distinctStopCount }} stops)</span>
          <button class="focus-clear" @click="clearSelection"><X class="icon-xs" /></button>
        </div>

        <div v-else-if="selectedStop" class="subtle-focus-item">
          <span class="focus-kicker">Stop</span>
          <span class="focus-value">{{ selectedStop.stopCode ? `(${selectedStop.stopCode}) - ` : '' }}{{ selectedStop.stopName }}</span>
          <button class="focus-clear" @click="clearSelection"><X class="icon-xs" /></button>
        </div>
      </div>

      <p v-if="error" class="msg-error">{{ error }}</p>

      <div v-if="isLoading" class="loading-state">
        <LoaderCircle class="spinner-icon" />
        <p>Loading route schedules...</p>
      </div>

      <div v-else-if="filteredRoutes.length === 0" class="empty-state">
        <p>No routes match the current filters.</p>
      </div>

      <!-- Table only shows up when a route or stop is actively selected -->
      <div v-else-if="selectedTimetable" class="timetable-card">
        <div class="timetable-scroll">
          <table class="timetable-table">
            <thead>
              <tr>
                <!-- Label adapts based on whether a route or stop is selected -->
                <th class="route-column">{{ selectedTimetable.kind === 'route' ? 'Stop' : 'Route' }}</th>
                <th class="stop-column">Times</th>
              </tr>
            </thead>
            <tbody>
              <!-- Maps rows sequentially using code as the unique identifier -->
              <tr v-for="row in selectedTimetable.rows" :key="row.code">
                <th class="route-row-cell">
                  <div class="route-row-button">
                    <span class="route-id">{{ row.code }}</span>
                  </div>
                </th>
                <td class="stop-cell">
                  <div class="time-container" style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
                    <template v-if="row.times && row.times.length">
                      <span v-for="time in row.times" :key="time" class="cell-time">
                        {{ time }}
                      </span>
                    </template>
                    <span v-else class="cell-empty">--</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-else class="empty-state select-prompt">
        <Search class="prompt-icon" />
        <p>Search and select a route or stop above to view its schedule.</p>
      </div>
    </div>

    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><House class="nav-icon" /></router-link>
      <router-link to="/map" class="nav-item"><Map class="nav-icon" /></router-link>
      <router-link to="/cart" class="nav-item"><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/cards" class="nav-item"><Ticket class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item "><User class="nav-icon active" /></router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import {
  ArrowLeft,
  House,
  LoaderCircle,
  Map,
  Search,
  ShoppingCart,
  SlidersHorizontal,
  Ticket,
  User,
  X,
} from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import { useScheduleViewModel } from '../viewmodels'

const route = useRoute()
const initialRouteId = typeof route.query.routeId === 'string' ? route.query.routeId : ''
const scheduleViewModel = useScheduleViewModel(initialRouteId)

const {
  searchQuery,
  selectedTransportType,
  selectedRoute,
  selectedStop,
  selectedTimetable,
  filteredRoutes,
  transportTypeOptions,
  routeOptions,
  stopOptions,
  selectRoute,
  selectStop,
  loadRoutes,
  clearFilters,
  clearSelection,
  transportTypeLabel,
  isLoading,
  error,
} = scheduleViewModel

onMounted(() => {
  void loadRoutes()
})
</script>

<style scoped>
.schedule-container {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  background:
    radial-gradient(circle at top left, rgba(102, 126, 234, 0.14), transparent 34%),
    radial-gradient(circle at top right, rgba(15, 23, 42, 0.08), transparent 28%),
    var(--color-screen-bg);
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.route-card {
  background: var(--color-surface);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  box-shadow: var(--shadow-card);
}

.single-line-filter {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--color-surface);
  padding: 0.6rem;
  border-radius: 99px;
  box-shadow: var(--shadow-card);
}

.search-input-wrapper {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.search-input-wrapper input {
  width: 100%;
  border: none;
  background: transparent;
  outline: none;
  padding: 0.5rem 0.5rem 0.5rem 2.2rem;
  font-size: 0.95rem;
  color: #111827;
}

.icon-search {
  position: absolute;
  left: 0.6rem;
  width: 1.1rem;
  height: 1.1rem;
  color: #6b7280;
}

.transport-type-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  background: #f1f5f9;
  border-radius: 99px;
  padding: 0 0.8rem;
  height: 2.4rem;
}

.transport-type-wrapper select {
  appearance: none;
  border: none;
  background: transparent;
  outline: none;
  padding-left: 1.5rem;
  font-weight: 600;
  color: #334155;
  font-size: 0.85rem;
  cursor: pointer;
}

.icon-filter {
  position: absolute;
  left: 0.6rem;
  width: 1rem;
  height: 1rem;
  color: #475569;
  pointer-events: none;
}

.clear-icon-btn {
  background: transparent;
  border: none;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #94a3b8;
  transition: color 0.2s;
  padding-right: 0.4rem;
}

.clear-icon-btn:hover {
  color: #ef4444;
}

.icon-clear {
  width: 1.2rem;
  height: 1.2rem;
}

.subtle-focus-container {
  display: flex;
  justify-content: center;
  margin-top: 0.2rem;
}

.subtle-focus-item {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: #e2e8f0;
  padding: 0.4rem 0.8rem;
  border-radius: 8px;
  font-size: 0.85rem;
}

.focus-kicker {
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  font-size: 0.75rem;
}

.focus-value {
  font-weight: 600;
  color: #0f172a;
}

.focus-meta {
  color: #64748b;
  font-size: 0.8rem;
}

.focus-clear {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.1);
  border: none;
  border-radius: 50%;
  width: 1.2rem;
  height: 1.2rem;
  cursor: pointer;
  color: #334155;
  margin-left: 0.2rem;
}

.focus-clear:hover {
  background: rgba(15, 23, 42, 0.2);
}

.icon-xs {
  width: 0.8rem;
  height: 0.8rem;
}

.loading-state,
.empty-state {
  padding: 2.5rem 1rem;
  text-align: center;
  color: #6b7280;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
}

.prompt-icon {
  width: 2.5rem;
  height: 2.5rem;
  color: #9ca3af;
  margin-bottom: 0.25rem;
}

.select-prompt p {
  font-weight: 500;
  color: #4b5563;
  margin: 0;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.6rem;
}

.msg-error {
  color: #dc2626;
  font-size: 0.9rem;
  font-weight: 700;
  margin: 0;
}

.bottom-nav {
  margin-top: auto;
}

@media (max-width: 640px) {
  .selected-route-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .route-row-button {
    min-width: 5rem;
  }
}

.timetable-card {
  background: var(--color-surface);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  box-shadow: var(--shadow-card);
  padding: 1rem;
}

.timetable-scroll {
  overflow-x: auto;
  padding-bottom: 0.35rem;
}

.timetable-table {
  width: max-content;
  min-width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.timetable-table th,
.timetable-table td {
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  border-right: 1px solid rgba(15, 23, 42, 0.08);
  vertical-align: top;
}

.timetable-table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f8fafc;
  color: #111827;
  text-align: left;
  font-size: 0.8rem;
  font-weight: 800;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  padding: 0.9rem;
}

.route-column {
  min-width: 6rem;
  background: #f8fafc;
}

.stop-column {
  min-width: 9.5rem;
}

.route-row-cell {
  background: #ffffff;
  padding: 0;
  position: sticky;
  left: 0;
  z-index: 1;
}

.route-row-button {
  width: 100%;
  min-height: 100%;
  border: none;
  background: #ffffff;
  padding: 0.95rem;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.25rem;
  text-align: left;
}

.route-row-button .route-id {
  color: #111827;
  font-size: 0.98rem;
  font-weight: 800;
}

.stop-cell {
  min-width: 9.5rem;
  padding: 0.95rem 0.85rem;
  background: #ffffff;
}

.timetable-table tr.active .route-row-button,
.timetable-table tr.active .route-column,
.timetable-table tr.active .stop-cell {
  background: rgba(102, 126, 234, 0.04);
}

.cell-time {
  color: #111827;
  font-size: 0.9rem;
  font-weight: 800;
  background: rgba(15, 23, 42, 0.05);
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
}

.cell-empty {
  color: #cbd5e1;
  font-weight: 700;
}

.suggestions-card {
  background: var(--color-surface);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  box-shadow: var(--shadow-card);
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.suggestions-section h3 {
  margin: 0 0 0.5rem 0;
  font-size: 0.85rem;
  font-weight: 700;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.suggestions-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.suggestion-item-btn {
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background: #ffffff;
  color: #111827;
  padding: 0.5rem 0.75rem;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.suggestion-item-btn:hover {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.05);
  color: #4f46e5;
}
</style>