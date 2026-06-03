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
      <section class="filters-card">
        <div class="filter-field">
          <label for="schedule-search">Search routes or stops</label>
          <div class="input-shell">
            <Search class="input-icon" />
            <input
              id="schedule-search"
              v-model="searchQuery"
              type="text"
              placeholder="Search by route name or stop"
            />
          </div>
        </div>

        <div class="filter-field">
          <label for="transport-type">Transport type</label>
          <div class="select-shell">
            <SlidersHorizontal class="input-icon" />
            <select id="transport-type" v-model="selectedTransportType">
              <option v-for="option in transportTypeOptions" :key="option" :value="option">
                {{ transportTypeLabel(option) }}
              </option>
            </select>
          </div>
        </div>

        <button class="clear-btn" @click="clearFilters">Clear filters</button>
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

      <section v-if="selectedRoute" class="selected-route-card">
        <div class="selected-route-header">
          <div>
            <p class="selected-route-kicker">Focused route</p>
            <h3>{{ selectedRoute.name }}</h3>
          </div>
          <span class="selected-route-count">{{ selectedRoute.distinctStopCount }} stops</span>
        </div>
      </section>

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
    </div>

    <nav class="bottom-nav">
      <router-link to="/home" class="nav-item"><House class="nav-icon" /></router-link>
      <router-link to="/map" class="nav-item"><Map class="nav-icon" /></router-link>
      <router-link to="/cart" class="nav-item"><ShoppingCart class="nav-icon" /></router-link>
      <router-link to="/cards" class="nav-item"><Ticket class="nav-icon" /></router-link>
      <router-link to="/profile" class="nav-item"><User class="nav-icon" /></router-link>
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
  selectedTimetable,
  filteredRoutes,
  transportTypeOptions,
  routeOptions,
  stopOptions,
  selectRoute,
  selectStop,
  loadRoutes,
  clearFilters,
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

.filters-card,
.selected-route-card,
.route-card {
  background: var(--color-surface);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  box-shadow: var(--shadow-card);
}

.filters-card {
  padding: 1rem;
  display: grid;
  gap: 0.85rem;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.filter-field label {
  font-size: 0.84rem;
  font-weight: 700;
  color: #374151;
}

.input-shell,
.select-shell {
  position: relative;
  display: flex;
  align-items: center;
}

.input-shell input,
.select-shell select {
  width: 100%;
  min-height: 3rem;
  border: 1px solid #d1d5db;
  border-radius: 14px;
  background: #ffffff;
  color: #111827;
  padding: 0.85rem 0.95rem 0.85rem 2.5rem;
  font-size: 0.95rem;
  outline: none;
}

.input-shell input:focus,
.select-shell select:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
}

.input-icon {
  position: absolute;
  left: 0.9rem;
  width: 1rem;
  height: 1rem;
  color: #6b7280;
  pointer-events: none;
}

.clear-btn {
  border: none;
  border-radius: 12px;
  background: #111827;
  color: #ffffff;
  font-weight: 700;
  padding: 0.85rem 1rem;
  cursor: pointer;
}

.selected-route-card {
  padding: 1rem;
}

.selected-route-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.selected-route-kicker {
  margin: 0 0 0.25rem 0;
  color: #667eea;
  font-size: 0.78rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.selected-route-card h3 {
  margin: 0;
  color: #111827;
}

.selected-route-count {
  flex-shrink: 0;
  border-radius: 999px;
  background: rgba(102, 126, 234, 0.1);
  color: #4f46e5;
  font-size: 0.8rem;
  font-weight: 700;
  padding: 0.35rem 0.65rem;
}

.loading-state,
.empty-state {
  padding: 1.5rem 1rem;
  text-align: center;
  color: #6b7280;
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