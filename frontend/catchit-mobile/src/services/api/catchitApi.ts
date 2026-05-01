import type {
  Card,
  CardTier,
  PaymentMethod,
  Stop,
  Ticket,
  TravelCard,
  Trip,
  UserNotification,
  User,
  Vehicle,
} from '../../models'
import { TicketStatus } from '../../models'
import { requestJson, type ApiResponse } from './http'

type BackendLocation = {
  latitude?: number
  longitude?: number
}

type BackendStop = {
  id: string
  name: string
  stopType?: string
  location?: BackendLocation | null
  latitude?: number
  longitude?: number
}

type BackendTicket = {
  id: string
  userID?: string
  createdAt?: string
  validFrom?: string
  validUntil?: string
  price?: number | string
  qrCode?: string
  status?: string
  from?: BackendStop | null
  to?: BackendStop | null
}

type BackendCard = {
  id: string
  price?: number | string
  validFrom?: string
  validUntil?: string
  zone?: { id: string; name?: string } | null
}

type BackendRouteSchedule = {
  stop?: BackendStop | null
  arrivalTime?: string
  departureTime?: string
  sequence?: number
}

type BackendRoute = {
  id: string
  name?: string
  schedules?: BackendRouteSchedule[]
}

type BackendRouteSearchResult = {
  routeId: string
  routeName?: string
  fromStop?: BackendStop | null
  toStop?: BackendStop | null
  departureTime?: string
  arrivalTime?: string
  price?: number
}

type BackendTrip = {
  id: string
  startTime?: string
  endTime?: string
  route?: BackendRoute | null
  vehicle?: { id: string; capacity?: number } | null
}

type BackendUser = {
  id: string
  name?: string
  email?: string
  balance?: number
  card?: BackendCard | null
  tickets?: BackendTicket[]
  notifications?: BackendNotification[]
}

type BackendNotification = {
  id: string
  stopId?: string
  stopName?: string
  message?: string
  createdAt?: string
}

type BackendAuthResponse = {
  user: BackendUser
  token: string
}

type BackendCheckoutSession = {
  sessionId: string
  subtotal: number
  taxes: number
  total: number
}

type BackendCheckoutConfirmation = {
  orderId: string
  confirmationNumber: string
  items: string[]
}

type BackendCheckoutOrderValidation = {
  orderId: string
  paymentStatus: string
  valid: boolean
  message: string
}

type BackendCartItemSource = {
  cardId?: string
  tier?: CardTier
  routeId?: string
  fromStopId?: string
  toStopId?: string
  fromStop?: string
  toStop?: string
  departureTime?: string
  arrivalTime?: string
}

type BackendCartItem = {
  id: string
  kind: 'card' | 'ticket'
  title: string
  description: string
  quantity: number
  unitPrice: number
  totalPrice: number
  source: BackendCartItemSource
}

type BackendCartResponse = {
  items: BackendCartItem[]
  subtotal: number
  taxes: number
  total: number
}

type RouteSearchResult = {
  routeId: string
  fromStop: Stop
  toStop: Stop
  departureTime: string
  arrivalTime: string
  price: number
  vehicle: Vehicle
}

const storedPaymentMethodsKey = 'catchit.paymentMethods'

const defaultPaymentMethods = (): PaymentMethod[] => [
  {
    id: 'balance-default',
    type: 'digital_wallet',
    isDefault: true,
  },
]

const normalizePaymentMethods = (methods: PaymentMethod[]): PaymentMethod[] => {
  const balanceMethods = methods.filter((method) => method.id.toLowerCase().startsWith('balance-'))
  if (!balanceMethods.length) {
    return defaultPaymentMethods()
  }

  return balanceMethods.map((method, index) => ({
    ...method,
    type: 'digital_wallet',
    isDefault: index === 0,
    cardLast4: undefined,
  }))
}

const toDate = (value?: string) => (value ? new Date(value) : new Date())

const mapStop = (stop: BackendStop): Stop => ({
  id: stop.id,
  name: stop.name,
  latitude: stop.location?.latitude ?? stop.latitude ?? 0,
  longitude: stop.location?.longitude ?? stop.longitude ?? 0,
  stopType: stop.stopType,
})

const mapTicketStatus = (status?: string): TicketStatus => {
  const normalized = (status ?? '').toUpperCase()
  if (normalized.includes('VALID')) return TicketStatus.Valid
  if (normalized.includes('EXPIRED')) return TicketStatus.Expired
  if (normalized.includes('USED')) return TicketStatus.Used
  return TicketStatus.PurchasedButNotValid
}

const mapTicket = (ticket: BackendTicket, userId = ''): Ticket => ({
  id: ticket.id,
  userID: ticket.userID ?? userId,
  createdAt: toDate(ticket.createdAt),
  validFrom: toDate(ticket.validFrom),
  validUntil: toDate(ticket.validUntil),
  price: Number(ticket.price ?? 0),
  qrCode: ticket.qrCode ?? '',
  status: mapTicketStatus(ticket.status),
  stopFrom: mapStop(ticket.from ?? { id: '', name: '', latitude: 0, longitude: 0 }),
  stopTo: mapStop(ticket.to ?? { id: '', name: '', latitude: 0, longitude: 0 }),
})

const inferTier = (label: string): CardTier => {
  const normalized = label.toLowerCase()
  if (normalized.includes('weekly')) return 'weekly'
  if (normalized.includes('yearly')) return 'yearly'
  return 'monthly'
}

const mapCard = (card: BackendCard, userId = ''): Card & TravelCard => {
  const displayName = card.zone?.name?.trim() || `Card ${card.id.slice(0, 8)}`
  const tier = inferTier(displayName)
  const price = Number(card.price ?? 0)

  return {
    id: card.id,
    userID: userId,
    name: displayName,
    price,
    description: card.zone?.name || 'Travel card',
    validFrom: toDate(card.validFrom),
    validUntil: toDate(card.validUntil),
    tier,
    monthlyPrice: price,
    annualPrice: price,
  }
}

const mapVehicle = (vehicle?: BackendTrip['vehicle']): Vehicle => ({
  id: vehicle?.id ?? '',
  capacity: vehicle?.capacity ?? 0,
  currentPassengers: 0,
  updateLocation: () => {},
  notifyObservers: () => {},
})

const mapNotification = (notification: BackendNotification): UserNotification => ({
  id: notification.id,
  stopId: notification.stopId ?? '',
  stopName: notification.stopName ?? '',
  message: notification.message ?? '',
  createdAt: notification.createdAt ?? new Date().toISOString(),
})

const mapUser = (user: BackendUser): User => ({
  id: user.id,
  name: user.name ?? '',
  email: user.email ?? '',
  balance: Number(user.balance ?? 0),
  tickets: (user.tickets ?? []).map((ticket) => mapTicket(ticket, user.id)),
  notifications: (user.notifications ?? []).map(mapNotification),
})

const loadPaymentMethods = (): PaymentMethod[] => {
  const stored = localStorage.getItem(storedPaymentMethodsKey)
  if (!stored) {
    const seeded = defaultPaymentMethods()
    savePaymentMethods(seeded)
    return seeded
  }

  try {
    const parsed = JSON.parse(stored) as PaymentMethod[]
    if (!Array.isArray(parsed) || parsed.length === 0) {
      const seeded = defaultPaymentMethods()
      savePaymentMethods(seeded)
      return seeded
    }

    const normalized = normalizePaymentMethods(parsed)
    savePaymentMethods(normalized)
    return normalized
  } catch {
    const seeded = defaultPaymentMethods()
    savePaymentMethods(seeded)
    return seeded
  }
}

const savePaymentMethods = (paymentMethods: PaymentMethod[]) => {
  localStorage.setItem(storedPaymentMethodsKey, JSON.stringify(paymentMethods))
}

export class CatchItApiClient {
  async signup(data: { name: string; email: string; password: string }): Promise<ApiResponse<{ user: User; token: string }>> {
    const response = await requestJson<BackendAuthResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({
        name: data.name,
        email: data.email,
        password: data.password,
      }),
    })

    if (!response.success || !response.data) {
      return { success: false, error: response.error }
    }

    return {
      success: true,
      data: {
        user: mapUser(response.data.user),
        token: response.data.token,
      },
    }
  }

  async login(data: { email: string; password: string }): Promise<ApiResponse<{ user: User; token: string }>> {
    const response = await requestJson<BackendAuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        email: data.email,
        password: data.password,
      }),
    })

    if (!response.success || !response.data) {
      return { success: false, error: response.error }
    }

    return {
      success: true,
      data: {
        user: mapUser(response.data.user),
        token: response.data.token,
      },
    }
  }

  async logout(): Promise<ApiResponse<void>> {
    return requestJson<void>('/api/auth/logout', { method: 'POST' })
  }

  async getCurrentSessionUser(): Promise<ApiResponse<User>> {
    const response = await requestJson<BackendUser>('/api/auth/me')
    if (!response.success || !response.data) return { success: false, error: response.error }
    return { success: true, data: mapUser(response.data) }
  }

  async getUserProfile(userId: string): Promise<ApiResponse<User>> {
    const response = await requestJson<BackendUser>(`/api/users/${userId}`)
    if (!response.success || !response.data) return { success: false, error: response.error }
    return { success: true, data: mapUser(response.data) }
  }

  async getUserNotifications(userId: string): Promise<ApiResponse<UserNotification[]>> {
    const response = await requestJson<BackendNotification[]>(`/api/users/${userId}/notifications`)
    if (!response.success || !response.data) return { success: false, error: response.error }
    return { success: true, data: response.data.map(mapNotification) }
  }

  async deleteUserNotification(userId: string, notificationId: string): Promise<ApiResponse<void>> {
    return requestJson<void>(`/api/users/${userId}/notifications/${notificationId}`, {
      method: 'DELETE',
    })
  }

  async updateUserProfile(userId: string, updates: Partial<User>): Promise<ApiResponse<User>> {
    const response = await requestJson<BackendUser>(`/api/users/${userId}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: updates.name,
        email: updates.email,
        balance: updates.balance,
      }),
    })

    if (!response.success || !response.data) return { success: false, error: response.error }
    return { success: true, data: mapUser(response.data) }
  }

  async getUserTickets(userId: string): Promise<ApiResponse<Ticket[]>> {
    const response = await this.getUserProfile(userId)
    if (!response.success || !response.data) return { success: false, error: response.error }
    return { success: true, data: response.data.tickets }
  }

  async purchaseTickets(data: {
    userID: string
    tripID: string
    quantity: number
    price: number
    stopFromID: string
    stopToID: string
  }): Promise<ApiResponse<Ticket[]>> {
    void data.tripID
    const stopsResponse = await this.getStops()
    if (!stopsResponse.success || !stopsResponse.data) {
      return { success: false, error: stopsResponse.error }
    }

    const stopFrom = stopsResponse.data.find((stop) => stop.id === data.stopFromID)
    const stopTo = stopsResponse.data.find((stop) => stop.id === data.stopToID)
    if (!stopFrom || !stopTo) {
      return { success: false, error: 'Invalid stops' }
    }

    const createdTickets: Ticket[] = []

    for (let index = 0; index < data.quantity; index += 1) {
      const response = await requestJson<BackendTicket>('/api/tickets', {
        method: 'POST',
        body: JSON.stringify({
          createdAt: new Date().toISOString(),
          validFrom: new Date().toISOString(),
          validUntil: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString(),
          price: data.price,
          qrCode: `TICKET_${Date.now()}_${index}`,
          status: TicketStatus.PurchasedButNotValid,
          from: stopFrom,
          to: stopTo,
        }),
      })

      if (!response.success || !response.data) {
        return { success: false, error: response.error }
      }

      createdTickets.push(mapTicket(response.data, data.userID))
    }

    const currentUserResponse = await this.getUserProfile(data.userID)
    if (currentUserResponse.success && currentUserResponse.data) {
      await requestJson<BackendUser>(`/api/users/${data.userID}`, {
        method: 'PUT',
        body: JSON.stringify({
          name: currentUserResponse.data.name,
          email: currentUserResponse.data.email,
          balance: currentUserResponse.data.balance,
          tickets: [...currentUserResponse.data.tickets, ...createdTickets],
        }),
      })
    }

    return { success: true, data: createdTickets }
  }

  async activateTicket(ticketId: string): Promise<ApiResponse<Ticket>> {
    const response = await this.getTicket(ticketId)
    if (!response.success || !response.data) return response

    const updateResponse = await requestJson<BackendTicket>(`/api/tickets/${ticketId}`, {
      method: 'PUT',
      body: JSON.stringify({
        id: ticketId,
        status: TicketStatus.Valid,
        createdAt: response.data.createdAt,
        validFrom: response.data.validFrom,
        validUntil: response.data.validUntil,
        price: response.data.price,
        qrCode: response.data.qrCode,
        from: response.data.stopFrom,
        to: response.data.stopTo,
      }),
    })

    if (!updateResponse.success || !updateResponse.data) {
      return { success: false, error: updateResponse.error }
    }

    return { success: true, data: mapTicket(updateResponse.data) }
  }

  async cancelTicket(ticketId: string): Promise<ApiResponse<{ success: boolean }>> {
    const response = await requestJson<unknown>(`/api/tickets/${ticketId}`, { method: 'DELETE' })
    if (!response.success) return { success: false, error: response.error }
    return { success: true, data: { success: true } }
  }

  async getUserCards(userId: string): Promise<ApiResponse<Card[]>> {
    const userResponse = await requestJson<BackendUser>(`/api/users/${userId}`)
    if (!userResponse.success || !userResponse.data) {
      return { success: true, data: [] }
    }

    const userCard = userResponse.data.card
    return { success: true, data: userCard ? [mapCard(userCard, userId)] : [] }
  }

  async getAvailableCards(): Promise<ApiResponse<TravelCard[]>> {
    const response = await requestJson<BackendCard[]>('/api/cards')
    if (!response.success || !response.data) return { success: false, error: response.error }
    return {
      success: true,
      data: response.data.map((card) => mapCard(card)),
    }
  }

  async purchaseCard(data: { userId: string; cardId: string; tier: CardTier }): Promise<ApiResponse<Card>> {
    const availableCards = await this.getAvailableCards()
    const selectedCard = availableCards.success && availableCards.data?.find((card) => card.id === data.cardId)

    const selectedCardPayload = selectedCard
      ? {
          price: selectedCard.price,
          validFrom: selectedCard.validFrom.toISOString(),
          validUntil: selectedCard.validUntil.toISOString(),
          zone: null,
        }
      : {
          price: data.tier === 'weekly' ? 8 : data.tier === 'monthly' ? 20 : 200,
          validFrom: new Date().toISOString(),
          validUntil: new Date(Date.now() + (data.tier === 'weekly' ? 7 : data.tier === 'monthly' ? 30 : 365) * 24 * 60 * 60 * 1000).toISOString(),
          zone: null,
        }

    const createResponse = await requestJson<BackendCard>('/api/cards', {
      method: 'POST',
      body: JSON.stringify(selectedCardPayload),
    })

    if (!createResponse.success || !createResponse.data) {
      return { success: false, error: createResponse.error }
    }

    const createdCard = mapCard(createResponse.data, data.userId)

    const userResponse = await this.getUserProfile(data.userId)
    if (userResponse.success && userResponse.data) {
      await requestJson<BackendUser>(`/api/users/${data.userId}`, {
        method: 'PUT',
        body: JSON.stringify({
          name: userResponse.data.name,
          email: userResponse.data.email,
          balance: userResponse.data.balance,
          card: createResponse.data,
        }),
      })
    }

    return { success: true, data: createdCard }
  }

  async getStops(): Promise<ApiResponse<Stop[]>> {
    const response = await requestJson<BackendStop[]>('/api/stops')
    if (!response.success || !response.data) return { success: false, error: response.error }
    return { success: true, data: response.data.map(mapStop) }
  }

  async getRoutes(): Promise<ApiResponse<BackendRoute[]>> {
    const response = await requestJson<BackendRoute[]>('/api/routes')
    if (!response.success || !response.data) return { success: false, error: response.error }
    return { success: true, data: response.data }
  }

  async searchRoutes(data: {
    fromStopId: string
    toStopId: string
    departureDate: string
  }): Promise<ApiResponse<RouteSearchResult[]>> {
    const searchParams = new URLSearchParams({
      fromStopId: data.fromStopId,
      toStopId: data.toStopId,
    })

    const backendSearch = await requestJson<BackendRouteSearchResult[]>(`/api/routes/search?${searchParams.toString()}`)
    if (!backendSearch.success || !backendSearch.data) {
      return { success: false, error: backendSearch.error || 'Unable to search routes' }
    }

    const mappedResults = backendSearch.data.map((result) => ({
      routeId: result.routeId,
      fromStop: mapStop(result.fromStop ?? { id: data.fromStopId, name: 'Origin', latitude: 0, longitude: 0 }),
      toStop: mapStop(result.toStop ?? { id: data.toStopId, name: 'Destination', latitude: 0, longitude: 0 }),
      departureTime: result.departureTime ?? '00:00',
      arrivalTime: result.arrivalTime ?? '00:00',
      price: Number(result.price ?? 0),
      vehicle: mapVehicle(),
    }))

    return { success: true, data: mappedResults }
  }

  async bookTravel(data: { userId: string; routeId: string; tripId: string }): Promise<ApiResponse<Trip>> {
    const response = await requestJson<BackendTrip>('/api/trips', {
      method: 'POST',
      body: JSON.stringify({
        id: data.tripId,
        startTime: new Date().toISOString(),
        endTime: new Date(Date.now() + 4 * 60 * 60 * 1000).toISOString(),
        route: { id: data.routeId },
      }),
    })

    if (!response.success || !response.data) {
      return { success: false, error: response.error }
    }

    const routeResponse = await requestJson<BackendRoute>(`/api/routes/${data.routeId}`)
    const routeStops = routeResponse.success && routeResponse.data
      ? [...(routeResponse.data.schedules ?? [])]
          .filter((schedule): schedule is BackendRouteSchedule & { stop: BackendStop } => !!schedule.stop)
          .sort((left, right) => (left.sequence ?? 0) - (right.sequence ?? 0))
          .map((schedule) => mapStop(schedule.stop))
      : []

    return {
      success: true,
      data: {
        id: response.data.id,
        startTime: toDate(response.data.startTime),
        endTime: toDate(response.data.endTime),
        stops: routeStops,
        vehicle: mapVehicle(response.data.vehicle),
      },
    }
  }

  async getCart(): Promise<ApiResponse<BackendCartResponse>> {
    return requestJson<BackendCartResponse>('/api/cart')
  }

  async upsertCartItem(item: BackendCartItem): Promise<ApiResponse<BackendCartResponse>> {
    return requestJson<BackendCartResponse>('/api/cart/items', {
      method: 'POST',
      body: JSON.stringify(item),
    })
  }

  async removeCartItem(itemId: string): Promise<ApiResponse<BackendCartResponse>> {
    return requestJson<BackendCartResponse>(`/api/cart/items/${itemId}`, {
      method: 'DELETE',
    })
  }

  async clearCart(): Promise<ApiResponse<BackendCartResponse>> {
    return requestJson<BackendCartResponse>('/api/cart', {
      method: 'DELETE',
    })
  }

  async createCheckoutSession(): Promise<ApiResponse<BackendCheckoutSession>> {
    return requestJson<BackendCheckoutSession>('/api/checkout/session', {
      method: 'POST',
      body: JSON.stringify({}),
    })
  }

  async confirmCheckout(data: {
    sessionId: string
    paymentMethodId: string
  }): Promise<ApiResponse<BackendCheckoutConfirmation>> {
    return requestJson<BackendCheckoutConfirmation>('/api/checkout/confirm', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  async validateCheckoutOrder(orderId: string): Promise<ApiResponse<BackendCheckoutOrderValidation>> {
    return requestJson<BackendCheckoutOrderValidation>(`/api/checkout/orders/${orderId}/validation`)
  }

  async getPaymentMethods(userId: string): Promise<ApiResponse<PaymentMethod[]>> {
    void userId
    return { success: true, data: loadPaymentMethods() }
  }

  async addPaymentMethod(userId: string, data: Partial<PaymentMethod>): Promise<ApiResponse<PaymentMethod>> {
    void userId
    const paymentMethod: PaymentMethod = {
      id: `balance-${Date.now()}`,
      type: 'digital_wallet',
      isDefault: true,
    }

    savePaymentMethods([paymentMethod])

    return { success: true, data: paymentMethod }
  }

  async validateTicket(data: { ticketId: string; vehicleId: string }): Promise<
    ApiResponse<{
      valid: boolean
      message: string
      remainingValidityTime: number
    }>
  > {
    void data
    return {
      success: true,
      data: {
        valid: true,
        message: 'Ticket validated successfully',
        remainingValidityTime: 12 * 60 * 60 * 1000,
      },
    }
  }

  async getTicket(ticketId: string): Promise<ApiResponse<Ticket>> {
    const response = await requestJson<BackendTicket>(`/api/tickets/${ticketId}`)
    if (!response.success || !response.data) return { success: false, error: response.error }
    return { success: true, data: mapTicket(response.data) }
  }

  async addPOI(userId: string, stopId: string): Promise<ApiResponse<void>> {
    return requestJson<void>(`/api/stops/${stopId}/observers/${userId}`, {
      method: 'POST',
    })
  }

  async removePOI(userId: string, stopId: string): Promise<ApiResponse<void>> {
    return requestJson<void>(`/api/stops/${stopId}/observers/${userId}`, {
      method: 'DELETE',
    })
  }

  async getUserPOI(userId: string): Promise<ApiResponse<Stop[]>> {
    const response = await requestJson<BackendStop[]>(`/api/users/${userId}/poi`)
    if (!response.success || !response.data) return { success: false, error: response.error }
    return { success: true, data: response.data.map(mapStop) }
  }
}

export const catchitApi = new CatchItApiClient()
