import { computed, ref } from 'vue'
import { catchitApi, type RouteScheduleDTO } from '../services/api/catchitApi'

export type ScheduleStopEntry = {
  stopId: string
  stopName: string
  stopType?: string | null
  latitude: number
  longitude: number
  arrivalTime?: string
  departureTime?: string
  sequence?: number
}

export type ScheduleRoute = {
  id: string
  name: string
  schedules: ScheduleStopEntry[]
  distinctStopCount: number
}

export type ScheduleStopOption = {
  stopId: string
  stopName: string
  stopType?: string | null
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

const formatTime = (value?: string) => parseTimeValue(value) || '--:--'

const formatTransportType = (value?: string | null) => {
  if (!value) return 'Unknown'
  return value
    .toLowerCase()
    .split(/[_\s-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}

const toScheduleEntry = (schedule: NonNullable<RouteScheduleDTO['schedules']>[number]): ScheduleStopEntry => ({
  stopId: schedule.stopId,
  stopName: schedule.stopName,
  stopType: schedule.stopType ?? null,
  latitude: schedule.latitude,
  longitude: schedule.longitude,
  arrivalTime: schedule.arrivalTime,
  departureTime: schedule.departureTime,
  sequence: schedule.sequence,
})

const getScheduleTime = (schedule: ScheduleStopEntry) => schedule.departureTime ?? schedule.arrivalTime ?? ''

const collectDistinctStops = (routes: ScheduleRoute[]) => {
  const stops: ScheduleStopOption[] = []
  const seenStops = new Set<string>()

  for (const route of routes) {
    for (const schedule of route.schedules) {
      if (seenStops.has(schedule.stopId)) {
        continue
      }

      seenStops.add(schedule.stopId)
      stops.push({
        stopId: schedule.stopId,
        stopName: schedule.stopName,
        stopType: schedule.stopType ?? null,
      })
    }
  }

  return stops
}

const toRouteSchedules = (routes: RouteScheduleDTO[]): ScheduleRoute[] =>
  routes
    .map((route) => {
      const schedules = (route.schedules ?? [])
        .map(toScheduleEntry)
        .sort((left, right) => {
          const sequenceDelta = (left.sequence ?? 0) - (right.sequence ?? 0)
          if (sequenceDelta !== 0) return sequenceDelta

          return parseTimeValue(getScheduleTime(left)).localeCompare(parseTimeValue(getScheduleTime(right)))
        })

      return {
        id: route.id,
        name: route.name?.trim() || 'Unnamed route',
        schedules,
        distinctStopCount: new Set(schedules.map((schedule) => schedule.stopId)).size,
      }
    })
    .filter((route) => route.schedules.length > 0)
    .sort((left, right) => left.name.localeCompare(right.name))

const collectUniqueTimes = (schedules: ScheduleStopEntry[]) => {
  const times = new Set<string>()

  for (const schedule of schedules) {
    const time = getScheduleTime(schedule)
    if (time) {
      times.add(parseTimeValue(time))
    }
  }

  return Array.from(times)
    .filter(Boolean)
    .sort((left, right) => left.localeCompare(right))
}

const buildRouteTimetable = (route: ScheduleRoute): SelectedTimetable => {
  // Sort schedules by sequence first
  const sortedSchedules = [...route.schedules].sort((a, b) => (a.sequence ?? 0) - (b.sequence ?? 0));
  
  const stopMap = new Map<string, string[]>();
  sortedSchedules.forEach(s => {
    if(!stopMap.has(s.stopId)) stopMap.set(s.stopId, []);
    const time = getScheduleTime(s);
    if(time) stopMap.get(s.stopId)!.push(parseTimeValue(time));
  });

  return {
    kind: 'route',
    code: route.name,
    label: route.name,
    distinctCount: route.distinctStopCount,
    rows: Array.from(stopMap.entries()).map(([stopId, times]) => ({
      code: stopId, // This is the 1st column
      times: times.sort() // ASC Sort
    })),
  };
}

const buildStopTimetable = (stop: ScheduleStopOption, routes: ScheduleRoute[]): SelectedTimetable => {
  const rows = routes
    .map(route => {
      const times = route.schedules
        .filter(s => s.stopId === stop.stopId)
        .map(s => parseTimeValue(getScheduleTime(s)))
        .sort(); // ASC Sort

      return times.length ? { code: route.id, times } : null;
    })
    .filter(Boolean);

  return {
    kind: 'stop',
    code: stop.stopName,
    label: stop.stopName,
    distinctCount: rows.length,
    rows: rows as any
  };
}

export function useScheduleViewModel(initialRouteId = '') {
  const routes = ref<ScheduleRoute[]>([])
  const isLoading = ref(false)
  const error = ref('')
  const searchQuery = ref('')
  const selectedTransportType = ref('ALL')
  const selectedRouteId = ref(initialRouteId)
  const selectedStopId = ref('')

  const allStops = computed(() => collectDistinctStops(routes.value))

  const filteredRoutes = computed(() => {
    const query = normalizeText(searchQuery.value)
    const selectedType = selectedTransportType.value.toUpperCase()

    return routes.value.filter((route) => {
      if (selectedType !== 'ALL') {
        const hasType = route.schedules.some(
          (schedule) => (schedule.stopType ?? '').toUpperCase() === selectedType,
        )

        if (!hasType) {
          return false
        }
      }

      if (!query) {
        return true
      }

      return (
        normalizeText(route.id).includes(query) ||
        normalizeText(route.name).includes(query) ||
        route.schedules.some(
          (schedule) =>
            normalizeText(schedule.stopId).includes(query) ||
            normalizeText(schedule.stopName).includes(query) ||
            normalizeText(schedule.stopType ?? '').includes(query),
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

      if (!query) {
        return true
      }

      return (
        normalizeText(stop.stopId).includes(query) ||
        normalizeText(stop.stopName).includes(query) ||
        normalizeText(stop.stopType ?? '').includes(query)
      )
    })
  })

  const routeOptions = computed(() => filteredRoutes.value.slice(0, 12))
  const stopOptions = computed(() => filteredStops.value.slice(0, 12))

  const transportTypeOptions = computed(() => {
    const types = new Set<string>()

    for (const route of routes.value) {
      for (const schedule of route.schedules) {
        if (schedule.stopType) {
          types.add(schedule.stopType.toUpperCase())
        }
      }
    }

    return ['ALL', ...Array.from(types).sort()]
  })

  const selectedRoute = computed(() => routes.value.find((route) => route.id === selectedRouteId.value) ?? null)
  const selectedStop = computed(() => allStops.value.find((stop) => stop.stopId === selectedStopId.value) ?? null)

  const selectedTimetable = computed(() => {
    if (selectedRoute.value) {
      return buildRouteTimetable(selectedRoute.value)
    }

    if (selectedStop.value) {
      return buildStopTimetable(selectedStop.value, routes.value)
    }

    return null
  })

  const loadRoutes = async () => {
    isLoading.value = true
    error.value = ''

    try {
      const response = await catchitApi.getRouteSchedules()
      if (response.success && response.data) {
        routes.value = toRouteSchedules(response.data)
        return
      }

      routes.value = []
      error.value = response.error || 'Unable to load route schedules'
    } finally {
      isLoading.value = false
    }
  }

  const selectRoute = (routeId: string) => {
    selectedRouteId.value = routeId
    selectedStopId.value = ''
  }

  const selectStop = (stopId: string) => {
    selectedStopId.value = stopId
    selectedRouteId.value = ''
  }

  const clearSelection = () => {
    selectedRouteId.value = ''
    selectedStopId.value = ''
  }

  const clearFilters = () => {
    searchQuery.value = ''
    selectedTransportType.value = 'ALL'
    clearSelection()
  }

  const transportTypeLabel = (value: string) => (value === 'ALL' ? 'All' : formatTransportType(value))

  return {
    routes,
    isLoading,
    error,
    searchQuery,
    selectedTransportType,
    selectedRouteId,
    selectedStopId,
    selectedRoute,
    selectedStop,
    selectedTimetable,
    transportTypeOptions,
    routeOptions,
    stopOptions,
    filteredRoutes, 
    loadRoutes,
    selectRoute,
    selectStop,
    clearSelection,
    clearFilters,
    formatTime,
    transportTypeLabel,
  }
}

