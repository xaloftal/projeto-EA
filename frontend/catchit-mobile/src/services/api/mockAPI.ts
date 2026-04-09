import type {
  User,
  Ticket,
  Card,
  TravelCard,
  PaymentMethod,
  Stop,
  Vehicle,
  Trip,
} from '../../models'
import { TicketStatus } from '../../models'

// ============================================================================
// MOCK DATA - This simulates backend data
// ============================================================================

const mockStops: Stop[] = [
  {
    id: 'stop_1',
    name: 'Vila Nova de Famalicão (Estação de Autocarros)',
    latitude: 41.4057,
    longitude: -8.5332,
  },
  {
    id: 'stop_2',
    name: 'Covilhã',
    latitude: 40.2806,
    longitude: -7.5004,
  },
  {
    id: 'stop_3',
    name: 'Lisboa (Oriente)',
    latitude: 38.7613,
    longitude: -9.0951,
  },
  {
    id: 'stop_4',
    name: 'Cascais',
    latitude: 38.6926,
    longitude: -9.4218,
  },
  {
    id: 'stop_5',
    name: 'Sintra',
    latitude: 38.8038,
    longitude: -9.3899,
  },
  {
    id: 'stop_6',
    name: '1722 Rotunda Estação II',
    latitude: 38.7,
    longitude: -9.1,
  },
  {
    id: 'stop_7',
    name: '0042 Caires',
    latitude: 38.65,
    longitude: -9.15,
  },
]

const mockTickets: Ticket[] = [
  {
    id: 'ticket_1',
    userID: 'user_1',
    createdAt: new Date('2024-11-01'),
    validFrom: new Date('2025-12-02'),
    validUntil: new Date('2026-12-02'),
    price: 45.0,
    qrCode: 'TUB_2025_001',
    status: TicketStatus.Valid,
    tripID: 'trip_1',
  },
  {
    id: 'ticket_2',
    userID: 'user_1',
    createdAt: new Date('2024-10-15'),
    validFrom: new Date('2025-01-01'),
    validUntil: new Date('2026-01-01'),
    price: 125.0,
    qrCode: 'TUB_2025_002',
    status: TicketStatus.Valid,
    tripID: 'trip_2',
  },
]

const mockCards: Card[] = [
  {
    id: 'card_1',
    userID: 'user_1',
    name: 'TUB Card',
    price: 20.0,
    description: 'Travel card for TUB Transportes',
  },
  {
    id: 'card_2',
    userID: 'user_1',
    name: 'FlixBus Card',
    price: 15.0,
    description: 'Travel card for FlixBus',
  },
]

const mockTravelCards: TravelCard[] = [
  {
    id: 'travel_card_1',
    userID: '',
    name: 'Travel Card 1',
    price: 20,
    monthlyPrice: 20,
    annualPrice: 200,
  },
  {
    id: 'travel_card_2',
    userID: '',
    name: 'Travel Card 2',
    price: 30,
    monthlyPrice: 30,
    annualPrice: 300,
  },
  {
    id: 'travel_card_3',
    userID: '',
    name: 'Travel Card 3',
    price: 50,
    monthlyPrice: 50,
    annualPrice: 500,
  },
]

const mockPaymentMethods: PaymentMethod[] = [
  {
    id: 'payment_1',
    type: 'credit_card',
    isDefault: true,
    cardLast4: '1234',
  },
]

const mockCurrentUser: User = {
  id: 'user_1',
  name: 'Maria Bernardina',
  email: 'maria@example.com',
  balance: 150.5,
  tickets: mockTickets,
}

// ============================================================================
// MOCK API SERVICE
// ============================================================================

export interface APIResponse<T> {
  success: boolean
  data?: T
  error?: string
}

class MockAPIService {
  // Simulates network delay
  private delay(ms: number = 500): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms))
  }

  // ========================================
  // AUTH ROUTES
  // ========================================

  /**
   * POST /api/auth/signup
   * Register a new user
   */
  async signup(data: {
    name: string
    email: string
    password: string
  }): Promise<APIResponse<{ user: User; token: string }>> {
    await this.delay()
    return {
      success: true,
      data: {
        user: {
          ...mockCurrentUser,
          name: data.name,
          email: data.email,
        },
        token: 'mock_jwt_token_' + Date.now(),
      },
    }
  }

  /**
   * POST /api/auth/login
   * Authenticate user
   */
  async login(data: {
    email: string
    password: string
  }): Promise<APIResponse<{ user: User; token: string }>> {
    await this.delay()
    if (data.email === 'maria@example.com' && data.password === 'password') {
      return {
        success: true,
        data: {
          user: mockCurrentUser,
          token: 'mock_jwt_token_' + Date.now(),
        },
      }
    }
    return {
      success: false,
      error: 'Invalid credentials',
    }
  }

  // ========================================
  // USER ROUTES
  // ========================================

  /**
   * GET /api/users/:id
   * Get user profile
   */
  async getUserProfile(userId: string): Promise<APIResponse<User>> {
    await this.delay()
    void userId
    return {
      success: true,
      data: mockCurrentUser,
    }
  }

  /**
   * PUT /api/users/:id
   * Update user profile
   */
  async updateUserProfile(
    userId: string,
    data: Partial<User>
  ): Promise<APIResponse<User>> {
    await this.delay()
    return {
      success: true,
      data: { ...mockCurrentUser, ...data },
    }
  }

  // ========================================
  // TICKET ROUTES
  // ========================================

  /**
   * GET /api/users/:userId/tickets
   * Get all tickets for user
   */
  async getUserTickets(userId: string): Promise<APIResponse<Ticket[]>> {
    await this.delay()
    void userId
    return {
      success: true,
      data: mockTickets,
    }
  }

  /**
   * GET /api/tickets/:ticketId
   * Get single ticket details
   */
  async getTicket(ticketId: string): Promise<APIResponse<Ticket>> {
    await this.delay()
    const ticket = mockTickets.find((t) => t.id === ticketId)
    if (!ticket) {
      return {
        success: false,
        error: 'Ticket not found',
      }
    }

    return {
      success: true,
      data: ticket,
    }
  }

  /**
   * POST /api/tickets
   * Create/purchase tickets
   */
  async purchaseTickets(data: {
    userID: string
    tripID: string
    quantity: number
    price: number
  }): Promise<APIResponse<Ticket[]>> {
    await this.delay()
    const newTickets: Ticket[] = Array.from({ length: data.quantity }).map(
      (_, i) => ({
        id: `ticket_${Date.now()}_${i}`,
        userID: data.userID,
        createdAt: new Date(),
        validFrom: new Date(),
        validUntil: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000),
        price: data.price,
        qrCode: `TUB_${Date.now()}_${i}`,
        status: TicketStatus.PurchasedButNotValid,
        tripID: data.tripID,
      })
    )
    return {
      success: true,
      data: newTickets,
    }
  }

  /**
   * PUT /api/tickets/:ticketId/activate
   * Activate a ticket for use
   */
  async activateTicket(ticketId: string): Promise<APIResponse<Ticket>> {
    await this.delay()
    const ticket = mockTickets[0]
    return {
      success: true,
      data: {
        ...ticket,
        id: ticketId,
        status: TicketStatus.Valid,
      } as Ticket,
    }
  }

  /**
   * DELETE /api/tickets/:ticketId
   * Cancel/delete ticket
   */
  async cancelTicket(ticketId: string): Promise<APIResponse<{ success: boolean }>> {
    await this.delay()
    void ticketId
    return {
      success: true,
      data: { success: true },
    }
  }

  // ========================================
  // CARD ROUTES
  // ========================================

  /**
   * GET /api/users/:userId/cards
   * Get all cards for user
   */
  async getUserCards(userId: string): Promise<APIResponse<Card[]>> {
    await this.delay()
    void userId
    return {
      success: true,
      data: mockCards,
    }
  }

  /**
   * GET /api/cards/available
   * Get all available cards to purchase
   */
  async getAvailableCards(): Promise<APIResponse<TravelCard[]>> {
    await this.delay()
    return {
      success: true,
      data: mockTravelCards,
    }
  }

  /**
   * POST /api/cards
   * Purchase/add a card
   */
  async purchaseCard(data: {
    userId: string
    cardId: string
    type: 'monthly' | 'annual'
  }): Promise<APIResponse<Card>> {
    await this.delay()
    const travelCard = mockTravelCards.find((c) => c.id === data.cardId)
    if (!travelCard) {
      return {
        success: false,
        error: 'Card not found',
      }
    }
    const newCard: Card = {
      id: `card_${Date.now()}`,
      userID: data.userId,
      name: travelCard.name,
      price: data.type === 'monthly' ? travelCard.monthlyPrice : travelCard.annualPrice,
    }
    return {
      success: true,
      data: newCard,
    }
  }

  // ========================================
  // SEARCH & TRAVEL ROUTES
  // ========================================

  /**
   * GET /api/stops
   * Get all available stops
   */
  async getStops(): Promise<APIResponse<Stop[]>> {
    await this.delay()
    return {
      success: true,
      data: mockStops,
    }
  }

  /**
   * GET /api/routes/search
   * Search available routes
   */
  async searchRoutes(data: {
    fromStopId: string
    toStopId: string
    departureDate: string
  }): Promise<
    APIResponse<
      {
        routeId: string
        fromStop: Stop
        toStop: Stop
        departureTime: string
        arrivalTime: string
        price: number
        vehicle: Vehicle
      }[]
    >
  > {
    await this.delay()
    const fromStop = mockStops.find((s) => s.id === data.fromStopId)
    const toStop = mockStops.find((s) => s.id === data.toStopId)

    if (!fromStop || !toStop) {
      return {
        success: false,
        error: 'Invalid stops',
      }
    }

    return {
      success: true,
      data: [
        {
          routeId: 'route_1',
          fromStop,
          toStop,
          departureTime: '06:35',
          arrivalTime: '10:30',
          price: 19.98,
          vehicle: {
            id: 'vehicle_1',
            capacity: 50,
            currentPassengers: 23,
            updateLocation: () => {},
            notifyObservers: () => {},
          },
        },
        {
          routeId: 'route_2',
          fromStop,
          toStop,
          departureTime: '14:00',
          arrivalTime: '18:30',
          price: 19.98,
          vehicle: {
            id: 'vehicle_2',
            capacity: 50,
            currentPassengers: 35,
            updateLocation: () => {},
            notifyObservers: () => {},
          },
        },
      ],
    }
  }

  /**
   * POST /api/travel/book
   * Book a travel
   */
  async bookTravel(data: {
    userId: string
    routeId: string
    tripId: string
  }): Promise<APIResponse<Trip>> {
    await this.delay()
    return {
      success: true,
      data: {
        id: data.tripId,
        startTime: new Date(),
        endTime: new Date(Date.now() + 4 * 60 * 60 * 1000),
        stops: mockStops.slice(0, 3),
        vehicle: {
          id: 'vehicle_1',
          capacity: 50,
          currentPassengers: 25,
          updateLocation: () => {},
          notifyObservers: () => {},
        },
      },
    }
  }

  // ========================================
  // CHECKOUT & PAYMENT ROUTES
  // ========================================

  /**
   * POST /api/checkout/session
   * Create checkout session
   */
  async createCheckoutSession(data: {
    userId: string
    items: Array<{
      type: 'ticket' | 'card'
      itemId: string
      quantity: number
    }>
  }): Promise<
    APIResponse<{
      sessionId: string
      subtotal: number
      taxes: number
      total: number
    }>
  > {
    await this.delay()
    const subtotal = 19.98 * data.items.length
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

  /**
   * POST /api/checkout/confirm
   * Confirm payment and complete checkout
   */
  async confirmCheckout(data: {
    sessionId: string
    paymentMethodId: string
  }): Promise<
    APIResponse<{
      orderId: string
      confirmationNumber: string
      items: string[]
    }>
  > {
    await this.delay()
    void data
    return {
      success: true,
      data: {
        orderId: `order_${Date.now()}`,
        confirmationNumber: Math.random().toString(36).substring(2, 11).toUpperCase(),
        items: ['ticket_1', 'ticket_2'],
      },
    }
  }

  /**
   * GET /api/users/:userId/payment-methods
   * Get user's payment methods
   */
  async getPaymentMethods(
    userId: string
  ): Promise<APIResponse<PaymentMethod[]>> {
    await this.delay()
    void userId
    return {
      success: true,
      data: mockPaymentMethods,
    }
  }

  /**
   * POST /api/users/:userId/payment-methods
   * Add payment method
   */
  async addPaymentMethod(
    userId: string,
    data: Partial<PaymentMethod>
  ): Promise<APIResponse<PaymentMethod>> {
    await this.delay()
    void userId
    const newMethod: PaymentMethod = {
      id: `payment_${Date.now()}`,
      type: data.type || 'credit_card',
      isDefault: false,
      cardLast4: data.cardLast4 || '5678',
    }
    return {
      success: true,
      data: newMethod,
    }
  }

  // ========================================
  // VALIDATION ROUTES
  // ========================================

  /**
   * POST /api/validation/validate-ticket
   * Validate ticket (on boarding)
   */
  async validateTicket(data: {
    ticketId: string
    vehicleId: string
  }): Promise<
    APIResponse<{
      valid: boolean
      message: string
      remainingValidityTime: number
    }>
  > {
    await this.delay()
    void data
    return {
      success: true,
      data: {
        valid: true,
        message: 'Ticket validated successfully',
        remainingValidityTime: 12 * 60 * 60 * 1000, // 12 hours
      },
    }
  }
}

export const mockAPI = new MockAPIService()
