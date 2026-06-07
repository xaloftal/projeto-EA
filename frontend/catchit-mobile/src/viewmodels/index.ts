import { ref, computed } from 'vue'
import { catchitApi } from '../services/api/catchitApi'
import type { User, Ticket, Card, CardTier, TravelCard, PaymentMethod, Stop, Vehicle, UserNotification } from '../models'
import { requestJson } from 'src/services/api/http'

const error = ref('')
const isLoading = ref(false)
const activeTrips = ref<any[]>([])

export type RouteSearchResult = {
  routeId: string
  routeName: string
  fromStop: Stop
  toStop: Stop
  departureTime: string
  arrivalTime: string
  price: number
  vehicle: Vehicle
}

type CardCartEntry = {
  id: string
  kind: 'card'
  title: string
  description: string
  quantity: number
  unitPrice: number
  totalPrice: number
  source: {
    cardId: string
    tier: CardTier
  }
}

type TicketCartEntry = {
  id: string
  kind: 'ticket'
  title: string
  description: string
  quantity: number
  unitPrice: number
  totalPrice: number
  source: {
    routeId: string
    fromStopId: string
    toStopId: string
    fromStop: string
    toStop: string
    departureTime: string
    arrivalTime: string
  }
}

export type CartEntry = CardCartEntry | TicketCartEntry

const cartItems = ref<CartEntry[]>([])
const subtotal = computed(() => cartItems.value.reduce((sum, item) => sum + item.totalPrice, 0))
const taxes = computed(() => subtotal.value * 0.1)
const total = computed(() => subtotal.value + taxes.value)

// Global state for the current user
export const currentUser = ref<User | null>(null)
const authToken = ref<string>('')
export const isAuthenticated = computed(() => !!authToken.value)

/**
 * ViewModel for Authentication
 * Handles user signup, login, and session management
 */
export function useAuthViewModel() {
  const isLoading = ref(false)
  const error = ref<string>('')

  const applySession = (user: User, token?: string) => {
    currentUser.value = user
    authToken.value = token ?? authToken.value
    localStorage.setItem('user', JSON.stringify(user))
    if (token) {
      localStorage.setItem('authToken', token)
    }
  }

  const signup = async (name: string, email: string, password: string) => {
    isLoading.value = true
    error.value = ''
    try {
      const response = await catchitApi.signup({ name, email, password })
      if (response.success && response.data) {
        applySession(response.data.user, response.data.token)
        return true
      }
      error.value = response.error || 'Signup failed'
      return false
    } finally {
      isLoading.value = false
    }
  }

  const login = async (email: string, password: string) => {
    isLoading.value = true
    error.value = ''
    try {
      const response = await catchitApi.login({ email, password })
      if (response.success && response.data) {
        applySession(response.data.user, response.data.token)
        return true
      }
      error.value = response.error || 'Login failed'
      return false
    } finally {
      isLoading.value = false
    }
  }

  const logout = async () => {
    await catchitApi.logout()
    currentUser.value = null
    authToken.value = ''
    localStorage.removeItem('authToken')
    localStorage.removeItem('user')
  }

  const restoreSession = async () => {
    const sessionResponse = await catchitApi.getCurrentSessionUser()
    if (sessionResponse.success && sessionResponse.data) {
      applySession(sessionResponse.data)
      return
    }

    currentUser.value = null
    authToken.value = ''
    localStorage.removeItem('authToken')
    localStorage.removeItem('user')
  }

  return {
    isLoading,
    error,
    signup,
    login,
    logout,
    restoreSession,
    isAuthenticated,
    currentUser,
  }
}

/**
 * ViewModel for Tickets
 * Handles ticket display, purchase, and management
 */
export function useTicketViewModel() {
  const tickets = ref<Ticket[]>([])
  const isLoading = ref(false)
  const error = ref<string>('')

  const fetchUserTickets = async () => {
    if (!currentUser.value) return
    isLoading.value = true
    error.value = ''
    try {
      const response = await catchitApi.getUserTickets(currentUser.value.id)
      if (response.success && response.data) {
        tickets.value = response.data
      }
    } finally {
      isLoading.value = false
    }
  }

  const purchaseTickets = async (tripID: string, quantity: number, price: number) => {
    if (!currentUser.value) return false
    isLoading.value = true
    try {
      const response = await catchitApi.purchaseTickets({
        userID: currentUser.value.id,
        tripID,
        quantity,
        price,
        stopFromID: '', // You would need to pass the actual stop IDs here
        stopToID: '',
      })
      if (response.success && response.data) {
        tickets.value.push(...response.data)
        return true
      }
      error.value = response.error || 'Purchase failed'
      return false
    } finally {
      isLoading.value = false
    }
  }

  const activateTicket = async (ticketId: string) => {
    isLoading.value = true
    try {
      const response = await catchitApi.activateTicket(ticketId)
      if (response.success && response.data) {
        const index = tickets.value.findIndex((t) => t.id === ticketId)
        if (index !== -1) {
          tickets.value[index] = response.data
        }
        return true
      }
      return false
    } finally {
      isLoading.value = false
    }
  }

  return {
    tickets,
    isLoading,
    error,
    fetchUserTickets,
    purchaseTickets,
    activateTicket,
  }
}

/**
 * ViewModel for Cards
 * Handles card display and purchase
 */
export function useCardViewModel() {
  const userCards = ref<Card[]>([])
  const availableCards = ref<TravelCard[]>([])
  const isLoading = ref(false)
  const error = ref<string>('')

  const fetchUserCards = async () => {
    if (!currentUser.value) return
    isLoading.value = true
    try {
      const response = await catchitApi.getUserCards(currentUser.value.id)
      if (response.success && response.data) {
        userCards.value = response.data
      }
    } finally {
      isLoading.value = false
    }
  }

  const fetchAvailableCards = async () => {
    isLoading.value = true
    try {
      const response = await catchitApi.getAvailableCards()
      if (response.success && response.data) {
        availableCards.value = response.data
      }
    } finally {
      isLoading.value = false
    }
  }

  const purchaseCard = async (cardId: string) => {
    if (!currentUser.value) return false
    isLoading.value = true
    try {
      const response = await catchitApi.purchaseCard({
        userId: currentUser.value.id,
        cardId,
      })
      if (response.success && response.data) {
        userCards.value.push(response.data)
        return true
      }
      error.value = response.error || 'Purchase failed'
      return false
    } finally {
      isLoading.value = false
    }
  }

  return {
    userCards,
    availableCards,
    isLoading,
    error,
    fetchUserCards,
    fetchAvailableCards,
    purchaseCard,
  }
}

/**
 * ViewModel for Search & Travel
 * Handles route search and travel booking
 */
export function useTravelViewModel() {
  const searchResults = ref<RouteSearchResult[]>([])
  const isLoading = ref(false)
  const error = ref<string>('')

  const searchRoutes = async (
    fromStopId: string,
    toStopId: string,
    departureDate: string
  ) => {
    isLoading.value = true
    error.value = ''
    try {
      const response = await catchitApi.searchRoutes({
        fromStopId,
        toStopId,
        departureDate,
      })
      if (response.success && response.data) {
        searchResults.value = response.data
        return response.data
      }
      error.value = response.error || 'Search failed'
      return []
    } finally {
      isLoading.value = false
    }
  }

  const bookTravel = async (routeId: string, tripId: string) => {
    if (!currentUser.value) return null
    isLoading.value = true
    try {
      const response = await catchitApi.bookTravel({
        userId: currentUser.value.id,
        routeId,
        tripId,
      })
      if (response.success && response.data) {
        return response.data
      }
      return null
    } finally {
      isLoading.value = false
    }
  }

  return {
    searchResults,
    isLoading,
    error,
    searchRoutes,
    bookTravel,
  }
}

/**
 * ViewModel for Transport Check-In / Check-Out
 * Encapsulates API interactions and state for the TransportCheckIn view
 */
export function useTransportViewModel(titleId?: string) {
  type TripOption = { id: string; routeName: string; startTime?: string; stopIds?: string[]; zoneName?: string }

  const activeTrips = ref<TripOption[]>([])
  const selectedTripId = ref('')
  const isLoadingTrips = ref(false)
  const isCheckingIn = ref(false)
  const isCheckingOut = ref(false)
  const checkInSuccess = ref(false)
  const errorMessage = ref('')
  const checkOutMessage = ref<{ success: boolean; text: string } | null>(null)
  const titleLabel = ref('Loading...')
  const ticketFromStop = ref<Stop | null>(null)
  const ticketToStop = ref<Stop | null>(null)
  const isTicketTitle = ref(false)

  const formatTime = (date?: string) => (date ? new Date(date).toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit' }) : '')

  const loadTitleInfo = async (id?: string) => {
    if (!currentUser.value) return
    const localId = id ?? titleId
    if (!localId) return

    ticketFromStop.value = null
    ticketToStop.value = null
    isTicketTitle.value = false

    const ticketsResponse = await catchitApi.getUserTickets(currentUser.value.id)
    const ticket = ticketsResponse.data?.find((t) => t.id === localId)
    if (ticket) {
      titleLabel.value = `🎟️ Ticket: ${ticket.stopFrom?.name ?? '?'} → ${ticket.stopTo?.name ?? '?'}`
      ticketFromStop.value = ticket.stopFrom ?? null
      ticketToStop.value = ticket.stopTo ?? null
      isTicketTitle.value = true
      return
    }

    const cardsResponse = await catchitApi.getUserCards(currentUser.value.id)
    const card = cardsResponse.data?.find((c) => c.id === localId)
    if (card) {
      titleLabel.value = `🎫 Card: ${card.name}`
      return
    }

    titleLabel.value = 'Title not found'
  }

  const loadActiveTrips = async () => {
}

  const handleCheckIn = async () => {
    if (!selectedTripId.value) return
    isCheckingIn.value = true
    errorMessage.value = ''
    try {
      const response = await catchitApi.checkIn({ titleId: titleId ?? '', tripId: selectedTripId.value })
      if (response.success && response.data?.success) {
        checkInSuccess.value = true
      } else {
        errorMessage.value = response.data?.message ?? response.error ?? 'Check in failed'
      }
    } finally {
      isCheckingIn.value = false
    }
  }

  const handleCheckOut = async () => {
    isCheckingOut.value = true
    checkOutMessage.value = null
    try {
      const response = await catchitApi.checkoutTransport({ titleId: titleId ?? '', tripId: selectedTripId.value })
      if (response.success && response.data) {
        checkOutMessage.value = { success: response.data.success, text: response.data.message }
      }
    } finally {
      isCheckingOut.value = false
    }
  }

  return {
    activeTrips,
    selectedTripId,
    isLoadingTrips,
    isCheckingIn,
    isCheckingOut,
    checkInSuccess,
    errorMessage,
    checkOutMessage,
    titleLabel,
    ticketFromStop,
    ticketToStop,
    isTicketTitle,
    loadTitleInfo,
    loadActiveTrips,
    handleCheckIn,
    handleCheckOut,
  }
}

/**
 * ViewModel for Checkout
 * Handles shopping cart and payment process
 */
export function useCheckoutViewModel() {
  const isLoading = ref(false)
  const error = ref<string>('')
  const paymentMethods = ref<PaymentMethod[]>([])

  const applyBackendCart = (response: {
    items: Array<{
      id: string
      kind: 'card' | 'ticket'
      title: string
      description: string
      quantity: number
      unitPrice: number
      totalPrice: number
      source: Record<string, unknown>
    }>
  }) => {
    cartItems.value = response.items as CartEntry[]
  }

  const fetchCart = async () => {
    if (!currentUser.value) return
    const response = await catchitApi.getCart()
    if (response.success && response.data) {
      applyBackendCart(response.data)
      return
    }

    error.value = response.error || 'Unable to load cart'
  }

  const addCardToCart = async (item: TravelCard | Card, quantity: number = 1) => {
    if (!currentUser.value) return

    await fetchCart()
    const existing = cartItems.value.find((entry) => entry.kind === 'card' && entry.source.cardId === item.id)
    const resolvedQuantity = existing && existing.kind === 'card' ? Math.max(existing.quantity, quantity) : quantity

    const payload: CartEntry = {
      id: `card_${item.id}`,
      kind: 'card',
      title: item.name,
      description: item.description || 'Travel card',
      quantity: resolvedQuantity,
      unitPrice: item.price,
      totalPrice: item.price * resolvedQuantity,
      source: {
        cardId: item.id,
        tier: item.tier || 'weekly',
      },
    }

    const response = await catchitApi.upsertCartItem(payload)
    if (response.success && response.data) {
      applyBackendCart(response.data)
      return
    }

    error.value = response.error || 'Unable to add card to cart'
  }

  const addTicketToCart = async (route: RouteSearchResult, quantity: number = 1) => {
    if (!currentUser.value) return

    await fetchCart()
    const existing = cartItems.value.find(
      (entry) => entry.kind === 'ticket' && entry.source.routeId === route.routeId
    )
    const resolvedQuantity = existing && existing.kind === 'ticket' ? existing.quantity + quantity : quantity

    const payload: CartEntry = {
      id: `ticket_${route.routeId}`,
      kind: 'ticket',
      title: `${route.fromStop.name} → ${route.toStop.name}`,
      description: `${route.departureTime} - ${route.arrivalTime}`,
      quantity: resolvedQuantity,
      unitPrice: route.price,
      totalPrice: route.price * resolvedQuantity,
      source: {
        routeId: route.routeId,
        fromStopId: route.fromStop.id,
        toStopId: route.toStop.id,
        fromStop: route.fromStop.name,
        toStop: route.toStop.name,
        departureTime: route.departureTime,
        arrivalTime: route.arrivalTime,
      },
    }

    const response = await catchitApi.upsertCartItem(payload)
    if (response.success && response.data) {
      applyBackendCart(response.data)
      return
    }

    error.value = response.error || 'Unable to add ticket to cart'
  }

  const removeFromCart = async (itemId: string) => {
    if (!currentUser.value) return

    const response = await catchitApi.removeCartItem(itemId)
    if (response.success && response.data) {
      applyBackendCart(response.data)
      return
    }

    error.value = response.error || 'Unable to remove item from cart'
  }

  const clearCart = async () => {
    if (!currentUser.value) return

    const response = await catchitApi.clearCart()
    if (response.success && response.data) {
      applyBackendCart(response.data)
      return
    }

    error.value = response.error || 'Unable to clear cart'
  }

  const fetchPaymentMethods = async () => {
    if (!currentUser.value) return
    isLoading.value = true
    try {
      const response = await catchitApi.getPaymentMethods(currentUser.value.id)
      if (response.success && response.data) {
        paymentMethods.value = response.data
      }
    } finally {
      isLoading.value = false
    }
  }

  const createCheckoutSession = async () => {
    if (!currentUser.value) return null
    isLoading.value = true
    try {
      await fetchCart()

      const response = await catchitApi.createCheckoutSession()
      if (response.success && response.data) {
        return response.data.sessionId
      }
      return null
    } finally {
      isLoading.value = false
    }
  }

  // Altera a assinatura para aceitar dois parâmetros: paymentMethodId e sessionId
  const confirmCheckout = async (paymentMethodId: string, sessionId: string) => {
  if (!currentUser.value) {
    error.value = 'No authenticated user found'
    return null
  }

  isLoading.value = true
  error.value = ''

  try {
    // Faz o POST para o teu CheckoutController do Backend
    const response = await requestJson<{ orderId: string }>('/api/checkout/confirm', {
      method: 'POST',
      body: JSON.stringify({
        paymentMethodId: paymentMethodId,
        sessionId: sessionId, // <-- PASSAMOS AGORA O SESSÃO ID DO REDIS DAQUI
      }),
    })

    if (response.success && response.data) {
      // Limpa o carrinho local se o pagamento correu bem
      cartItems.value = []
      return response.data
    }

    error.value = response.error || 'Payment confirmation failed'
    return null
  } catch (err: any) {
    error.value = err.message || 'An unexpected error occurred during confirmation'
    return null
  } finally {
    isLoading.value = false
  }
}

  return {
    cartItems,
    subtotal,
    taxes,
    total,
    paymentMethods,
    isLoading,
    error,
    addCardToCart,
    addTicketToCart,
    removeFromCart,
    clearCart,
    fetchCart,
    fetchPaymentMethods,
    createCheckoutSession,
    confirmCheckout,
  }
}

/**
 * ViewModel for User Profile
 * Handles profile display and editing
 */
export function useProfileViewModel() {
  const isLoading = ref(false)
  const error = ref<string>('')

  const updateProfile = async (updates: Partial<User>) => {
    if (!currentUser.value) return false
    isLoading.value = true
    error.value = ''
    try {
      const response = await catchitApi.updateUserProfile(currentUser.value.id, updates)
      if (response.success && response.data) {
        currentUser.value = response.data
        localStorage.setItem('user', JSON.stringify(response.data))
        return true
      }
      error.value = response.error || 'Update failed'
      return false
    } finally {
      isLoading.value = false
    }
  }

  return {
    isLoading,
    error,
    updateProfile,
    currentUser,
  }
}

/**
 * ViewModel for Notifications
 * Handles loading and refreshing notification items for the current user
 */
export function useNotificationViewModel() {
  const notifications = ref<UserNotification[]>([])
  const isLoading = ref(false)
  const error = ref<string>('')
  const deletingIds = ref<string[]>([])

  const fetchNotifications = async () => {
    if (!currentUser.value) {
      notifications.value = []
      return
    }

    isLoading.value = true
    error.value = ''
    try {
      const response = await catchitApi.getUserNotifications(currentUser.value.id)
      if (response.success && response.data) {
        notifications.value = [...response.data].sort(
          (left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime(),
        )
      } else {
        error.value = response.error || 'Unable to load notifications'
      }
    } finally {
      isLoading.value = false
    }
  }

  const deleteNotification = async (notificationId: string) => {
    if (!currentUser.value || deletingIds.value.includes(notificationId)) {
      return false
    }

    deletingIds.value = [...deletingIds.value, notificationId]
    error.value = ''

    try {
      const response = await catchitApi.deleteUserNotification(currentUser.value.id, notificationId)
      if (!response.success) {
        error.value = response.error || 'Unable to delete notification'
        return false
      }

      notifications.value = notifications.value.filter((notification) => notification.id !== notificationId)
      return true
    } finally {
      deletingIds.value = deletingIds.value.filter((id) => id !== notificationId)
    }
  }

  return {
    notifications,
    isLoading,
    error,
    deletingIds,
    fetchNotifications,
    deleteNotification,
  }
}


export { useScheduleViewModel } from './scheduleViewModel';