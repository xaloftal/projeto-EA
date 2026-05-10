# Real Data and Map Features README

## Overview

This update delivers a full real-data transport flow for routes, stops, and schedules, replacing the older mock-generation approach and improving the map experience with arrival-aware stop details.

The result is a system where backend, frontend, and seed data are aligned around real scraped datasets.

## User-facing features

### 1. Stop-level next arrival information

- The map stop panel now shows the next expected pass time for routes serving that stop.
- Arrival labels are presented as both timestamp and relative countdown (for example, "in 9m").
- Past/stale arrivals are filtered out in the UI.

### 2. Public map data flow

- Core map data endpoints are available without authentication, enabling map browsing and stop info flows for non-logged users.

### 3. Improved stop popup usability

- Stop detail content is now centered on arrival information and route relevance at the selected stop.

## Backend feature set

### 1. Route-stop sequencing model

- Route and stop linkage now uses an explicit sequence model through `RouteStop`.
- This enables deterministic stop ordering for route traversal and arrival calculations.

### 2. Arrival computation API

- Added stop-arrival endpoint:
  - `GET /api/routes/stop-arrivals?stopId=<uuid>`
- Returns next upcoming arrival per route for the requested stop.

### 3. Timezone-safe upcoming logic

- Next-arrival logic computes against `Europe/Lisbon`.
- Returned `nextArrivalAt` values are offset-aware (`OffsetDateTime`) to prevent timezone parsing drift.
- Computation enforces strictly future occurrences.

### 4. Optimized route/schedule loading

- Repository queries and fetch strategy were updated for route + schedule + stop graph loading needed by map and arrivals.

## Frontend feature set

### 1. API integration for stop arrivals

- Added typed client method for `GET /api/routes/stop-arrivals`.
- `MapView` now uses this endpoint to render route arrival cards in the stop sheet.

### 2. Arrival-centric stop header

- Stop header now displays `Next arrival` based on computed route arrivals rather than static stop progression.

### 3. Countdown rendering behavior

- Arrival countdown is recalculated continuously from current time to keep labels up to date.

## Real-data pipeline features

### 1. Scraping scripts

- Added route-stop and schedule scrapers:
  - `script_mock_data/routes_stops_scrapper.py`
  - `script_mock_data/schedule_scrapper.py`

### 2. Merge and diagnostics utilities

- Added data join and validation helpers:
  - `script_mock_data/join_data.py`
  - `script_mock_data/diagnose_merge.py`
  - `script_mock_data/merge_test.py`

### 3. Dataset shape migration

- Moved from split mock files (`routes.csv`, `stops.csv`, `zones.csv`) to merged/real-data-oriented files, especially:
  - `stops_routes_zones.csv`
  - updated `schedule.csv`
  - `route_gtfs_map.csv`

## Seeder and schema features

### 1. Seeder rewrite for real datasets

- `DatabaseSeeder` now seeds directly from merged route-stop-zone and schedule files.
- Includes normalization, defensive matching, and schedule parsing for extended-hour times.

### 2. Schema support

- Added schema creation SQL in `db/01-create-schemas.sql`.

## Docker/runtime reliability features

### 1. Backend readiness healthcheck

- Backend container now exposes readiness through HTTP health probing.

### 2. Safer startup ordering

- Frontend waits for backend health rather than only container start signal.
- This reduces startup-time 502 windows during backend warmup.

## Validation summary

Feature validation included:

- Backend compilation after arrival/model changes.
- Endpoint checks:
  - `GET /api/stops/geojson`
  - `GET /api/routes/stop-arrivals?stopId=<id>`
- Runtime verification that returned arrival timestamps are future-oriented.

## Key implementation files

- Backend:
  - `backend/src/main/java/PSM/Location/RouteStop.java`
  - `backend/src/main/java/PSM/Location/api/routestop/RouteStopRepository.java`
  - `backend/src/main/java/PSM/Location/api/route/RouteService.java`
  - `backend/src/main/java/PSM/Location/api/route/RouteController.java`
  - `backend/src/main/java/PSM/Location/api/route/StopRouteArrivalDTO.java`
  - `backend/src/main/java/PSM/config/DatabaseSeeder.java`
  - `backend/src/main/java/PSM/config/SecurityConfig.java`

- Frontend:
  - `frontend/catchit-mobile/src/views/MapView.vue`
  - `frontend/catchit-mobile/src/services/api/catchitApi.ts`
  - `frontend/catchit-mobile/src/router/index.ts`

- Data/scripts:
  - `script_mock_data/routes_stops_scrapper.py`
  - `script_mock_data/schedule_scrapper.py`
  - `script_mock_data/data/stops_routes_zones.csv`
  - `script_mock_data/data/schedule.csv`

- Infra:
  - `docker-compose.yml`
  - `backend/Dockerfile`
