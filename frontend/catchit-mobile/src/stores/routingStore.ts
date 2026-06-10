import { defineStore } from 'pinia'
import { ref } from 'vue'
import { catchitApi } from '../services/api/catchitApi'
import type { RoutingPlanRequest, RoutingPlanResponse } from '../types/routing'

const MAX_CACHED_ITINERARIES = 10

function buildCacheKey(request: RoutingPlanRequest): string {
  return [
    request.fromLat.toFixed(6),
    request.fromLon.toFixed(6),
    request.toLat.toFixed(6),
    request.toLon.toFixed(6),
    request.date ?? 'nodate',
    request.time ?? 'notime',
  ].join(':')
}

export const useRoutingStore = defineStore('routing', () => {
  const itineraryCache = ref<Map<string, RoutingPlanResponse>>(new Map())
  const cacheOrder = ref<string[]>([])
  const isLoading = ref(false)
  const currentPlan = ref<RoutingPlanResponse | null>(null)
  const currentError = ref<string | null>(null)
  const lastRequest = ref<RoutingPlanRequest | null>(null)

  function rememberInCache(key: string, plan: RoutingPlanResponse) {
    if (!itineraryCache.value.has(key)) {
      cacheOrder.value.push(key)
    }
    itineraryCache.value.set(key, plan)

    while (cacheOrder.value.length > MAX_CACHED_ITINERARIES) {
      const oldest = cacheOrder.value.shift()
      if (oldest) {
        itineraryCache.value.delete(oldest)
      }
    }
  }

  function getCachedPlan(request: RoutingPlanRequest): RoutingPlanResponse | null {
    const key = buildCacheKey(request)
    return itineraryCache.value.get(key) ?? null
  }

  async function fetchPlan(request: RoutingPlanRequest): Promise<RoutingPlanResponse | null> {
    lastRequest.value = request
    const cacheKey = buildCacheKey(request)
    const cached = itineraryCache.value.get(cacheKey)

    if (cached) {
      currentPlan.value = cached
      currentError.value = cached.error ?? null
      return cached
    }

    isLoading.value = true
    currentError.value = null

    try {
      const response = await catchitApi.planRoute(request)
      if (!response.success || !response.data) {
        currentPlan.value = null
        currentError.value = response.error ?? 'Failed to plan route'
        return null
      }

      const plan = response.data
      rememberInCache(cacheKey, plan)
      currentPlan.value = plan
      currentError.value = plan.error ?? null
      return plan
    } finally {
      isLoading.value = false
    }
  }

  function clearCurrentPlan() {
    currentPlan.value = null
    currentError.value = null
    lastRequest.value = null
  }

  return {
    itineraryCache,
    cacheOrder,
    isLoading,
    currentPlan,
    currentError,
    lastRequest,
    fetchPlan,
    getCachedPlan,
    clearCurrentPlan,
  }
})
