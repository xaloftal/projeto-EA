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

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final Path DATA_DIR = Path.of(
            System.getenv().getOrDefault("SCRIPT_MOCK_DATA_DIR",
                    "/../../../../../script_mock_data/data"));
    private static final Path ROUTES_CSV = DATA_DIR.resolve("routes.csv");
    private static final Path STOPS_CSV = DATA_DIR.resolve("stops.csv");
    private static final Path STOP_SCHEDULES_CSV = DATA_DIR.resolve("schedule.csv");
    private static final LocalDate SEED_BASE_DATE = LocalDate.of(2000, 1, 1);

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
        Map<String, Route> routesByCode = loadRoutes();
        Map<String, Stop> stopsByCode = loadStops();
        loadStopSchedules(routesByCode, stopsByCode);

        routeRepository.saveAll(routesByCode.values());
    }

    private Map<String, Route> loadRoutes() {
        Map<String, Route> routesByCode = new LinkedHashMap<>();

        for (String[] row : readCsv(ROUTES_CSV)) {
            if (row.length < 2) {
                throw new IllegalStateException("Invalid routes.csv row. Expected id,name");
            }

            String routeCode = row[1].trim();

            Route route = new Route();
            route.setName(routeCode);
            routesByCode.put(routeCode, route);
        }

        if (routesByCode.isEmpty()) {
            throw new IllegalStateException("routes.csv is empty");
        }

        return routesByCode;
    }

    private Map<String, Stop> loadStops() {
        Map<String, Stop> stopsByCode = new LinkedHashMap<>();

        for (String[] row : readCsv(STOPS_CSV)) {
            if (row.length < 2) {
                throw new IllegalStateException("Invalid stops.csv row. Expected id,name,stop_type,location_id");
            }

            String stopCode = row[0].trim();
            String stopName = row[1].trim();
            VehicleType stopType = parseVehicleType(row.length > 2 ? row[2] : null);

            Location location = new Location();
            // Mock CSV does not include coordinates. Persist neutral coordinates for now.
            location.setLatitude(0.0);
            location.setLongitude(0.0);
            locationRepository.save(location);

            Stop stop = new Stop();
            stop.setName(stopName);
            stop.setStopType(stopType);
            stop.setLocation(location);
            stopsByCode.put(stopCode, stopRepository.save(stop));
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
