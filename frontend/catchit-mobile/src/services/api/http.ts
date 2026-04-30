const viteEnv = (import.meta as ImportMeta & { env?: Record<string, string | undefined> }).env ?? {}
const apiBaseUrl = (viteEnv.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: string
}

export const requestJson = async <T>(
  path: string,
  init: RequestInit = {}
): Promise<ApiResponse<T>> => {
  try {
    const authToken = localStorage.getItem('authToken')
    const requestUrl = `${apiBaseUrl}${path}`
    const response = await fetch(requestUrl, {
      ...init,
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
        ...(init.headers ?? {}),
      },
    })

    const contentType = response.headers.get('content-type') ?? ''
    const rawBody = await response.text()
    const body = rawBody && contentType.includes('application/json') ? JSON.parse(rawBody) : rawBody

    if (!response.ok) {
      const message =
        (body && typeof body === 'object' && 'message' in body && String((body as { message?: string }).message)) ||
        (typeof body === 'string' && body.trim()) ||
        response.statusText ||
        'Request failed'

      return { success: false, error: message }
    }

    return { success: true, data: body as T }
  } catch (error) {
    return {
      success: false,
      error: error instanceof Error ? error.message : 'Network request failed',
    }
  }
}
