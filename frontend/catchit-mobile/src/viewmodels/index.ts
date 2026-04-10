import { ref, computed } from 'vue'
import { mockAPI } from '../services/api/mockAPI'
import type { User, Ticket, Card, CardTier, TravelCard, PaymentMethod, Stop, Vehicle } from '../models'

export type RouteSearchResult = {
  routeId: string
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

  const signup = async (name: string, email: string, password: string) => {
    isLoading.value = true
    error.value = ''
    try {
      const response = await mockAPI.signup({ name, email, password })
      if (response.success && response.data) {
        currentUser.value = response.data.user
        authToken.value = response.data.token
        localStorage.setItem('authToken', response.data.token)
        localStorage.setItem('user', JSON.stringify(response.data.user))
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
      const response = await mockAPI.login({ email, password })
      if (response.success && response.data) {
        currentUser.value = response.data.user
        authToken.value = response.data.token
        localStorage.setItem('authToken', response.data.token)
        localStorage.setItem('user', JSON.stringify(response.data.user))
        return true
      }
      error.value = response.error || 'Login failed'
      return false
    } finally {
      isLoading.value = false
    }
  }

  const logout = () => {
    currentUser.value = null
    authToken.value = ''
    localStorage.removeItem('authToken')
    localStorage.removeItem('user')
  }

  const restoreSession = () => {
    const token = localStorage.getItem('authToken')
    const user = localStorage.getItem('user')
    if (token && user) {
      authToken.value = token
      currentUser.value = JSON.parse(user)
    }
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
      const response = await mockAPI.getUserTickets(currentUser.value.id)
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
      const response = await mockAPI.purchaseTickets({
        userID: currentUser.value.id,
        tripID,
        quantity,
        price,
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
      const response = await mockAPI.activateTicket(ticketId)
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
      const response = await mockAPI.getUserCards(currentUser.value.id)
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
      const response = await mockAPI.getAvailableCards()
      if (response.success && response.data) {
        availableCards.value = response.data
      }
    } finally {
      isLoading.value = false
    }
  }

  const purchaseCard = async (cardId: string, tier: CardTier) => {
    if (!currentUser.value) return false
    isLoading.value = true
    try {
      const response = await mockAPI.purchaseCard({
        userId: currentUser.value.id,
        cardId,
        tier,
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
      const response = await mockAPI.searchRoutes({
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
      const response = await mockAPI.bookTravel({
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
 * ViewModel for Checkout
 * Handles shopping cart and payment process
 */
export function useCheckoutViewModel() {
  const isLoading = ref(false)
  const error = ref<string>('')
  const paymentMethods = ref<PaymentMethod[]>([])

  const addCardToCart = (item: TravelCard | Card, quantity: number = 1) => {
    const existing = cartItems.value.find((entry) => entry.kind === 'card' && entry.source.cardId === item.id)

    if (existing && existing.kind === 'card') {
      existing.quantity = Math.max(existing.quantity, quantity)
      existing.unitPrice = item.price
      existing.totalPrice = existing.unitPrice * existing.quantity
      return
    }

    cartItems.value.push({
      id: `card_${item.id}`,
      kind: 'card',
      title: item.name,
      description: item.description || 'Travel card',
      quantity,
      unitPrice: item.price,
      totalPrice: item.price * quantity,
      source: {
        cardId: item.id,
        tier: item.tier || 'weekly',
      },
    })
  }

  const addTicketToCart = (route: RouteSearchResult, quantity: number = 1) => {
    const existing = cartItems.value.find(
      (entry) => entry.kind === 'ticket' && entry.source.routeId === route.routeId
    )

    if (existing && existing.kind === 'ticket') {
      existing.quantity += quantity
      existing.totalPrice = existing.unitPrice * existing.quantity
      return
    }

    cartItems.value.push({
      id: `ticket_${route.routeId}`,
      kind: 'ticket',
      title: `${route.fromStop.name} → ${route.toStop.name}`,
      description: `${route.departureTime} - ${route.arrivalTime}`,
      quantity,
      unitPrice: route.price,
      totalPrice: route.price * quantity,
      source: {
        routeId: route.routeId,
        fromStop: route.fromStop.name,
        toStop: route.toStop.name,
        departureTime: route.departureTime,
        arrivalTime: route.arrivalTime,
      },
    })
  }

  const removeFromCart = (itemId: string) => {
    cartItems.value = cartItems.value.filter((item) => item.id !== itemId)
  }

  const clearCart = () => {
    cartItems.value = []
  }

  const fetchPaymentMethods = async () => {
    if (!currentUser.value) return
    isLoading.value = true
    try {
      const response = await mockAPI.getPaymentMethods(currentUser.value.id)
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
      const response = await mockAPI.createCheckoutSession({
        userId: currentUser.value.id,
        items: cartItems.value.map((item) => ({
          type: item.kind,
          itemId: item.kind === 'card' ? item.source.cardId : item.source.routeId,
          quantity: item.quantity,
        })),
      })
      if (response.success && response.data) {
        return response.data.sessionId
      }
      return null
    } finally {
      isLoading.value = false
    }
  }

  const confirmCheckout = async (paymentMethodId: string) => {
    if (!currentUser.value) return null
    if (!cartItems.value.length) {
      error.value = 'Cart is empty'
      return null
    }

    isLoading.value = true
    error.value = ''
    try {
      const sessionId = await createCheckoutSession()
      if (!sessionId) {
        error.value = 'Unable to create checkout session'
        return null
      }

      const response = await mockAPI.confirmCheckout({
        sessionId,
        paymentMethodId,
      })
      if (response.success && response.data) {
        for (const item of cartItems.value) {
          if (item.kind === 'card') {
            const cardResponse = await mockAPI.purchaseCard({
              userId: currentUser.value.id,
              cardId: item.source.cardId,
              tier: item.source.tier,
            })

            if (!cardResponse.success) {
              error.value = cardResponse.error || 'Card purchase failed'
              return null
            }
          } else {
            const tripId = `trip_${Date.now()}_${item.id}`
            const tripResponse = await mockAPI.bookTravel({
              userId: currentUser.value.id,
              routeId: item.source.routeId,
              tripId,
            })

            if (!tripResponse.success || !tripResponse.data) {
              error.value = tripResponse.error || 'Trip booking failed'
              return null
            }

            const ticketResponse = await mockAPI.purchaseTickets({
              userID: currentUser.value.id,
              tripID: tripResponse.data.id,
              quantity: item.quantity,
              price: item.unitPrice,
            })

            if (!ticketResponse.success) {
              error.value = ticketResponse.error || 'Ticket purchase failed'
              return null
            }
          }
        }

        clearCart()
        return response.data
      }
      error.value = response.error || 'Checkout failed'
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
      const response = await mockAPI.updateUserProfile(currentUser.value.id, updates)
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
