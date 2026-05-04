# Vehicle Simulation Integration

## Overview

This document explains the vehicle simulation feature that was implemented across backend and frontend.

Goal:
- Simulate vehicle movement along route schedules on the backend
- Expose current simulated positions through an authenticated REST endpoint
- Render moving vehicle pins in the frontend map using periodic polling

## Backend Implementation

### Endpoints

- `GET /api/vehicles/simulation`
  - Returns a list of simulation snapshots (one per tracked vehicle)
- `GET /api/vehicles/simulation/{vehicleId}`
  - Returns the snapshot for a specific vehicle

Both endpoints are protected by the existing authentication layer and require `Authorization: Bearer <token>`.

### Core Service

File: `backend/src/main/java/PSM/Travel/api/vehicle/VehicleSimulationService.java`

Key responsibilities:
- Load routes and route schedules
- Build a route track (ordered points + time offsets)
- Map persisted vehicles to route tracks (`vehicle.getRoute() != null` is required)
- Recompute snapshot positions on a fixed tick
- Return latest in-memory snapshots

Scheduling behavior:
- Refresh tracks: `simulation.vehicle.refresh-ms` (default `30000`)
- Tick snapshots: `simulation.vehicle.tick-ms` (default `1000`)

Position algorithm summary:
1. Sort schedules by `sequence`
2. Convert each stop schedule to a point with cumulative `offsetSeconds`
3. Compute total route duration from final point offset
4. For each vehicle, apply a deterministic hash-based offset
5. Use linear interpolation (`lerp`) between previous and next stop based on elapsed time
6. Return snapshot DTO with location, route, stop context and progress

### DTO Contract

File: `backend/src/main/java/PSM/Travel/api/vehicle/VehicleSimulationSnapshotDTO.java`

Payload fields:
- `vehicleId`
- `routeId`
- `routeName`
- `latitude`
- `longitude`
- `previousStopId`
- `previousStopName`
- `nextStopId`
- `nextStopName`
- `progress`
- `updatedAt`

## Frontend Implementation

### API Client Additions

File: `frontend/catchit-mobile/src/services/api/catchitApi.ts`

Added:
- `BackendVehicleSimulationSnapshot` type
- `getVehicleSimulation()` method calling `/api/vehicles/simulation`

The method uses the shared `requestJson` helper, so Bearer token is automatically included from `localStorage.authToken`.

### Map Integration

File: `frontend/catchit-mobile/src/views/MapView.vue`

Changes made:
- Added simulation snapshot import/type usage from API client
- Added `simulationBuses` state and switched active bus source to backend simulation data
- Added mapper from backend snapshot -> map bus model
- Added periodic polling (`simulationPollMs = 3000`)
- Starts polling on mount and updates markers continuously
- Keeps selected bus in sync; clears selection if bus disappears
- Ensures selected bus arrow layer is created and updated from real snapshot positions

Behavior now:
- Stop pins remain circle markers (performance optimization)
- Vehicle pins are rendered from backend simulation snapshots
- Vehicle positions move over time according to backend tick updates

## Authentication

Simulation requests require Bearer auth.

Header format:
- `Authorization: Bearer <token>`

The frontend already injects this header automatically for authenticated users.

## How to Test

### 1) Ensure backend is running

Example:

```bash
cd backend
mvn spring-boot:run
```

### 2) Ensure there are vehicles assigned to routes

Simulation is empty if vehicles are not linked to a route.

Requirement:
- `vehicle.route` must not be `null`

### 3) Validate API manually

```bash
curl -s -H "Authorization: Bearer <token>" http://localhost:8080/api/vehicles/simulation | jq .
```

Expected:
- Array with at least one object containing latitude/longitude/progress

### 4) Run frontend

```bash
cd frontend/catchit-mobile
npm install
npm run dev
```

Open map view and confirm:
- Vehicle markers appear
- Markers update position over time
- Clicking a vehicle opens the bottom sheet with route/next stop info

## Validation Performed

Frontend checks executed after integration:

- `npm run lint`
- `npm run build`

Result:
- Both succeeded

## Notes and Limitations

- If `/api/vehicles/simulation` returns `[]`, the frontend shows no moving vehicles (expected behavior).
- Simulation currently uses polling (3 seconds), not WebSocket push.
- For larger fleets, consider viewport-based filtering and incremental marker updates.

## Suggested Next Improvements

1. Replace polling with WebSocket/SSE for lower latency and less network overhead.
2. Add a backend fallback mode for demo data when there are no persisted vehicles.
3. Expose simulation health/metrics endpoint (tracked vehicles, last tick time).
4. Add frontend status badge (e.g. "Simulation connected / empty / error").
