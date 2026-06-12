package PSM.ExitManager.api.checkout;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import PSM.ExitManager.ExitRecord;
import PSM.ExitManager.api.exitrecord.ExitRecordRepository;
import PSM.Location.Route;
import PSM.Location.RouteStop;
import PSM.Location.Stop;
import PSM.Location.api.route.RouteRepository;
import PSM.Ticketing.Card;
import PSM.Ticketing.Ticket;
import PSM.Ticketing.Title;
import PSM.Ticketing.api.title.TitleRepository;
import PSM.Travel.Trip;
import PSM.Travel.api.trip.TripRepository;

// DTO definido dentro do mesmo ficheiro
record CheckoutSituationResponseDTO(
    String situation,
    String currentStopName,
    String destinationStopName,
    String message
) {}

@Service
public class CheckOutTransportService {
    private static final Logger logger = LoggerFactory.getLogger(CheckOutTransportService.class);
    
    private final TitleRepository titleRepository;
    private final TripRepository tripRepository;
    private final ExitRecordRepository exitRecordRepository;
    private final RouteRepository routeRepository;

    public CheckOutTransportService(TitleRepository titleRepository, 
                                    TripRepository tripRepository, 
                                    ExitRecordRepository exitRecordRepository,
                                    RouteRepository routeRepository) {
        this.titleRepository = titleRepository;
        this.tripRepository = tripRepository;
        this.exitRecordRepository = exitRecordRepository;
        this.routeRepository = routeRepository;
    }

    public CheckOutResponseDTO checkOut(CheckOutRequestDTO request) {
        if (request.titleId() == null) {
            return new CheckOutResponseDTO(false, "Title ID is required");
        }
        if (request.tripId() == null) {
            return new CheckOutResponseDTO(false, "Trip ID is required");
        }

        Title title = titleRepository.findById(request.titleId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Title not found: " + request.titleId()));
        
        Trip trip = tripRepository.findById(request.tripId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + request.tripId()));

        if (!"VALIDATED".equals(title.getStateName())) {
            return new CheckOutResponseDTO(false, "Title is not checked in");
        }

        if (title instanceof Card) { 
            return checkOutCard((Card) title, trip); 
        }
        else if (title instanceof Ticket) { 
            return checkOutTicket((Ticket) title, trip); 
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid title type");
        }
    }

    private CheckOutResponseDTO checkOutTicket(Ticket ticket, Trip trip) {
        if (ticket.getTo() == null) {
            saveExitRecord(ticket, trip, false, false);
            return new CheckOutResponseDTO(false, "Ticket has no destination stop");
        }

        Stop currentStop = trip.getCurrentStop();
        if (currentStop == null) {
            saveExitRecord(ticket, trip, false, false);
            return new CheckOutResponseDTO(false, "Current stop not available for this trip");
        }

        CheckoutSituation situation = evaluateCheckoutSituation(trip, ticket.getTo(), currentStop);
        
        String message;
        boolean isCorrectExit;
        String situationStr;
        
        switch (situation) {
            case BEFORE_DESTINATION:
                message = "Checkout realized with success.";
                isCorrectExit = true;
                situationStr = "BEFORE_DESTINATION";
                break;
            case AT_DESTINATION:
                message = "Checkout realized with success!";
                isCorrectExit = true;
                situationStr = "AT_DESTINATION";
                break;
            case AFTER_DESTINATION:
                message = "⚠️ You have passed the destination stop.";
                isCorrectExit = false;
                situationStr = "AFTER_DESTINATION";
                break;
            default:
                message = "Checkout realized with success!";
                isCorrectExit = true;
                situationStr = "UNKNOWN";
        }
        
        ticket.use();
        titleRepository.save(ticket);
        saveExitRecord(ticket, trip, isCorrectExit, false);
        
        return new CheckOutResponseDTO(true, message, situationStr, ticket.getTo().getName(), currentStop.getName());
    }

    private CheckOutResponseDTO checkOutCard(Card card, Trip trip) {
        saveExitRecord(card, trip, true, false);
        return new CheckOutResponseDTO(true, "Card checkout successful");
    }

    private enum CheckoutSituation {
        BEFORE_DESTINATION,
        AT_DESTINATION,
        AFTER_DESTINATION
    }
    
    private CheckoutSituation evaluateCheckoutSituation(Trip trip, Stop destinationStop, Stop currentStop) {
        if (destinationStop == null || currentStop == null) {
            return CheckoutSituation.AT_DESTINATION;
        }
        
        if (destinationStop.getId().equals(currentStop.getId())) {
            logger.info("Passenger checking out AT destination stop: {}", currentStop.getName());
            return CheckoutSituation.AT_DESTINATION;
        }
        
        Route route = routeRepository.findById(trip.getRoute().getId()).orElse(null);
        
        if (route == null || route.getRouteStops() == null || route.getRouteStops().isEmpty()) {
            logger.warn("No route stops found for route: {}", trip.getRoute().getId());
            return CheckoutSituation.BEFORE_DESTINATION;
        }
        
        List<RouteStop> sortedStops = route.getRouteStops().stream()
            .sorted(Comparator.comparingInt(RouteStop::getSequence))
            .collect(Collectors.toList());
        
        int destinationIndex = -1;
        int currentIndex = -1;
        
        for (int i = 0; i < sortedStops.size(); i++) {
            Stop stop = sortedStops.get(i).getStop();
            if (stop == null) continue;
            
            if (stop.getId().equals(destinationStop.getId())) {
                destinationIndex = i;
            }
            if (stop.getId().equals(currentStop.getId())) {
                currentIndex = i;
            }
        }
        
        if (destinationIndex == -1 || currentIndex == -1) {
            logger.warn("Could not find stops in route - destination found: {}, current found: {}", 
                       destinationIndex != -1, currentIndex != -1);
            return CheckoutSituation.BEFORE_DESTINATION;
        }
        
        if (currentIndex < destinationIndex) {
            logger.info("Checkout BEFORE destination - Current index: {}, Destination index: {}", 
                       currentIndex, destinationIndex);
            return CheckoutSituation.BEFORE_DESTINATION;
        } else if (currentIndex == destinationIndex) {
            return CheckoutSituation.AT_DESTINATION;
        } else {
            logger.warn("Checkout AFTER destination - Current index: {}, Destination index: {}", 
                       currentIndex, destinationIndex);
            return CheckoutSituation.AFTER_DESTINATION;
        }
    }

    public void processAutomatedCheckOut() {
        List<Title> titles = titleRepository.findByStateName("VALIDATED");
        
        for (Title title : titles) {
            if (title.getTrips() == null || title.getTrips().isEmpty()) {
                continue;
            }
            
            Trip lastTrip = title.getTrips().get(title.getTrips().size() - 1);
            
            if (title instanceof Ticket) {
                title.use();
                titleRepository.save(title);
                saveExitRecord(title, lastTrip, false, true);
            } else if (title instanceof Card) {
                saveExitRecord(title, lastTrip, true, true);
            }
        }
    }

    private void saveExitRecord(Title title, Trip trip, Boolean correctExit, Boolean automatedExit) {
        ExitRecord exitRecord = new ExitRecord();
        
        // O ID é gerado automaticamente pelo JPA - não chamar setId()
        
        exitRecord.setTimestamp(LocalDateTime.now());
        exitRecord.setCorrectExit(correctExit);
        exitRecord.setAutomatedExit(automatedExit != null ? automatedExit : false);
        
        if (exitRecord.getTitles() == null) {
            exitRecord.setTitles(new java.util.ArrayList<>());
        }
        exitRecord.getTitles().add(title);
        
        exitRecord.setTrip(trip);
        
        if (trip != null && trip.getCurrentStop() != null) {
            exitRecord.setStop(trip.getCurrentStop());
        }
        
        exitRecordRepository.save(exitRecord);
    }
    
    public CheckoutSituationResponseDTO getCheckoutSituation(UUID titleId, UUID tripId) {
        Title title = titleRepository.findById(titleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Title not found"));
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
        
        if (!(title instanceof Ticket)) {
            return new CheckoutSituationResponseDTO("CARD", null, null, "Card can always checkout");
        }
        
        Ticket ticket = (Ticket) title;
        Stop currentStop = trip.getCurrentStop();
        Stop destinationStop = ticket.getTo();
        
        if (currentStop == null) {
            return new CheckoutSituationResponseDTO("UNKNOWN", null, 
                destinationStop != null ? destinationStop.getName() : null, 
                "Current stop not available");
        }
        
        CheckoutSituation situation = evaluateCheckoutSituation(trip, destinationStop, currentStop);
        String situationStr;
        String message;
        
        switch (situation) {
            case BEFORE_DESTINATION:
                situationStr = "BEFORE_DESTINATION";
                message = "You have not yet reached the destination stop (" + destinationStop.getName() + "). You can make an early checkout if you wish.";
                break;
            case AT_DESTINATION:
                situationStr = "AT_DESTINATION";
                message = "You are at the destination stop (" + destinationStop.getName() + "). You can make a normal checkout.";
                break;
            case AFTER_DESTINATION:
                situationStr = "AFTER_DESTINATION";
                message = "⚠️ Attention: You have passed the destination stop (" + destinationStop.getName() + "). The checkout will be registered as outside the allowed zone.";
                break;
            default:
                situationStr = "UNKNOWN";
                message = "Could not determine the current situation.";
        }
        
        return new CheckoutSituationResponseDTO(situationStr, currentStop.getName(), 
            destinationStop != null ? destinationStop.getName() : null, message);
    }
}