import type {
  Card,
  CardTier,
  PaymentMethod,
  Stop,
  Ticket,
  TravelCard,
  Trip,
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

const toDate = (value?: string) => (value ? new Date(value) : new Date())

const mapStop = (stop: BackendStop): Stop => ({
  id: stop.id,
  name: stop.name,
  latitude: stop.location?.latitude ?? stop.latitude ?? 0,
  longitude: stop.location?.longitude ?? stop.longitude ?? 0,
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

const mapUser = (user: BackendUser): User => ({
  id: user.id,
  name: user.name ?? '',
  email: user.email ?? '',
  balance: Number(user.balance ?? 0),
  tickets: (user.tickets ?? []).map((ticket) => mapTicket(ticket, user.id)),
})

const loadPaymentMethods = (): PaymentMethod[] => {
  const stored = localStorage.getItem(storedPaymentMethodsKey)
  if (!stored) return []

  try {
    return JSON.parse(stored) as PaymentMethod[]
  } catch {
    return []
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

  async searchRoutes(data: {
    fromStopId: string
    toStopId: string
    departureDate: string
  }): Promise<ApiResponse<RouteSearchResult[]>> {
    void data.departureDate

    const routesResponse = await requestJson<BackendRoute[]>('/api/routes')
    const stopsResponse = await this.getStops()

    if (!routesResponse.success || !routesResponse.data || !stopsResponse.success || !stopsResponse.data) {
      return {
        success: false,
        error: routesResponse.error || stopsResponse.error || 'Unable to search routes',
      }
    }

    const routeResults = routesResponse.data.flatMap((route, index) => {
      const schedules = [...(route.schedules ?? [])]
        .filter((schedule): schedule is BackendRouteSchedule & { stop: BackendStop } => !!schedule.stop)
        .sort((left, right) => (left.sequence ?? 0) - (right.sequence ?? 0))

      if (schedules.length < 2) return []

      const firstStop = schedules[0].stop
      const lastStop = schedules[schedules.length - 1].stop

      if (firstStop.id !== data.fromStopId || lastStop.id !== data.toStopId) {
        return []
      }

      return [
        {
          routeId: route.id,
          fromStop: mapStop(firstStop),
          toStop: mapStop(lastStop),
          departureTime: schedules[0].departureTime?.slice(11, 16) ?? '00:00',
          arrivalTime: schedules[schedules.length - 1].arrivalTime?.slice(11, 16) ?? '00:00',
          price: 19.98 + index,
          vehicle: mapVehicle(),
        },
      ]
    })

    return { success: true, data: routeResults }
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

  async createCheckoutSession(data: {
    userId: string
    items: Array<{
      type: 'ticket' | 'card'
      itemId: string
      quantity: number
    }>
  }): Promise<ApiResponse<BackendCheckoutSession>> {
    void data.userId
    const subtotal = data.items.reduce((sum, item) => sum + item.quantity * 19.98, 0)
    const taxes = subtotal * 0.1

    return {
      success: true,
      data: {
        sessionId: `session_${Date.now()}`,
        subtotal,
        taxes,
        total: subtotal + taxes,
      },
    }
  }

  async confirmCheckout(data: {
    sessionId: string
    paymentMethodId: string
  }): Promise<ApiResponse<BackendCheckoutConfirmation>> {
    void data.paymentMethodId
    return {
      success: true,
      data: {
        orderId: `order_${Date.now()}`,
        confirmationNumber: Math.random().toString(36).substring(2, 11).toUpperCase(),
        items: [data.sessionId],
      },
    }
  }

  async getPaymentMethods(userId: string): Promise<ApiResponse<PaymentMethod[]>> {
    void userId
    return { success: true, data: loadPaymentMethods() }
  }

  async addPaymentMethod(userId: string, data: Partial<PaymentMethod>): Promise<ApiResponse<PaymentMethod>> {
    void userId
    const paymentMethod: PaymentMethod = {
      id: `payment_${Date.now()}`,
      type: data.type || 'credit_card',
      isDefault: data.isDefault ?? false,
      cardLast4: data.cardLast4 || '0000',
    }

    const currentMethods = loadPaymentMethods()
    currentMethods.push(paymentMethod)
    savePaymentMethods(currentMethods)

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
}

export const catchitApi = new CatchItApiClient()
