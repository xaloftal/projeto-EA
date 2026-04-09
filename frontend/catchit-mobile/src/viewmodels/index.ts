import { ref, computed } from 'vue'
import { mockAPI } from '../services/api/mockAPI'
import type { User, Ticket, Card, PaymentMethod, Stop, Vehicle } from '../models'

type RouteSearchResult = {
  routeId: string
  fromStop: Stop
  toStop: Stop
  departureTime: string
  arrivalTime: string
  price: number
  vehicle: Vehicle
}

type CartEntry = {
  id: string
  item: Ticket | Card
  quantity: number
  price: number
}

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
  const availableCards = ref<Card[]>([])
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

  const purchaseCard = async (cardId: string, type: 'monthly' | 'annual') => {
    if (!currentUser.value) return false
    isLoading.value = true
    try {
      const response = await mockAPI.purchaseCard({
        userId: currentUser.value.id,
        cardId,
        type,
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
  const cartItems = ref<CartEntry[]>([])
  const subtotal = ref(0)
  const taxes = ref(0)
  const total = ref(0)
  const isLoading = ref(false)
  const error = ref<string>('')
  const paymentMethods = ref<PaymentMethod[]>([])

  const addToCart = (item: Ticket | Card, quantity: number = 1) => {
    cartItems.value.push({
      id: Date.now().toString(),
      item,
      quantity,
      price: item.price * quantity,
    })
    calculateTotals()
  }

  const removeFromCart = (itemId: string) => {
    cartItems.value = cartItems.value.filter((item) => item.id !== itemId)
    calculateTotals()
  }

  const calculateTotals = () => {
    subtotal.value = cartItems.value.reduce((sum, item) => sum + item.price, 0)
    taxes.value = subtotal.value * 0.1
    total.value = subtotal.value + taxes.value
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
          type: 'tripID' in item.item ? 'ticket' : 'card',
          itemId: item.item.id,
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

  const confirmCheckout = async (sessionId: string, paymentMethodId: string) => {
    isLoading.value = true
    error.value = ''
    try {
      const response = await mockAPI.confirmCheckout({
        sessionId,
        paymentMethodId,
      })
      if (response.success && response.data) {
        cartItems.value = []
        calculateTotals()
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
    addToCart,
    removeFromCart,
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
