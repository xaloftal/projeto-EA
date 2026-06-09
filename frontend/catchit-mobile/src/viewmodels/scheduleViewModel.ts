import { computed, ref } from 'vue'
import { catchitApi, type RouteScheduleDTO, type RouteSummaryDTO } from '../services/api/catchitApi'

export type ScheduleStopOption = {
  stopId: string
  stopName: string
  stopCode?: string | null
  stopType?: string | null
}

export type ScheduleRoute = {
  id: string
  name: string
  stops: ScheduleStopOption[]
  distinctStopCount: number
}

export type RouteTimetableRow = {
  code: string
  times: string[]
}

export type StopTimetableRow = {
  code: string
  times: string[]
}

type SelectedTimetable =
  | {
    kind: 'route'
    code: string
    label: string
    rows: RouteTimetableRow[]
    distinctCount: number
  }
  | {
    kind: 'stop'
    code: string
    label: string
    rows: StopTimetableRow[]
    distinctCount: number
  }
  | null

const normalizeText = (value: string) =>
  value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim()

const parseTimeValue = (value?: string) => {
  if (!value) return ''
  const normalized = value.includes('T') ? value.split('T')[1] : value
  return normalized.slice(0, 5) || ''
}

const getScheduleTime = (schedule: any) => schedule.departureTime ?? schedule.arrivalTime ?? ''

const buildRouteTimetableFromDTO = (dto: RouteScheduleDTO): SelectedTimetable => {
  const sortedSchedules = [...(dto.schedules || [])].sort((a, b) => (a.sequence ?? 0) - (b.sequence ?? 0));
  const stopMap = new Map<string, { stopName: string; stopCode?: string | null; times: string[] }>();
  
  sortedSchedules.forEach(s => {
    if (!stopMap.has(s.stopId)) {
      stopMap.set(s.stopId, { stopName: s.stopName, stopCode: s.stopCode ?? null, times: [] });
    }
    const time = getScheduleTime(s);
    if (time) stopMap.get(s.stopId)!.times.push(parseTimeValue(time));
  });

  return {
    kind: 'route',
    code: dto.name || 'Unknown Route',
    label: dto.name || 'Unknown Route',
    distinctCount: stopMap.size,
    rows: Array.from(stopMap.values()).map((stopInfo) => ({
      code: stopInfo.stopCode ? `${stopInfo.stopCode}` : stopInfo.stopName,
      times: stopInfo.times.sort()
    })),
  };
}

const buildStopTimetableFromDTO = (stopId: string, stopName: string, stopCode: string | null | undefined, dtos: RouteScheduleDTO[]): SelectedTimetable => {
  const rows = dtos.map(dto => {
    const times = (dto.schedules || [])
      .filter(s => s.stopId === stopId)
      .map(s => parseTimeValue(getScheduleTime(s)))
      .sort();

    return times.length ? { code: dto.name || 'Unknown Route', times } : null;
  }).filter(Boolean) as StopTimetableRow[];

  const displayLabel = stopCode ? `${stopCode} - ${stopName}` : stopName;

  return {
    kind: 'stop',
    code: displayLabel,
    label: displayLabel,
    distinctCount: rows.length,
    rows
  };
}

export function useScheduleViewModel(initialRouteId = '') {
  const routes = ref<ScheduleRoute[]>([])
  const allStops = ref<ScheduleStopOption[]>([])
  const isLoading = ref(false)
  const isTimetableLoading = ref(false)
  const error = ref('')
  
  const searchQuery = ref('')
  const selectedTransportType = ref('ALL')
  const selectedRouteId = ref(initialRouteId)
  const selectedStopId = ref('')
  
  const selectedTimetable = ref<SelectedTimetable>(null)

  const filteredRoutes = computed(() => {
    const query = normalizeText(searchQuery.value)
    const selectedType = selectedTransportType.value.toUpperCase()

    return routes.value.filter((route) => {
      if (selectedType !== 'ALL') {
        const hasType = route.stops.some(stop => (stop.stopType ?? '').toUpperCase() === selectedType)
        if (!hasType) return false
      }

      if (!query) return true

      return (
        normalizeText(route.id).includes(query) ||
        normalizeText(route.name).includes(query) ||
        route.stops.some(stop =>
          normalizeText(stop.stopId).includes(query) ||
          normalizeText(stop.stopName).includes(query) ||
          normalizeText(stop.stopCode ?? '').includes(query) ||
          normalizeText(stop.stopType ?? '').includes(query)
        )
      )
    })
  })

  const filteredStops = computed(() => {
    const query = normalizeText(searchQuery.value)
    const selectedType = selectedTransportType.value.toUpperCase()

    return allStops.value.filter((stop) => {
      if (selectedType !== 'ALL' && (stop.stopType ?? '').toUpperCase() !== selectedType) {
        return false
      }

      if (!query) return true

      return (
        normalizeText(stop.stopId).includes(query) ||
        normalizeText(stop.stopName).includes(query) ||
        normalizeText(stop.stopCode ?? '').includes(query) ||
        normalizeText(stop.stopType ?? '').includes(query)
      )
    })
  })

  const routeOptions = computed(() => {
    const query = normalizeText(searchQuery.value)
    let list = [...filteredRoutes.value]
    if (query) {
      list.sort((a, b) => {
        const aExact = normalizeText(a.name) === query ? -1 : 0
        const bExact = normalizeText(b.name) === query ? -1 : 0
        if (aExact !== bExact) return aExact - bExact
        
        const aStart = normalizeText(a.name).startsWith(query) ? -1 : 0
        const bStart = normalizeText(b.name).startsWith(query) ? -1 : 0
        if (aStart !== bStart) return aStart - bStart

        return a.name.localeCompare(b.name)
      })
    }
    return list.slice(0, 12)
  })

  const stopOptions = computed(() => {
    const query = normalizeText(searchQuery.value)
    let list = [...filteredStops.value]
    if (query) {
      list.sort((a, b) => {
        const aExactCode = normalizeText(a.stopCode || '') === query ? -1 : 0
        const bExactCode = normalizeText(b.stopCode || '') === query ? -1 : 0
        if (aExactCode !== bExactCode) return aExactCode - bExactCode

        const aExactName = normalizeText(a.stopName) === query ? -1 : 0
        const bExactName = normalizeText(b.stopName) === query ? -1 : 0
        if (aExactName !== bExactName) return aExactName - bExactName

        return a.stopName.localeCompare(b.stopName)
      })
    }
    return list.slice(0, 12)
  })

  const transportTypeOptions = computed(() => {
    const types = new Set<string>()
    for (const stop of allStops.value) {
      if (stop.stopType) types.add(stop.stopType.toUpperCase())
    }
    return ['ALL', ...Array.from(types).sort()]
  })

  const selectedRoute = computed(() => routes.value.find((route) => route.id === selectedRouteId.value) ?? null)
  const selectedStop = computed(() => allStops.value.find((stop) => stop.stopId === selectedStopId.value) ?? null)

  const loadRouteTimetable = async (routeId: string) => {
    if (!routeId) return
    isTimetableLoading.value = true
    error.value = ''
    selectedTimetable.value = null
    try {
      const response = await catchitApi.getRouteSchedule(routeId)
      if (response.success && response.data) {
        selectedTimetable.value = buildRouteTimetableFromDTO(response.data)
      } else {
        error.value = response.error || 'Failed to load route schedule'
      }
    } catch (err) {
      error.value = 'Failed to load route schedule'
    } finally {
      isTimetableLoading.value = false
    }
  }

  const loadStopTimetable = async (stopId: string) => {
    if (!stopId) return
    const stop = allStops.value.find(s => s.stopId === stopId)
    if (!stop) return
    
    isTimetableLoading.value = true
    error.value = ''
    selectedTimetable.value = null
    try {
      const response = await catchitApi.getStopSchedule(stopId)
      if (response.success && response.data) {
        selectedTimetable.value = buildStopTimetableFromDTO(stopId, stop.stopName, stop.stopCode, response.data)
      } else {
        error.value = response.error || 'Failed to load stop schedule'
      }
    } catch (err) {
      error.value = 'Failed to load stop schedule'
    } finally {
      isTimetableLoading.value = false
    }
  }

  const selectRoute = (id: string) => {
    if (selectedRouteId.value === id) return
    selectedRouteId.value = id
    selectedStopId.value = ''
    loadRouteTimetable(id)
  }

  const selectStop = (id: string) => {
    if (selectedStopId.value === id) return
    selectedStopId.value = id
    selectedRouteId.value = ''
    loadStopTimetable(id)
  }

  const clearSelection = () => {
    selectedRouteId.value = ''
    selectedStopId.value = ''
    selectedTimetable.value = null
  }

  const clearFilters = () => {
    searchQuery.value = ''
    selectedTransportType.value = 'ALL'
  }

  const transportTypeLabel = (option: string) => {
    if (option === 'ALL') return 'All Transports'
    return option.charAt(0) + option.slice(1).toLowerCase()
  }

  const loadData = async () => {
    isLoading.value = true
    error.value = ''

    try {
      const response = await catchitApi.getRouteSummaries()
      if (response.success && response.data) {
        routes.value = response.data.map(route => ({
          id: route.id,
          name: route.name?.trim() || 'Unnamed route',
          stops: route.stops,
          distinctStopCount: route.stops.length
        })).sort((a, b) => a.name.localeCompare(b.name))
        
        const stopMap = new Map<string, ScheduleStopOption>()
        for (const route of routes.value) {
          for (const stop of route.stops) {
            if (!stopMap.has(stop.stopId)) {
              stopMap.set(stop.stopId, stop)
            }
          }
        }
        allStops.value = Array.from(stopMap.values())

        if (initialRouteId) {
          selectRoute(initialRouteId)
        }
      } else {
        error.value = response.error || 'Failed to load routes'
      }
    } catch (err: any) {
      error.value = err.message || 'An error occurred'
    } finally {
      isLoading.value = false
    }
  }

  return {
    isLoading,
    isTimetableLoading,
    error,
    searchQuery,
    selectedTransportType,
    transportTypeOptions,
    routeOptions,
    stopOptions,
    selectedRoute,
    selectedStop,
    selectedTimetable,
    selectRoute,
    selectStop,
    clearSelection,
    clearFilters,
    transportTypeLabel,
    loadData,
  }
}
