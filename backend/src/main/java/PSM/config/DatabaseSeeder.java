package PSM.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import PSM.Location.api.location.LocationRepository;
import PSM.Location.api.route.RouteRepository;
import PSM.Location.api.stop.StopRepository;
import PSM.Location.api.stopschedule.StopScheduleRepository;
import PSM.Travel.VehicleType;

//@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Path DATA_DIR = Path.of(
            System.getenv().getOrDefault("SCRIPT_MOCK_DATA_DIR",
                    "/../../../../../script_mock_data/data"));
    private static final Path ROUTES_CSV = DATA_DIR.resolve("routes.csv");
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

    private final LocationRepository locationRepository;
    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final StopScheduleRepository stopScheduleRepository;

    public DatabaseSeeder(
            LocationRepository locationRepository,
            StopRepository stopRepository,
            RouteRepository routeRepository,
            StopScheduleRepository stopScheduleRepository) {
        this.locationRepository = locationRepository;
        this.stopRepository = stopRepository;
        this.routeRepository = routeRepository;
        this.stopScheduleRepository = stopScheduleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (routeRepository.count() > 0 || stopRepository.count() > 0 || stopScheduleRepository.count() > 0) {
            return;
        }

        seedFromCsv();
    }

    private void seedFromCsv() {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        
        try {
            // Step 1: Load routes and locations in parallel (independent operations)
            CompletableFuture<Map<String, Route>> routesFuture = 
                CompletableFuture.supplyAsync(this::loadRoutes, executor);
            CompletableFuture<Map<String, Location>> locationsFuture = 
                CompletableFuture.supplyAsync(this::loadLocations, executor);
            
            // Wait for both to complete
            Map<String, Route> routesByCode = routesFuture.join();
            Map<String, Location> locationsByCode = locationsFuture.join();
            
            // Step 2: Load stops (depends on locations)
            Map<String, Stop> stopsByCode = loadStops(locationsByCode);
            
            // Step 3: Load stop schedules (depends on routes and stops)
            loadStopSchedules(routesByCode, stopsByCode);
            
            // Step 4: Save all routes
            routeRepository.saveAll(routesByCode.values());
        } finally {
            executor.shutdown();
        }
    }

    private Map<String, Route> loadRoutes() {
        Map<String, Route> routesByCode = new LinkedHashMap<>();

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
        }

        if (routesByCode.isEmpty()) {
            throw new IllegalStateException("routes.csv is empty");
        }

        return routesByCode;
    }

    private Map<String, Location> loadLocations() {
        Map<String, Location> locationsByCode = new LinkedHashMap<>();

        for (String[] row : readCsv(STOPS_CSV)) {
            if (row.length <= COL_LONGITUDE) {
                throw new IllegalStateException(
                        "Invalid stops.csv row. Expected id,name,stop_type,stop_code,location_id,latitude,longitude");
            }

            String locationId = row[COL_LOCATION_ID].trim();

            // Skip if already processed (in case of duplicate location_ids)
            if (locationsByCode.containsKey(locationId)) {
                continue;
            }

            double latitude = Double.parseDouble(row[COL_LATITUDE].trim());
            double longitude = Double.parseDouble(row[COL_LONGITUDE].trim());

            Location location = new Location();
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            locationsByCode.put(locationId, locationRepository.save(location));
        }

        if (locationsByCode.isEmpty()) {
            throw new IllegalStateException("No locations found in stops.csv");
        }

        return locationsByCode;
    }

    private Map<String, Stop> loadStops(Map<String, Location> locationsByCode) {
        Map<String, Stop> stopsByCode = new LinkedHashMap<>();

        for (String[] row : readCsv(STOPS_CSV)) {
            if (row.length <= COL_LONGITUDE) {
                throw new IllegalStateException(
                        "Invalid stops.csv row. Expected id,name,stop_type,stop_code,location_id,latitude,longitude");
            }

            String stopId = row[COL_STOP_ID].trim();
            String stopCode = row[COL_STOP_CODE].trim();
            String stopName = row[COL_STOP_NAME].trim();
            VehicleType stopType = parseVehicleType(row[COL_STOP_TYPE]);
            String locationId = row[COL_LOCATION_ID].trim();

            Location location = locationsByCode.get(locationId);
            if (location == null) {
                throw new IllegalStateException("Unknown location_id in stops.csv: " + locationId);
            }

            Stop stop = new Stop();
            stop.setName(stopName);
            stop.setStopType(stopType);
            stop.setLocation(location);

            Stop savedStop = stopRepository.save(stop);

            // Support schedule rows that reference either stop id or stop code.
            stopsByCode.put(stopId, savedStop);
            stopsByCode.put(stopCode, savedStop);
        }

        if (stopsByCode.isEmpty()) {
            throw new IllegalStateException("stops.csv is empty");
        }

        return stopsByCode;
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