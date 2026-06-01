package PSM.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import PSM.Ticketing.Card;
import PSM.Ticketing.Ticket;
import PSM.Ticketing.api.card.CardRepository;
import PSM.Ticketing.api.ticket.TicketRepository;
import PSM.Travel.Trip;
import PSM.Travel.Vehicle;
import PSM.Travel.VehicleType;
import PSM.Travel.api.trip.TripRepository;
import PSM.Travel.api.vehicle.VehicleRepository;
import PSM.UserManagement.User;
import PSM.UserManagement.api.user.UserRepository;

@Component
public class DatabaseSeeder_test implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder_test.class);

    private static final Path DATA_DIR = Path.of(
            System.getenv().getOrDefault("SCRIPT_MOCK_DATA_DIR",
                    "/../../../../../script_mock_data/data"));
    private static final Path ROUTES_CSV         = DATA_DIR.resolve("routes.csv");
    private static final Path STOPS_CSV          = DATA_DIR.resolve("stops.csv");
    private static final Path STOP_SCHEDULES_CSV = DATA_DIR.resolve("schedule.csv");
    private static final Path VEHICLES_CSV       = DATA_DIR.resolve("vehicles.csv");
    private static final Path ZONES_CSV          = DATA_DIR.resolve("zones.csv");
    private static final Path USERS_CSV          = DATA_DIR.resolve("users.csv");
    private static final Path TRIPS_CSV          = DATA_DIR.resolve("trips.csv");
    private static final Path TITLES_CSV         = DATA_DIR.resolve("titles.csv");

    private static final LocalDate           SEED_BASE_DATE = LocalDate.of(2000, 1, 1);
    private static final DateTimeFormatter   DT_FORMATTER   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int                 THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    private static final int COL_STOP_ID       = 0;
    private static final int COL_STOP_NAME     = 1;
    private static final int COL_STOP_TYPE     = 2;
    private static final int COL_STOP_CODE     = 3;
    private static final int COL_LOCATION_ID   = 4;
    private static final int COL_LATITUDE      = 5;
    private static final int COL_LONGITUDE     = 6;

    private final LocationRepository     locationRepository;
    private final StopRepository         stopRepository;
    private final RouteRepository        routeRepository;
    private final StopScheduleRepository stopScheduleRepository;
    private final ZoneRepository         zoneRepository;
    private final VehicleRepository      vehicleRepository;
    private final UserRepository         userRepository;
    private final TripRepository         tripRepository;
    private final CardRepository         cardRepository;
    private final TicketRepository       ticketRepository;

    public DatabaseSeeder_test(
            LocationRepository locationRepository,
            StopRepository stopRepository,
            RouteRepository routeRepository,
            StopScheduleRepository stopScheduleRepository,
            ZoneRepository zoneRepository,
            VehicleRepository vehicleRepository,
            UserRepository userRepository,
            TripRepository tripRepository,
            CardRepository cardRepository,
            TicketRepository ticketRepository) {
        this.locationRepository     = locationRepository;
        this.stopRepository         = stopRepository;
        this.routeRepository        = routeRepository;
        this.stopScheduleRepository = stopScheduleRepository;
        this.zoneRepository         = zoneRepository;
        this.vehicleRepository      = vehicleRepository;
        this.userRepository         = userRepository;
        this.tripRepository         = tripRepository;
        this.cardRepository         = cardRepository;
        this.ticketRepository       = ticketRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (routeRepository.count() > 0 || stopRepository.count() > 0 || stopScheduleRepository.count() > 0) {
            seedSecondaryData();
            return;
        }
        seedFromCsv();
        seedSecondaryData();
    }

    // ------------------------------------------------------------------
    // Primary seed (routes, locations, stops, schedules)
    // ------------------------------------------------------------------

    private void seedFromCsv() {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        try {
            CompletableFuture<Map<String, Route>>    routesFuture    = CompletableFuture.supplyAsync(this::loadRoutes, executor);
            CompletableFuture<Map<String, Location>> locationsFuture = CompletableFuture.supplyAsync(this::loadLocations, executor);

            Map<String, Route>    routesByCode    = routesFuture.join();
            Map<String, Location> locationsByCode = locationsFuture.join();

            Map<String, Stop> stopsByCode = loadStops(locationsByCode);
            loadStopSchedules(routesByCode, stopsByCode);
            routeRepository.saveAll(routesByCode.values());
        } finally {
            executor.shutdown();
        }
    }

    // ------------------------------------------------------------------
    // Secondary seed (zones, vehicles, users, trips, titles)
    // ------------------------------------------------------------------

    private void seedSecondaryData() {
        List<Zone>    zones    = zoneRepository.count()    == 0 ? seedZones()    : zoneRepository.findAll();
        List<Vehicle> vehicles = vehicleRepository.count() == 0 ? seedVehicles() : vehicleRepository.findAll();
        List<User>    users    = userRepository.count()    == 0 ? seedUsers()    : userRepository.findAll();

        if (tripRepository.count() == 0) seedTrips(vehicles);
        if (cardRepository.count() == 0 && ticketRepository.count() == 0) seedTitles(zones, users);
    }

    // ------------------------------------------------------------------
    // Zones
    // ------------------------------------------------------------------

    private List<Zone> seedZones() {
        log.info("A popular tabela 'zone'...");
        List<Zone> zones = new ArrayList<>();
        for (String[] row : readCsv(ZONES_CSV)) {
            if (row.length < 1) continue;
            Zone zone = new Zone();
            zone.setName(row[0].trim());
            zones.add(zone);
        }
        if (zones.isEmpty()) throw new IllegalStateException("zones.csv is empty");
        List<Zone> saved = zoneRepository.saveAll(zones);
        log.info("✅ {} zonas inseridas", saved.size());
        return saved;
    }

    // ------------------------------------------------------------------
    // Vehicles
    // ------------------------------------------------------------------

    private List<Vehicle> seedVehicles() {
        log.info("A popular tabela 'vehicle'...");
        List<Route>   routes   = routeRepository.findAll();
        List<Vehicle> vehicles = new ArrayList<>();
        if (routes.isEmpty()) throw new IllegalStateException("No routes found — seed routes first");

        int routeIdx = 0;
        for (String[] row : readCsv(VEHICLES_CSV)) {
            if (row.length < 2) continue;
            Vehicle vehicle = new Vehicle();
            vehicle.setCapacity(Integer.parseInt(row[0].trim()));
            vehicle.setType(VehicleType.valueOf(row[1].trim().toUpperCase()));
            vehicle.activeRoute = routes.get(routeIdx % routes.size());
            routeIdx++;
            vehicles.add(vehicle);
        }
        if (vehicles.isEmpty()) throw new IllegalStateException("vehicles.csv is empty");
        List<Vehicle> saved = vehicleRepository.saveAll(vehicles);
        log.info("✅ {} veículos inseridos", saved.size());
        return saved;
    }

    // ------------------------------------------------------------------
    // Users
    // ------------------------------------------------------------------

    private List<User> seedUsers() {
        log.info("A popular tabela 'users'...");
        List<User> users = new ArrayList<>();
        for (String[] row : readCsv(USERS_CSV)) {
            if (row.length < 4) continue;
            User user = new User();
            user.setName(row[0].trim());
            user.setEmail(row[1].trim());
            user.setPasswordHash(row[2].trim());
            user.setBalance(Float.parseFloat(row[3].trim()));
            users.add(user);
        }
        if (users.isEmpty()) throw new IllegalStateException("users.csv is empty");
        List<User> saved = userRepository.saveAll(users);
        log.info("✅ {} utilizadores inseridos", saved.size());
        return saved;
    }

    // ------------------------------------------------------------------
    // Trips
    // ------------------------------------------------------------------

    private void seedTrips(List<Vehicle> vehicles) {
        log.info("A popular tabela 'trip'...");
        List<Route> routes = routeRepository.findAll();
        Random rnd = new Random(42);
        List<Trip> trips = new ArrayList<>();

        for (String[] row : readCsv(TRIPS_CSV)) {
            if (row.length < 2) continue;
            Trip trip = new Trip();
            trip.setStartTime(LocalDateTime.parse(row[0].trim(), DT_FORMATTER));
            String endTimeRaw = row[1].trim();
            if (!endTimeRaw.isEmpty()) {
                trip.setEndTime(LocalDateTime.parse(endTimeRaw, DT_FORMATTER));
            }
            trip.vehicle = vehicles.get(rnd.nextInt(vehicles.size()));
            trip.route   = routes.get(rnd.nextInt(routes.size()));
            trips.add(trip);
        }
        tripRepository.saveAll(trips);
        log.info("✅ {} trips inseridos", trips.size());
    }

    // ------------------------------------------------------------------
    // Titles (Cards + Tickets)
    // ------------------------------------------------------------------

    private void seedTitles(List<Zone> zones, List<User> users) {
        log.info("A popular tabela 'title' (cards + tickets)...");
        Random rnd = new Random(42);
        List<Card>   cards   = new ArrayList<>();
        List<Ticket> tickets = new ArrayList<>();

        // Construir mapa route -> lista de stops para garantir pares válidos
        List<Route> routes = routeRepository.findAll();
        Map<Route, List<Stop>> routeStopsMap = new java.util.HashMap<>();
        for (Route route : routes) {
            List<Stop> stops = route.schedules.stream()
                    .map(s -> s.stop)
                    .filter(s -> s != null)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
            if (stops.size() >= 2) {
                routeStopsMap.put(route, stops);
            }
        }
        List<Route> routesWithStops = new ArrayList<>(routeStopsMap.keySet());
        if (routesWithStops.isEmpty()) throw new IllegalStateException("No routes with stops found");

        for (String[] row : readCsv(TITLES_CSV)) {
            if (row.length < 6) continue;
            String        titleType  = row[0].trim();
            String        stateName  = row[1].trim();
            BigDecimal    price      = new BigDecimal(row[2].trim());
            LocalDateTime createdAt  = LocalDateTime.parse(row[3].trim(), DT_FORMATTER);
            LocalDateTime validFrom  = LocalDateTime.parse(row[4].trim(), DT_FORMATTER);
            LocalDateTime validUntil = LocalDateTime.parse(row[5].trim(), DT_FORMATTER);
            User user = users.get(rnd.nextInt(users.size()));

            if ("card".equalsIgnoreCase(titleType)) {
                Card card = new Card();
                card.setCreatedAt(createdAt);
                card.setValidFrom(validFrom);
                card.setValidUntil(validUntil);
                card.setPrice(price);
                card.setStateName(stateName);
                card.setUser(user);
                card.zone = zones.get(rnd.nextInt(zones.size()));
                cards.add(card);
            } else {
                // Escolher uma rota aleatória e dois stops distintos dessa rota
                Route route = routesWithStops.get(rnd.nextInt(routesWithStops.size()));
                List<Stop> stops = routeStopsMap.get(route);
                int fromIdx = rnd.nextInt(stops.size());
                int toIdx;
                do { toIdx = rnd.nextInt(stops.size()); } while (toIdx == fromIdx);

                Ticket ticket = new Ticket();
                ticket.setCreatedAt(createdAt);
                ticket.setValidFrom(validFrom);
                ticket.setValidUntil(validUntil);
                ticket.setPrice(price);
                ticket.setStateName(stateName);
                ticket.setUser(user);
                ticket.setFrom(stops.get(fromIdx));
                ticket.setTo(stops.get(toIdx));
                tickets.add(ticket);
            }
        }
        cardRepository.saveAll(cards);
        ticketRepository.saveAll(tickets);
        log.info("✅ {} cards e {} tickets inseridos", cards.size(), tickets.size());
    }

    // ------------------------------------------------------------------
    // Primary helpers (routes, locations, stops, schedules)
    // ------------------------------------------------------------------

    private Map<String, Route> loadRoutes() {
        log.info("A popular tabela 'route'...");
        Map<String, Route> routesByCode = new LinkedHashMap<>();
        for (String[] row : readCsv(ROUTES_CSV)) {
            if (row.length < 2) throw new IllegalStateException("Invalid routes.csv row. Expected id,name");
            String routeId   = row[0].trim();
            String routeCode = row[1].trim();
            Route route = new Route();
            route.setName(routeCode);
            routesByCode.put(routeId, route);
            routesByCode.put(routeCode, route);
        }
        if (routesByCode.isEmpty()) throw new IllegalStateException("routes.csv is empty");
        return routesByCode;
    }

    private Map<String, Location> loadLocations() {
        log.info("A popular tabela 'location'...");
        Map<String, Location> locationsByCode = new LinkedHashMap<>();
        for (String[] row : readCsv(STOPS_CSV)) {
            if (row.length <= COL_LONGITUDE) throw new IllegalStateException(
                    "Invalid stops.csv row. Expected id,name,stop_type,stop_code,location_id,latitude,longitude");
            String locationId = row[COL_LOCATION_ID].trim();
            if (locationsByCode.containsKey(locationId)) continue;
            Location location = new Location();
            location.setLatitude(Double.parseDouble(row[COL_LATITUDE].trim()));
            location.setLongitude(Double.parseDouble(row[COL_LONGITUDE].trim()));
            locationsByCode.put(locationId, locationRepository.save(location));
        }
        if (locationsByCode.isEmpty()) throw new IllegalStateException("No locations found in stops.csv");
        log.info("✅ {} locations inseridas", locationsByCode.size());
        return locationsByCode;
    }

    private Map<String, Stop> loadStops(Map<String, Location> locationsByCode) {
        log.info("A popular tabela 'stop'...");
        Map<String, Stop> stopsByCode = new LinkedHashMap<>();
        List<Stop> stopsToSave = new ArrayList<>();
        List<String[]> rows = readCsv(STOPS_CSV);

        for (String[] row : rows) {
            if (row.length <= COL_LONGITUDE) throw new IllegalStateException(
                    "Invalid stops.csv row. Expected id,name,stop_type,stop_code,location_id,latitude,longitude");
            String stopId   = row[COL_STOP_ID].trim();
            String stopCode = row[COL_STOP_CODE].trim();
            Location location = locationsByCode.get(row[COL_LOCATION_ID].trim());
            if (location == null) throw new IllegalStateException("Unknown location_id in stops.csv: " + row[COL_LOCATION_ID]);
            Stop stop = new Stop();
            stop.setName(row[COL_STOP_NAME].trim());
            stop.setStopType(parseVehicleType(row[COL_STOP_TYPE]));
            stop.setLocation(location);
            stopsToSave.add(stop);
            // Temporary mapping by index — will be replaced after saveAll
            stopsByCode.put(stopId, stop);
            stopsByCode.put(stopCode, stop);
        }
        stopRepository.saveAll(stopsToSave);
        if (stopsByCode.isEmpty()) throw new IllegalStateException("stops.csv is empty");
        log.info("✅ {} stops inseridas", stopsToSave.size());
        return stopsByCode;
    }

    private void loadStopSchedules(Map<String, Route> routesByCode, Map<String, Stop> stopsByCode) {
        log.info("A popular tabela 'stopschedule'...");
        int count = 0;
        for (String[] row : readCsv(STOP_SCHEDULES_CSV)) {
            if (row.length < 6) throw new IllegalStateException(
                    "Invalid schedule.csv row. Expected id,arrival_time,departure_time,sequence,stop_id,route_id");
            Route route = routesByCode.get(row[5].trim());
            if (route == null) throw new IllegalStateException("Unknown route_code in stop_schedules.csv: " + row[5]);
            Stop stop = stopsByCode.get(row[4].trim());
            if (stop == null) throw new IllegalStateException("Unknown stop_code in stop_schedules.csv: " + row[4]);
            StopSchedule schedule = new StopSchedule();
            schedule.setArrivalTime(LocalDateTime.of(SEED_BASE_DATE, parseTime(row[1])));
            schedule.setDepartureTime(LocalDateTime.of(SEED_BASE_DATE, parseTime(row[2])));
            schedule.setSequence(Integer.parseInt(row[3].trim()));
            schedule.stop = stop;
            route.schedules.add(schedule);
            count++;
        }
        log.info("✅ {} stop schedules inseridos", count);
    }

    // ------------------------------------------------------------------
    // Shared utilities
    // ------------------------------------------------------------------

    private List<String[]> readCsv(Path filePath) {
        if (!Files.exists(filePath)) throw new IllegalStateException("CSV file not found: " + filePath);
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
        if (!line.isEmpty() && line.charAt(0) == '\uFEFF') return line.substring(1);
        return line;
    }

    private LocalTime parseTime(String value) { return LocalTime.parse(value.trim()); }

    private VehicleType parseVehicleType(String raw) {
        if (raw == null || raw.isBlank()) return VehicleType.BUS;
        return VehicleType.valueOf(raw.trim().toUpperCase());
    }

    private String[] splitCsvLine(String line) { return line.split(",", -1); }
}