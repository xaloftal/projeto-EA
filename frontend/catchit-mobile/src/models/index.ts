// User related types
export interface User {
  id: string
  name: string
  email: string
  balance: number
  tickets: Ticket[]
}

// Ticket related types
export enum TicketStatus {
  PurchasedButNotValid = 'PURCHASED_BUT_NOT_VALID',
  Valid = 'VALID',
  Expired = 'EXPIRED',
  Used = 'USED',
}

export interface Ticket {
  id: string
  userID: string
  createdAt: Date
  validFrom: Date
  validUntil: Date
  price: number
  qrCode: string
  status: TicketStatus
  stopFrom: Stop
  stopTo: Stop
}

export interface TicketPack {
  id: string
  tickets: Ticket[]
}

export interface MonthlyPass extends TicketPack {
  startDate: Date
  endDate: Date
}

export interface OneWayTicket extends Ticket {
  startStop: Stop
  endStop: Stop
}

// Travel related types
export interface Travel {
  id: string
  userID: string
  startTime: Date
  endTime: Date
  startStop: Stop
  endStop: Stop
  vehicle?: Vehicle
  stops?: Stop[]
}

export interface Trip {
  id: string
  startTime: Date
  endTime: Date
  stops: Stop[]
  vehicle?: Vehicle
}

export interface Stop {
  id: string
  name: string
  latitude: number
  longitude: number
  schedule?: StopSchedule[]
}

export interface StopSchedule {
  stop: Stop
  arrivalTime: Date
  departureTime: Date
  sequence: number
}

export interface Location {
  id: string
  latitude: number
  longitude: number
  name?: string
}

export interface Route {
  id: string
  name: string
  schedule: StopSchedule[]
  getNextStop(): Stop | null
  getEstimatedArrival(): Date | null
}

// Vehicle related types
export interface Vehicle {
  id: string
  capacity: number
  currentPassengers: number
  updateLocation(): void
  notifyObservers(): void
}

export interface Subject {
  attach(observer: Observer): void
  detach(observer: Observer): void
  notify(): void
}

export interface Observer {
  update(subject: Subject): void
}

export interface POI {
  id: string
  name: string
  stores: Store[]
}

export interface Store {
  id: string
  name: string
  poiID: string
  stops: Stop[]
}

// Card related types
export type CardTier = 'weekly' | 'monthly' | 'yearly'

export interface Card {
  id: string
  userID: string
  name: string
  price: number
  description?: string
  validFrom: Date
  validUntil: Date
  tier?: CardTier
}

export interface TravelCard extends Card {
  monthlyPrice: number
  annualPrice: number
}

// Search and Navigation
export interface SearchQuery {
  from: Location
  to: Location
  departureDate: Date
  departureTime?: Date
}

export interface SearchResult {
  route: Route
  vehicle: Vehicle
  estimatedDuration: number
  price: number
}

// Checkout
export interface CheckoutSession {
  id: string
  userId: string
  items: CartItem[]
  subtotal: number
  taxes: number
  total: number
  paymentMethod?: PaymentMethod
  createdAt: Date
}

export interface CartItem {
  id: string
  type: 'ticket' | 'card'
  quantity: number
  price: number
  item: Ticket | Card
}

export interface PaymentMethod {
  id: string
  type: 'credit_card' | 'debit_card' | 'digital_wallet'
  isDefault: boolean
  cardLast4?: string
}

// Validation
export interface ValidationRecord {
  id: string
  userID: string
  timestamp: Date
  result: boolean
}

export interface ValidationStrategy {
  validate(ticket: Ticket, vehicle: Vehicle): boolean
  updateOccupancy(vehicle: Vehicle): void
}
