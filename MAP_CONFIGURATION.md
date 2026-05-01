# Map Configuration and GeoJSON Service Guide

## Overview
The map has been refactored to use a GeoJSON-based approach that significantly reduces frontend resource usage and improves mobile performance. The backend now pre-calculates and serves all spatial data as optimized GeoJSON.

## Architecture

### Backend GeoJSON Generation
- **Service**: [PSM/Location/api/stop/GeoJsonGeneratorService.java](PSM/Location/api/stop/GeoJsonGeneratorService.java)
- **Endpoint**: `GET /api/stops/geojson`
- **Purpose**: Converts all stops from the database into a GeoJSON FeatureCollection
- **Benefits**: 
  - No frontend processing required
  - Reduced payload size with optimized structure
  - Pre-validated coordinates
  - Mobile-optimized data format

### Frontend Integration
- **Component**: [src/views/MapView.vue](src/views/MapView.vue)
- **API Method**: `catchitApi.getStopsGeoJson()` in [src/services/api/catchitApi.ts](src/services/api/catchitApi.ts)
- **Map Library**: Leaflet.js with OpenStreetMap tiles

## Map Configuration Reference

### 1. Map Bounds (Region Restriction)
**Location**: [frontend/catchit-mobile/src/views/MapView.vue](frontend/catchit-mobile/src/views/MapView.vue#L640)

```typescript
const portugallNorthBounds = L.latLngBounds([40.5, -9.0], [42.0, -7.5])
```

**Current Configuration**:
- **Southwest corner**: 40.5°N, 9.0°W
- **Northeast corner**: 42.0°N, 7.5°W
- **Region**: Portugal North (Porto area and surrounding regions)

**To Modify**:
1. Change the latitude/longitude values in `L.latLngBounds()`
2. Format: `[latitude, longitude]`
3. Southwest corner has smaller coordinates; Northeast corner has larger coordinates

**Examples**:
- Central Portugal: `L.latLngBounds([39.5, -9.5], [41.5, -6.5])`
- All of Portugal: `L.latLngBounds([37.0, -10.0], [42.0, -6.0])`

### 2. Zoom Level Constraints
**Location**: [frontend/catchit-mobile/src/views/MapView.vue](frontend/catchit-mobile/src/views/MapView.vue#L645)

```typescript
map = L.map(mapContainer.value, {
  minZoom: 10,  // Minimum zoom level (zoomed out most)
  maxZoom: 18,  // Maximum zoom level (zoomed in most)
})
```

**Current Configuration**:
- **minZoom: 10** → Prevents zooming out beyond Portugal North region
- **maxZoom: 18** → Allows detailed street-level zoom

**Zoom Level Guide**:
- 1-5: World view (too far out)
- 10-13: City/region overview (recommended minimum for Portugal North)
- 14-16: Street-level detail
- 17-18: Ultra-detailed, individual buildings
- 19-20: Sometimes available but not recommended

**To Modify Zoom**:
- Increase `minZoom` to zoom in more (e.g., 12 for city-only view)
- Decrease `minZoom` to allow more zoom out (e.g., 8 for larger regions)
- Adjust `maxZoom` for maximum detail level

### 3. Initial Map View
**Location**: [frontend/catchit-mobile/src/views/MapView.vue](frontend/catchit-mobile/src/views/MapView.vue#L650)

```typescript
.setView([41.4057, -8.5332], 13)
```

**Current Configuration**:
- **Center**: 41.4057°N, 8.5332°W (Porto, Portugal)
- **Initial Zoom**: 13 (good for city overview)

**To Change Initial View**:
1. Change coordinates: `[latitude, longitude]`
2. Adjust zoom level (third parameter)

### 4. Navigation Restrictions
**Location**: [frontend/catchit-mobile/src/views/MapView.vue](frontend/catchit-mobile/src/views/MapView.vue#L645)

```typescript
maxBounds: portugallNorthBounds,
maxBoundsViscosity: 1.0
```

**Current Configuration**:
- **maxBoundsViscosity: 1.0** → Hard boundary (no bouncing past limits)

**To Allow Bouncing Beyond Bounds**:
- Change `maxBoundsViscosity` to a value < 1.0 (e.g., 0.5 for soft boundary)
- Value of 0 = no resistance; 1.0 = hard boundary

## GeoJSON Caching System

### Overview
The GeoJSON is now pre-calculated on application startup and cached to disk. This means:
- Map loads **instantly** without database queries
- Zero processing time after first request
- Scales to any number of stops without performance degradation

### Cache Architecture

#### Services
1. **CachedGeoJsonService** ([PSM/Location/api/stop/CachedGeoJsonService.java](PSM/Location/api/stop/CachedGeoJsonService.java))
   - Manages GeoJSON cache storage and retrieval
   - Writes cache to: `./cache/stops-geojson.json`
   - Automatically falls back to generating if cache unavailable

2. **GeoJsonCacheInitializer** ([PSM/Location/api/stop/GeoJsonCacheInitializer.java](PSM/Location/api/stop/GeoJsonCacheInitializer.java))
   - Runs on application startup (after Spring context ready)
   - Pre-generates and caches GeoJSON before first request
   - Logs cache generation time and location

#### Startup Flow
```
Application Starts
    ↓
Spring Context Initialized
    ↓
ApplicationReadyEvent triggered
    ↓
GeoJsonCacheInitializer generates cache
    ↓
Frontend requests /api/stops/geojson
    ↓
CachedGeoJsonService serves from disk (instant)
```

### Cache Endpoints

#### GET `/api/stops/geojson`
Serves the pre-cached GeoJSON FeatureCollection.
- **Response**: Cached GeoJSON (instant delivery)
- **Size**: ~10-50KB (typical for hundreds of stops)
- **Cache-Hit**: <1ms

#### POST `/api/stops/geojson/refresh`
Manually triggers cache regeneration. Useful after bulk stop updates.
- **Response**: 
  ```json
  {
    "success": true,
    "message": "GeoJSON cache refreshed",
    "cacheLocation": "/path/to/cache/stops-geojson.json",
    "cacheSize": 12345
  }
  ```
- **Use Case**: After adding/removing stops programmatically

#### GET `/api/stops/geojson/status`
Checks cache status and location (for debugging).
- **Response**:
  ```json
  {
    "cached": true,
    "cacheLocation": "/path/to/cache/stops-geojson.json",
    "cacheSize": "12345 bytes"
  }
  ```

### Cache Location
- **Directory**: `./cache/` (relative to application working directory)
- **Filename**: `stops-geojson.json`
- **Full Path Example**: `/opt/app/cache/stops-geojson.json` (Docker)
- **Full Path Example**: `C:\app\cache\stops-geojson.json` (Windows)

### Cache Persistence

#### Docker / Production
Cache is created in the running container at startup:
```bash
# Cache will be generated when container starts
./cache/stops-geojson.json
```

For persistence across restarts, mount a volume:
```yaml
# docker-compose.yml
services:
  backend:
    volumes:
      - ./cache:/app/cache  # Persist cache
```

#### Local Development
Cache is created in your project root:
```
projeto-EA/
├── cache/
│   └── stops-geojson.json  ← Cache file created here
├── backend/
├── frontend/
└── ...
```

### Performance Impact

| Scenario | Time |
|---|---|
| First request (cache generation) | 50-200ms |
| Subsequent requests (cache hit) | <1ms |
| Cache file size | ~10-50KB |
| Download time on 3G | ~50-200ms |

### Manual Cache Management

#### Refresh Cache (Programmatically)
```bash
curl -X POST http://localhost:8080/api/stops/geojson/refresh
```

#### Check Cache Status
```bash
curl http://localhost:8080/api/stops/geojson/status
```

#### View Cache File
```bash
# Unix/Linux/Mac
cat cache/stops-geojson.json | jq '.'

# Windows PowerShell
Get-Content cache\stops-geojson.json | ConvertFrom-Json
```

#### Clear Cache Manually
```bash
rm cache/stops-geojson.json
# Cache will be regenerated on next application start
```

### Troubleshooting

#### Cache Not Generated
**Problem**: Cache file doesn't exist after startup

**Solution**:
1. Check application logs for warnings
2. Ensure `./cache/` directory is writable
3. Verify database contains stops with valid coordinates
4. Manually refresh: `curl -X POST http://localhost:8080/api/stops/geojson/refresh`

#### Cache File Growing Too Large
**Problem**: Cache file is larger than expected

**Solution**:
1. Check stop count: `SELECT COUNT(*) FROM stop;`
2. Check for duplicate coordinates
3. Verify stop properties are necessary in GeoJSON

#### Cache Not Updating After Stop Changes
**Problem**: Map doesn't show new stops

**Solution**:
1. Add the stop with valid coordinates
2. Manually refresh cache: `curl -X POST http://localhost:8080/api/stops/geojson/refresh`
3. Or wait for next application restart

## GeoJSON Endpoint Details

### Request
```
GET /api/stops/geojson
```

### Response Format
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [-8.5332, 41.4057]
      },
      "properties": {
        "id": "stop-uuid",
        "name": "Stop Name",
        "code": "123",
        "stopType": "STOP",
        "latitude": 41.4057,
        "longitude": -8.5332
      }
    }
  ]
}
```

**Note**: GeoJSON uses `[longitude, latitude]` order in coordinates, but properties include both for convenience.

## Performance Benefits

1. **Reduced Frontend Processing**: Backend generates GeoJSON; frontend only renders
2. **Optimized Data Format**: Only necessary fields are sent
3. **Lazy Loading**: No need to load routes before displaying stops
4. **Mobile Optimized**: Minimal data transmission and processing
5. **Caching Friendly**: Static GeoJSON can be cached by browser/CDN

## Modifying Stop Information in GeoJSON

To add or remove properties from the GeoJSON output, edit:
[PSM/Location/api/stop/GeoJsonGeneratorService.java](PSM/Location/api/stop/GeoJsonGeneratorService.java)

In the `createFeature()` method:
```java
Map<String, Object> properties = new HashMap<>();
properties.put("id", stop.getId().toString());
properties.put("name", stop.getName());
// Add new properties here
// properties.put("newField", value);
```

## Leaflet Documentation Reference

- [Leaflet Map Options](https://leafletjs.com/reference.html#map-option)
- [Leaflet Bounds](https://leafletjs.com/reference.html#latlngbounds)
- [Leaflet GeoJSON Layer](https://leafletjs.com/reference.html#geojson)

## Quick Configuration Changes Checklist

- [ ] Adjust bounds for different region? → Modify `portugallNorthBounds`
- [ ] Change minimum zoom out limit? → Modify `minZoom`
- [ ] Allow users to zoom in more? → Increase `maxZoom`
- [ ] Change initial center/zoom? → Modify `setView()` parameters
- [ ] Add stop properties to GeoJSON? → Edit `GeoJsonGeneratorService.java`
- [ ] Add caching headers to GeoJSON endpoint? → Add to `StopController`
