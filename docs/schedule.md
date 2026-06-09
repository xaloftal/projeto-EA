Lazy Loading Schedules and Fixing Search
The user wants to implement lazy loading for the ScheduleView to avoid loading massive amounts of schedule data upfront, and fix the search issue where "801" might not yield the expected route.

Open Questions
None at this time.

User Review Required
IMPORTANT

The backend endpoints and DTOs will be refactored to support lazy loading. The frontend will hit the summary endpoint on initial load and only fetch the full times table when a route or stop is explicitly selected.

Search Fix: The previous search cut off results at the top 12. If "801" wasn't in the top 12 (due to matching many stops), it wouldn't show up. We will prioritize exact route name matches and stop code matches to ensure relevant items appear at the top.

Proposed Changes
Backend
[NEW] PSM.Location.api.route.RouteSummaryDTO.java
Create a DTO to represent a lightweight route containing its ID, name, and a distinct list of its stops (ID, name, code, type).
[MODIFY] PSM.Location.api.route.RouteRepository.java
Add @Query for findByIdWithSchedules(UUID routeId) to load eager schedules for a single route.
[MODIFY] PSM.Location.api.route.RouteService.java
Add getRouteSummaries(): Fetches all routes and their stops without trip times.
Add findRouteScheduleOptimized(UUID routeId): Returns the full timetable for a single route.
Add findStopScheduleOptimized(UUID stopId): Returns the timetable for a single stop (times of all routes passing through).
[MODIFY] PSM.Location.api.route.RouteController.java
Expose GET /api/routes/summary
Expose GET /api/routes/{id}/schedule
Expose GET /api/stops/{id}/schedule
Frontend
[MODIFY] catchitApi.ts
Replace getRouteSchedules() with getRouteSummaries().
Add getRouteSchedule(routeId) and getStopSchedule(stopId).
[MODIFY] scheduleViewModel.ts
Refactor state to hold lightweight RouteSummary and StopSummary lists.
Update filteredRoutes and filteredStops search logic to sort exact matches (like "801") to the top of the list before applying the .slice(0, 12) limit.
Add reactive async logic loadRouteTimetable(routeId) and loadStopTimetable(stopId) when a user selects an item.
[MODIFY] ScheduleView.vue
Update UI to show a loading state specifically for the timetable grid.
Integrate the new selection logic to trigger the backend calls.
Verification Plan
Automated Tests
Restart backend container with docker compose restart catchit-backend to test the new endpoints.
Manual Verification
Go to the Schedule View. It should load significantly faster.
Search for "801". The route should appear at the top of the results.
Select "801". A loading spinner should briefly appear, then the full timetable should display.
Clear and search for a stop, then select it. The schedule for that stop should accurately render.