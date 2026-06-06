export type TransitMode = 'WALK' | 'BUS' | 'RAIL' | 'SUBWAY' | 'TRAM' | 'METRO'

export interface RoutingLegProperties {
  legIndex: number
  mode: TransitMode
  routeShortName: string
  routeLongName: string
  routeColor: string
  fromStop: string
  toStop: string
  fromStopCode: string
  toStopCode: string
  startTime: number
  endTime: number
  distanceMeters: number
  isTransitLeg: boolean
}

export interface RoutingLegFeature {
  type: 'Feature'
  geometry: {
    type: 'LineString'
    coordinates: [number, number][]
  }
  properties: RoutingLegProperties
}

export interface RoutingPlanSummary {
  durationSeconds: number
  transfers: number
  walkDistanceMeters: number
  startTime: number
  endTime: number
}

export interface RoutingPlanResponse {
  type: 'FeatureCollection'
  features: RoutingLegFeature[]
  summary?: RoutingPlanSummary
  error?: string
}

export interface RoutingPlanRequest {
  fromLat: number
  fromLon: number
  toLat: number
  toLon: number
}
