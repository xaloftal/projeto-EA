package PSM.ExitManager.api.checkout;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import PSM.ExitManager.ExitRecord;
import PSM.ExitManager.api.exitrecord.ExitRecordRepository;
import PSM.Location.Stop;
import PSM.Location.api.route.RouteRepository;
import PSM.Ticketing.Card;
import PSM.Ticketing.Ticket;
import PSM.Ticketing.Title;
import PSM.Ticketing.api.title.TitleRepository;
import PSM.Travel.Trip;
import PSM.Travel.api.trip.TripRepository;

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

        // 🔴 VERIFICAÇÃO CORRIGIDA - Aceita ACTIVE para cards
        if (!isTitleCheckedIn(title)) {
            logger.warn("Checkout failed - Title {} is not checked in. Current state: {}", 
                       title.getId(), title.getStateName());
            return new CheckOutResponseDTO(false, "Title is not checked in. Please do check-in first.");
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
    
    /**
     * 🔴 MÉTODO IMPORTANTE - Verifica se o título está em estado de check-in
     * Cards: aceita ACTIVE ou VALIDATED
     * Tickets: apenas VALIDATED
     */
    private boolean isTitleCheckedIn(Title title) {
        String state = title.getStateName();
        logger.info("Checking title state for checkout: {}", state);
        
        if (title instanceof Card) {
            // Cards: ACTIVE ou VALIDATED são considerados check-in válido
            boolean isValid = "ACTIVE".equals(state) || "VALIDATED".equals(state);
            logger.info("Card checkout validation - State: {}, IsValid: {}", state, isValid);
            return isValid;
        } else {
            // Tickets: apenas VALIDATED
            boolean isValid = "VALIDATED".equals(state);
            logger.info("Ticket checkout validation - State: {}, IsValid: {}", state, isValid);
            return isValid;
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

        boolean isAtDestination = ticket.getTo().getId().equals(currentStop.getId());
        
        String message;
        boolean isCorrectExit;
        
        if (isAtDestination) {
            message = "Checkout realizado com sucesso na paragem de destino!";
            isCorrectExit = true;
        } else {
            message = "⚠️ ATENÇÃO: Checkout realizado antes da paragem de destino (" + ticket.getTo().getName() + ").";
            isCorrectExit = false;
        }
        
        ticket.use();
        titleRepository.save(ticket);
        saveExitRecord(ticket, trip, isCorrectExit, false);
        
        return new CheckOutResponseDTO(true, message);
    }

    private CheckOutResponseDTO checkOutCard(Card card, Trip trip) {
        logger.info("Checkout Card - ID: {}, State: {}", card.getId(), card.getStateName());
        
        // Cards não precisam de validação de destino
        saveExitRecord(card, trip, true, false);
        
        return new CheckOutResponseDTO(true, "Card checkout successful");
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
    
    String situationStr;
    String message;
    
    if (destinationStop != null && destinationStop.getId().equals(currentStop.getId())) {
        situationStr = "AT_DESTINATION";
        message = "Está na paragem de destino (" + destinationStop.getName() + "). Pode fazer checkout normalmente.";
    } else {
        situationStr = "BEFORE_DESTINATION";
        message = "Ainda não chegou à paragem de destino. Pode fazer checkout antecipado se desejar.";
    }
    
    return new CheckoutSituationResponseDTO(situationStr, currentStop.getName(), 
        destinationStop != null ? destinationStop.getName() : null, message);
}
}