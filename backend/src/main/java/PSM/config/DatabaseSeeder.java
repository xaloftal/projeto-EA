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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import PSM.Location.Location;
import PSM.Location.Route;
import PSM.Location.RouteStop;
import PSM.Location.Stop;
import PSM.Location.StopSchedule;
import PSM.Location.Zone;
import PSM.Location.api.location.LocationRepository;
import PSM.Location.api.route.RouteRepository;
import PSM.Location.api.routestop.RouteStopRepository;
import PSM.Location.api.stop.StopRepository;
import PSM.Location.api.stopschedule.StopScheduleRepository;
import PSM.Location.api.zone.ZoneRepository;
import PSM.Travel.VehicleType;

//@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);

    private static final Path DATA_DIR = resolveDataDir();
    private static final Path ROUTE_STOPS_CSV = DATA_DIR.resolve("stops_routes_zones.csv");
    private static final Path STOP_SCHEDULES_CSV = DATA_DIR.resolve("schedule.csv");

    private static final LocalDate SEED_BASE_DATE = LocalDate.of(2000, 1, 1);
    
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    private static final int COL_ROUTE_CODE = 0;
    private static final int COL_STOP_CODE = 1;
    private static final int COL_STOP_NAME = 2;
    private static final int COL_LATITUDE = 3;
    private static final int COL_LONGITUDE = 4;
    private static final int COL_ZONE_CODE = 5;
    private static final int COL_SEQUENCE = 6;
    private static final int COL_TRANSPORT_TYPE = 7;
    private static final int COL_HEX_COLOR = 9;

    private static final int COL_SCHEDULE_ARRIVAL = 0;
    private static final int COL_SCHEDULE_DEPARTURE = 1;
    private static final int COL_SCHEDULE_STOP_CODE = 2;
    private static final int COL_SCHEDULE_ROUTE_CODE = 3;

    private final LocationRepository locationRepository;
    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final StopScheduleRepository stopScheduleRepository;
    private final ZoneRepository zoneRepository;

    public DatabaseSeeder(
            LocationRepository locationRepository,
            StopRepository stopRepository,
            RouteRepository routeRepository,
            RouteStopRepository routeStopRepository,
            StopScheduleRepository stopScheduleRepository,
            ZoneRepository zoneRepository) {
        this.locationRepository = locationRepository;
        this.stopRepository = stopRepository;
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.stopScheduleRepository = stopScheduleRepository;
        this.zoneRepository = zoneRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("DatabaseSeeder.run() called");

        long routeCount = routeRepository.count();
        long routeStopCount = routeStopRepository.count();
        long zoneCount = zoneRepository.count();
        long stopCount = stopRepository.count();
        long scheduleCount = stopScheduleRepository.count();

        logger.info(
                "Database counts - routes: {}, routeStops: {}, zones: {}, stops: {}, schedules: {}",
                routeCount,
                routeStopCount,
                zoneCount,
                stopCount,
                scheduleCount);

        if (routeCount > 0 || routeStopCount > 0 || zoneCount > 0 || stopCount > 0 || scheduleCount > 0) {
            logger.info("Database already seeded, skipping...");
            return;
        }

        logger.info("Starting database seeding from CSV files");
        seedFromCsv();
    }

    private void seedFromCsv() {
        try {
            logger.info("Loading route/stops/zones data from CSV...");
            RouteStopsData routeStopsData = loadRouteStopsData();
            logger.info(
                    "Loaded {} routes, {} zones, {} stops and {} route-stop links",
                    routeStopsData.routesToSave.size(),
                    routeStopsData.zonesToSave.size(),
                    routeStopsData.stopsToSave.size(),
                    routeStopsData.routeStopsByRouteAndStop.values().stream().mapToInt(Map::size).sum());

            logger.info("Loading schedules from CSV...");
            loadSchedules(routeStopsData.routesByCode, routeStopsData.stopsByCode, routeStopsData.routeStopSequenceByRouteAndStop);

            logger.info("Saving zones...");
            zoneRepository.saveAll(routeStopsData.zonesToSave);

            logger.info("Saving locations...");
            locationRepository.saveAll(routeStopsData.locationsToSave);

            logger.info("Saving stops...");
            stopRepository.saveAll(routeStopsData.stopsToSave);

            logger.info("Saving routes, route-stop links and schedules...");
            routeRepository.saveAll(routeStopsData.routesToSave);

            logger.info("Database seeding completed successfully.");
        } catch (Exception e) {
            logger.error("Error during database seeding", e);
            throw e;
        }
    }

    private RouteStopsData loadRouteStopsData() {
        Map<String, Route> routesByCode = new LinkedHashMap<>();
        Map<String, Zone> zonesByCode = new LinkedHashMap<>();
        Map<String, Location> locationsByStopCode = new LinkedHashMap<>();
        Map<String, Stop> stopsByCode = new LinkedHashMap<>();
        Map<String, Map<String, RouteStop>> routeStopsByRouteAndStop = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> routeStopSequenceByRouteAndStop = new LinkedHashMap<>();
        List<Location> locationsToSave = new ArrayList<>();
        List<Stop> stopsToSave = new ArrayList<>();

        for (String[] row : readCsv(ROUTE_STOPS_CSV)) {
            if (row.length < 10) {
                throw new IllegalStateException(
                        "Invalid stops_routes_zones.csv row. Expected route_code,stop_code,stop_name,latitude,longitude,zone_code,sequence,transport_type,zone_id,hex_color");
            }

            String routeCode = normalizeKey(row[COL_ROUTE_CODE]);
            String stopCode = normalizeKey(row[COL_STOP_CODE]);
            String stopName = row[COL_STOP_NAME].trim();
            double latitude = Double.parseDouble(row[COL_LATITUDE].trim());
            double longitude = Double.parseDouble(row[COL_LONGITUDE].trim());
            String zoneCode = normalizeKey(row[COL_ZONE_CODE]);
            int sequence = Integer.parseInt(row[COL_SEQUENCE].trim());
            VehicleType stopType = parseVehicleType(row[COL_TRANSPORT_TYPE]);
            String zoneColor = row[COL_HEX_COLOR].trim();

            Route route = routesByCode.computeIfAbsent(routeCode, code -> {
                Route newRoute = new Route();
                newRoute.setName(code);
                return newRoute;
            });

            Zone zone = zonesByCode.get(zoneCode);
            if (zone == null) {
                zone = new Zone();
                zone.setName(zoneCode);
                zone.setColorHexCode(zoneColor);
                zonesByCode.put(zoneCode, zone);
            } else if (zone.getColorHexCode() != null && !zone.getColorHexCode().equalsIgnoreCase(zoneColor)) {
                throw new IllegalStateException("Conflicting hex color for zone_code " + zoneCode);
            }

            Location location = locationsByStopCode.get(stopCode);
            if (location == null) {
                location = new Location();
                location.setLatitude(latitude);
                location.setLongitude(longitude);
                locationsByStopCode.put(stopCode, location);
                locationsToSave.add(location);
            } else if (!sameCoordinate(location.getLatitude(), latitude) || !sameCoordinate(location.getLongitude(), longitude)) {
                logger.warn(
                        "Conflicting coordinates for stop_code {}: existing=({}, {}), incoming=({}, {}). Keeping the first record.",
                        stopCode,
                        location.getLatitude(),
                        location.getLongitude(),
                        latitude,
                        longitude);
            }

            Stop stop = stopsByCode.get(stopCode);
            if (stop == null) {
                stop = new Stop();
                stop.setName(stopName);
                stop.setStopCode(stopCode);
                stop.setStopType(stopType);
                stop.setLocation(location);
                stop.setZone(zone);
                zone.getStops().add(stop);
                stopsByCode.put(stopCode, stop);
                stopsToSave.add(stop);
            } else {
                if (stop.getLocation() == null) {
                    stop.setLocation(location);
                }
                if (stop.getZone() == null) {
                    stop.setZone(zone);
                }
                if (stop.getName() != null && !stop.getName().trim().equalsIgnoreCase(stopName)) {
                    logger.warn("Conflicting stop names for stop_code {}: '{}' vs '{}'", stopCode, stop.getName(), stopName);
                }
                if (stop.getStopType() != null && stop.getStopType() != stopType) {
                    logger.warn("Conflicting stop types for stop_code {}: '{}' vs '{}'", stopCode, stop.getStopType(), stopType);
                }
            }

            RouteStop routeStop = new RouteStop(route, stop, sequence);
            route.getRouteStops().add(routeStop);
            routeStopsByRouteAndStop.computeIfAbsent(routeCode, ignored -> new LinkedHashMap<>()).putIfAbsent(stopCode, routeStop);
            routeStopSequenceByRouteAndStop
                    .computeIfAbsent(routeCode, ignored -> new LinkedHashMap<>())
                    .merge(stopCode, sequence, Math::min);
        }

        if (routesByCode.isEmpty() || stopsToSave.isEmpty() || zonesByCode.isEmpty()) {
            throw new IllegalStateException("stops_routes_zones.csv is empty");
        }

        return new RouteStopsData(
                routesByCode,
                zonesByCode,
                locationsByStopCode,
                stopsByCode,
                routeStopsByRouteAndStop,
                routeStopSequenceByRouteAndStop,
                new ArrayList<>(zonesByCode.values()),
                locationsToSave,
                stopsToSave,
                new ArrayList<>(routesByCode.values()));
    }

    private void loadSchedules(
            Map<String, Route> routesByCode,
            Map<String, Stop> stopsByCode,
            Map<String, Map<String, Integer>> routeStopSequenceByRouteAndStop) {
        int loadedSchedules = 0;

        for (String[] row : readCsv(STOP_SCHEDULES_CSV)) {
            if (row.length < 5) {
                throw new IllegalStateException("Invalid schedule.csv row. Expected arrival_time,departure_time,stop_code,route_id,direction");
            }

            String routeCode = normalizeKey(row[COL_SCHEDULE_ROUTE_CODE]);
            String stopCode = normalizeKey(row[COL_SCHEDULE_STOP_CODE]);
            LocalDateTime arrivalTime = parseScheduleDateTime(row[COL_SCHEDULE_ARRIVAL]);
            LocalDateTime departureTime = parseScheduleDateTime(row[COL_SCHEDULE_DEPARTURE]);

            Route route = routesByCode.get(routeCode);
            if (route == null) {
                throw new IllegalStateException("Unknown route_id in schedule.csv: " + routeCode);
            }

            Stop stop = stopsByCode.get(stopCode);
            if (stop == null) {
                throw new IllegalStateException("Unknown stop_code in schedule.csv: " + stopCode);
            }

            Integer sequence = routeStopSequenceByRouteAndStop
                    .getOrDefault(routeCode, Map.of())
                    .get(stopCode);
            if (sequence == null) {
                throw new IllegalStateException(
                        "No route_stop sequence found for route_code " + routeCode + " and stop_code " + stopCode);
            }

            StopSchedule schedule = new StopSchedule();
            schedule.setArrivalTime(arrivalTime);
            schedule.setDepartureTime(departureTime);
            schedule.setSequence(sequence);
            schedule.setStop(stop);
            schedule.setRoute(route);

            route.schedules.add(schedule);
            stop.schedules.add(schedule);
            loadedSchedules += 1;
        }

        if (loadedSchedules == 0) {
            throw new IllegalStateException("schedule.csv is empty");
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
                    .map(this::normalizeCsvRow)
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

    private LocalDateTime parseScheduleDateTime(String value) {
        String rawValue = value.trim();
        String[] parts = rawValue.split(":", -1);
        if (parts.length != 3) {
            throw new IllegalStateException("Invalid time value: " + rawValue);
        }

        int hour = Integer.parseInt(parts[0]);
        int dayOffset = hour / 24;
        int normalizedHour = hour % 24;

        if (dayOffset > 0) {
            return LocalDateTime.of(
                    SEED_BASE_DATE.plusDays(dayOffset),
                    LocalTime.of(normalizedHour, Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
        }

        return LocalDateTime.of(SEED_BASE_DATE, parseTime(rawValue));
    }

    private VehicleType parseVehicleType(String raw) {
        if (raw == null || raw.isBlank()) {
            return VehicleType.BUS;
        }
        return VehicleType.valueOf(raw.trim().toUpperCase());
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase();
    }

    private String[] splitCsvLine(String line) {
        return line.split(",", -1);
    }

    private String[] normalizeCsvRow(String[] row) {
        String[] normalized = new String[row.length];
        for (int i = 0; i < row.length; i++) {
            normalized[i] = normalizeCsvValue(row[i]);
        }
        return normalized;
    }

    private String normalizeCsvValue(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        if (!looksLikeMojibake(value)) {
            return value;
        }

        byte[] latin1Bytes = value.getBytes(StandardCharsets.ISO_8859_1);
        return new String(latin1Bytes, StandardCharsets.UTF_8);
    }

    private boolean looksLikeMojibake(String value) {
        return value.contains("Ã") || value.contains("Â") || value.contains("�");
    }

    private boolean sameCoordinate(double left, double right) {
        return Math.abs(left - right) < 0.000001d;
    }

    private static Path resolveDataDir() {
        String configuredDir = System.getenv("SCRIPT_MOCK_DATA_DIR");
        if (configuredDir != null && !configuredDir.isBlank()) {
            return Path.of(configuredDir).normalize();
        }

        return Path.of("..", "script_mock_data", "data").normalize();
    }

    private static class RouteStopsData {
        final Map<String, Route> routesByCode;
        final Map<String, Zone> zonesByCode;
        final Map<String, Location> locationsByStopCode;
        final Map<String, Stop> stopsByCode;
        final Map<String, Map<String, RouteStop>> routeStopsByRouteAndStop;
        final Map<String, Map<String, Integer>> routeStopSequenceByRouteAndStop;
        final List<Zone> zonesToSave;
        final List<Location> locationsToSave;
        final List<Stop> stopsToSave;
        final List<Route> routesToSave;

        RouteStopsData(
                Map<String, Route> routesByCode,
                Map<String, Zone> zonesByCode,
                Map<String, Location> locationsByStopCode,
                Map<String, Stop> stopsByCode,
                Map<String, Map<String, RouteStop>> routeStopsByRouteAndStop,
                Map<String, Map<String, Integer>> routeStopSequenceByRouteAndStop,
                List<Zone> zonesToSave,
                List<Location> locationsToSave,
                List<Stop> stopsToSave,
                List<Route> routesToSave) {
            this.routesByCode = routesByCode;
            this.zonesByCode = zonesByCode;
            this.locationsByStopCode = locationsByStopCode;
            this.stopsByCode = stopsByCode;
            this.routeStopsByRouteAndStop = routeStopsByRouteAndStop;
            this.routeStopSequenceByRouteAndStop = routeStopSequenceByRouteAndStop;
            this.zonesToSave = zonesToSave;
            this.locationsToSave = locationsToSave;
            this.stopsToSave = stopsToSave;
            this.routesToSave = routesToSave;
        }
    }
}
