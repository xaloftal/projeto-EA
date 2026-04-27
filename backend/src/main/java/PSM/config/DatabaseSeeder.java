package PSM.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import PSM.Location.Location;
import PSM.Location.Route;
import PSM.Location.Stop;
import PSM.Location.StopSchedule;
import PSM.Location.Zone;
import PSM.Location.api.location.LocationRepository;
import PSM.Location.api.route.RouteRepository;
import PSM.Location.api.stop.StopRepository;
import PSM.Location.api.stopschedule.StopScheduleRepository;
import PSM.Location.api.zone.ZoneRepository;
import PSM.Travel.VehicleType;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Path DATA_DIR = Path.of(
            System.getenv().getOrDefault("SCRIPT_MOCK_DATA_DIR",
                    "/../../../../../script_mock_data/data"));
    private static final Path ROUTES_CSV = DATA_DIR.resolve("routes.csv");
    private static final Path ZONES_CSV = DATA_DIR.resolve("zones.csv");
    private static final Path STOPS_CSV = DATA_DIR.resolve("stops.csv");
    private static final Path STOP_SCHEDULES_CSV = DATA_DIR.resolve("schedule.csv");
    private static final LocalDate SEED_BASE_DATE = LocalDate.of(2000, 1, 1);
    
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    private static final int COL_STOP_ID = 0;
    private static final int COL_STOP_NAME = 1;
    private static final int COL_STOP_TYPE = 2;
    private static final int COL_STOP_CODE = 3;
    private static final int COL_LOCATION_ID = 4;
    private static final int COL_LATITUDE = 5;
    private static final int COL_LONGITUDE = 6;
    private static final int COL_ZONE_ID = 7;

    private final LocationRepository locationRepository;
    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final StopScheduleRepository stopScheduleRepository;
    private final ZoneRepository zoneRepository;

    public DatabaseSeeder(
            LocationRepository locationRepository,
            StopRepository stopRepository,
            RouteRepository routeRepository,
            StopScheduleRepository stopScheduleRepository,
            ZoneRepository zoneRepository) {
        this.locationRepository = locationRepository;
        this.stopRepository = stopRepository;
        this.routeRepository = routeRepository;
        this.stopScheduleRepository = stopScheduleRepository;
        this.zoneRepository = zoneRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (routeRepository.count() > 0 || zoneRepository.count() > 0 || stopRepository.count() > 0 || stopScheduleRepository.count() > 0) {
            return;
        }

        seedFromCsv();
    }

    private void seedFromCsv() {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        
        try {
            CompletableFuture<RoutesData> routesFuture = CompletableFuture.supplyAsync(this::loadRoutes, executor);
            CompletableFuture<ZonesData> zonesFuture = CompletableFuture.supplyAsync(this::loadZones, executor);
            CompletableFuture<StopsData> stopsFuture = CompletableFuture.supplyAsync(this::loadStops, executor);

            RoutesData routesData = routesFuture.join();
            ZonesData zonesData = zonesFuture.join();
            StopsData stopsData = stopsFuture.join();

            zoneRepository.saveAll(zonesData.zonesToSave);

            List<Location> savedLocations = locationRepository.saveAll(stopsData.locationsToSave.values());
            Map<String, Location> savedLocationsById = new LinkedHashMap<>();
            int locationIndex = 0;
            for (String locationId : stopsData.locationsToSave.keySet()) {
                savedLocationsById.put(locationId, savedLocations.get(locationIndex++));
            }

            for (Stop stop : stopsData.stopsToSave) {
                String locationId = stopsData.stopCodeToLocationId.get(stop.getStopCode());
                String zoneId = stopsData.stopCodeToZoneId.get(stop.getStopCode());

                Location savedLocation = savedLocationsById.get(locationId);
                if (savedLocation == null) {
                    throw new IllegalStateException("Unknown location_id in stops.csv: " + locationId);
                }

                Zone zone = zonesData.zonesByCode.get(zoneId);
                if (zone == null) {
                    throw new IllegalStateException("Unknown zone_id in stops.csv: " + zoneId);
                }

                stop.setLocation(savedLocation);
                stop.setZone(zone);
                zone.getStops().add(stop);
            }

            stopRepository.saveAll(stopsData.stopsToSave);

            loadStopSchedules(routesData.routesByCode, stopsData.stopsByCode);
            routeRepository.saveAll(routesData.routesToSave);
        } finally {
            executor.shutdown();
        }
    }

    private RoutesData loadRoutes() {
        Map<String, Route> routesByCode = new LinkedHashMap<>();
        List<Route> routesToSave = new ArrayList<>();

        for (String[] row : readCsv(ROUTES_CSV)) {
            if (row.length < 2) {
                throw new IllegalStateException("Invalid routes.csv row. Expected id,name");
            }

            String routeId = row[0].trim();
            String routeCode = row[1].trim();

            Route route = new Route();
            route.setName(routeCode);

            // Support schedule rows that reference either route id or route code.
            routesByCode.put(routeId, route);
            routesByCode.put(routeCode, route);
            routesToSave.add(route);
        }

        if (routesByCode.isEmpty()) {
            throw new IllegalStateException("routes.csv is empty");
        }

        return new RoutesData(routesByCode, routesToSave);
    }

    private ZonesData loadZones() {
        Map<String, Zone> zonesByCode = new LinkedHashMap<>();
        List<Zone> zonesToSave = new ArrayList<>();

        for (String[] row : readCsv(ZONES_CSV)) {
            if (row.length < 3) {
                throw new IllegalStateException("Invalid zones.csv row. Expected id,name,colorHexCode");
            }

            String zoneId = row[0].trim();
            String zoneName = row[1].trim();
            String colorHexCode = row[2].trim();

            Zone zone = new Zone();
            zone.setName(zoneName);
            zone.setColorHexCode(colorHexCode);
            zonesByCode.put(zoneId, zone);
            zonesByCode.put(zoneName, zone);
            zonesToSave.add(zone);
        }

        if (zonesToSave.isEmpty()) {
            throw new IllegalStateException("zones.csv is empty");
        }

        return new ZonesData(zonesByCode, zonesToSave);
    }

    private static class RoutesData {
        final Map<String, Route> routesByCode;
        final List<Route> routesToSave;

        RoutesData(Map<String, Route> routesByCode, List<Route> routesToSave) {
            this.routesByCode = routesByCode;
            this.routesToSave = routesToSave;
        }
    }

    private static class ZonesData {
        final Map<String, Zone> zonesByCode;
        final List<Zone> zonesToSave;

        ZonesData(Map<String, Zone> zonesByCode, List<Zone> zonesToSave) {
            this.zonesByCode = zonesByCode;
            this.zonesToSave = zonesToSave;
        }
    }

    private static class StopsData {
        final Map<String, Location> locationsToSave;
        final List<Stop> stopsToSave;
        final Map<String, Stop> stopsByCode;
        final Map<String, String> stopCodeToLocationId;
        final Map<String, String> stopCodeToZoneId;

        StopsData(
                Map<String, Location> locationsToSave,
                List<Stop> stopsToSave,
                Map<String, Stop> stopsByCode,
                Map<String, String> stopCodeToLocationId,
                Map<String, String> stopCodeToZoneId) {
            this.locationsToSave = locationsToSave;
            this.stopsToSave = stopsToSave;
            this.stopsByCode = stopsByCode;
            this.stopCodeToLocationId = stopCodeToLocationId;
            this.stopCodeToZoneId = stopCodeToZoneId;
        }
    }

    private StopsData loadStops() {
        Map<String, Location> locationsToSave = new LinkedHashMap<>();
        List<Stop> stopsToSave = new ArrayList<>();
        Map<String, Location> locationsByCode = new LinkedHashMap<>();
        Map<String, Stop> stopsByCode = new LinkedHashMap<>();
        Map<String, String> stopCodeToLocationId = new LinkedHashMap<>();
        Map<String, String> stopCodeToZoneId = new LinkedHashMap<>();

        for (String[] row : readCsv(STOPS_CSV)) {
            if (row.length <= COL_ZONE_ID) {
                throw new IllegalStateException(
                        "Invalid stops.csv row. Expected id,name,stop_type,stop_code,location_id,latitude,longitude,zone_id");
            }

            String stopId = row[COL_STOP_ID].trim();
            String stopCode = row[COL_STOP_CODE].trim();
            String stopName = row[COL_STOP_NAME].trim();
            VehicleType stopType = parseVehicleType(row[COL_STOP_TYPE]);
            String locationId = row[COL_LOCATION_ID].trim();

            // Create location if not already processed
            if (!locationsByCode.containsKey(locationId)) {
                double latitude = Double.parseDouble(row[COL_LATITUDE].trim());
                double longitude = Double.parseDouble(row[COL_LONGITUDE].trim());

                Location location = new Location();
                location.setLatitude(latitude);
                location.setLongitude(longitude);
                locationsToSave.put(locationId, location);
                locationsByCode.put(locationId, location);
            }

            // Create stop (will link to zone later)
            Location location = locationsByCode.get(locationId);
            Stop stop = new Stop();
            stop.setName(stopName);
            stop.setStopCode(stopCode);
            stop.setStopType(stopType);
            stop.setLocation(location);

            stopsByCode.put(stopId, stop);
            stopsByCode.put(stopCode, stop);
            stopsToSave.add(stop);
            stopCodeToLocationId.put(stopCode, locationId);
            stopCodeToZoneId.put(stopCode, row[COL_ZONE_ID].trim());
        }

        if (locationsToSave.isEmpty() || stopsToSave.isEmpty()) {
            throw new IllegalStateException("No locations or stops found in stops.csv");
        }

        return new StopsData(locationsToSave, stopsToSave, stopsByCode, stopCodeToLocationId, stopCodeToZoneId);
    }

    private void loadStopSchedules(Map<String, Route> routesByCode, Map<String, Stop> stopsByCode) {
        for (String[] row : readCsv(STOP_SCHEDULES_CSV)) {
            if (row.length < 6) {
                throw new IllegalStateException("Invalid schedule.csv row. Expected id,arrival_time,departure_time,sequence,stop_id,route_id");
            }

            LocalDateTime arrivalTime = LocalDateTime.of(SEED_BASE_DATE, parseTime(row[1]));
            LocalDateTime departureTime = LocalDateTime.of(SEED_BASE_DATE, parseTime(row[2]));
            int sequence = Integer.parseInt(row[3].trim());
            String stopCode = row[4].trim();
            String routeCode = row[5].trim();

            Route route = routesByCode.get(routeCode);
            if (route == null) {
                throw new IllegalStateException("Unknown route_code in stop_schedules.csv: " + routeCode);
            }

            Stop stop = stopsByCode.get(stopCode);
            if (stop == null) {
                throw new IllegalStateException("Unknown stop_code in stop_schedules.csv: " + stopCode);
            }

            StopSchedule schedule = new StopSchedule();
            schedule.setArrivalTime(arrivalTime);
            schedule.setDepartureTime(departureTime);
            schedule.setSequence(sequence);
            schedule.stop = stop;
            route.schedules.add(schedule);
        }
    }

    private List<String[]> readCsv(Path filePath) {
        if (!Files.exists(filePath)) {
            throw new IllegalStateException("CSV file not found: " + filePath);
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .map(this::stripBom)
                    .skip(1)
                    .map(this::splitCsvLine)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read CSV file: " + filePath, e);
        }
    }

    private String stripBom(String line) {
        if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }

    private LocalTime parseTime(String value) {
        return LocalTime.parse(value.trim());
    }

    private VehicleType parseVehicleType(String raw) {
        if (raw == null || raw.isBlank()) {
            return VehicleType.BUS;
        }
        return VehicleType.valueOf(raw.trim().toUpperCase());
    }

    private String[] splitCsvLine(String line) {
        return line.split(",", -1);
    }
}