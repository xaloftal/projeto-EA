package PSM.ValidationManager.api.checkin;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import PSM.Location.Stop;
import PSM.Location.StopSchedule;
import PSM.Location.Zone;
import PSM.Ticketing.Card;
import PSM.Ticketing.Ticket;
import PSM.Ticketing.Title;
import PSM.Ticketing.api.title.TitleRepository;
import PSM.Travel.Trip;
import PSM.Travel.api.trip.TripRepository;
import PSM.ValidationManager.ValidationRecord;
import PSM.ValidationManager.api.validationrecord.ValidationRecordRepository;

@Service
public class CheckInService {
    private static final Logger logger = LoggerFactory.getLogger(CheckInService.class);
    
    private final TitleRepository titleRepository;
    private final TripRepository tripRepository;
    private final ValidationRecordRepository validationRepository;

    public CheckInService(TitleRepository titleRepository, TripRepository tripRepository, ValidationRecordRepository validationRepository) {
        this.titleRepository = titleRepository;
        this.tripRepository = tripRepository;
        this.validationRepository = validationRepository;
    }

    public CheckInResponseDTO checkIn(CheckInRequestDTO request) {
        logger.info("Check-in request - titleId: {}, tripId: {}", request.titleId(), request.tripId());
        
        Title title = titleRepository.findById(request.titleId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Title not found"));
        Trip trip = tripRepository.findById(request.tripId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        logger.info("Title found - Type: {}, Current State: {}", 
                   title instanceof Card ? "CARD" : "TICKET", 
                   title.getStateName());

        if (title instanceof Card) { 
            return checkInCard((Card) title, trip); 
        }
        else if (title instanceof Ticket) { 
            return checkInTicket((Ticket) title, trip); 
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid title type");
        }
    }

    private CheckInResponseDTO checkInTicket(Ticket ticket, Trip trip) {
        logger.info("Checking in TICKET - ID: {}, Current State: {}", ticket.getId(), ticket.getStateName());
        
        if (!ticket.isValid()) {
            logger.warn("Ticket is not valid - State: {}", ticket.getStateName());
            saveValidationRecord(ticket, trip, false);
            return new CheckInResponseDTO(false, "Ticket is not valid");
        }
        if (!validateTicketRoute(trip, ticket.getFrom(), ticket.getTo())) {
            logger.warn("Ticket route validation failed");
            saveValidationRecord(ticket, trip, false);
            return new CheckInResponseDTO(false, "Trip does not match ticket route");
        }
        
        // Validar o ticket (muda estado para VALIDATED)
        ticket.validate();
        titleRepository.save(ticket);
        logger.info("Ticket checked in - New State: {}", ticket.getStateName());
        
        saveValidationRecord(ticket, trip, true);
        return new CheckInResponseDTO(true, "Successful CheckIn");
    }

    private CheckInResponseDTO checkInCard(Card card, Trip trip) {
        logger.info("Checking in CARD - ID: {}, Current State: {}", card.getId(), card.getStateName());
        
        if (!card.isValid()) {
            logger.warn("Card is not valid - State: {}, ValidUntil: {}", card.getStateName(), card.getValidUntil());
            saveValidationRecord(card, trip, false);
            return new CheckInResponseDTO(false, "Card is not active or expired");
        }
        
        // Se o cartão não tem zona, é válido para qualquer viagem
        if (card.getZone() == null) {
            logger.info("Card has no zone restriction - valid for any trip");
            
            // Ativar o card se necessário
            String currentState = card.getStateName();
            if ("UNUSED".equals(currentState)) {
                card.activate();
                logger.info("Card activated - New State: {}", card.getStateName());
                titleRepository.save(card);
            } else if ("ACTIVE".equals(currentState)) {
                logger.info("Card already active - keeping state");
            }
            
            saveValidationRecord(card, trip, true);
            return new CheckInResponseDTO(true, "Card checked in successfully (no zone restriction)");
        }
        
        // Validar zona do cartão
        if (!validateCardZone(trip, card.getZone())) {
            // Mensagem mais útil sobre quais zonas a trip cobre
            Set<String> tripZones = new HashSet<>();
            if (trip.getRoute() != null && trip.getRoute().getSchedules() != null) {
                for (StopSchedule schedule : trip.getRoute().getSchedules()) {
                    if (schedule.getStop() != null && schedule.getStop().getZone() != null) {
                        tripZones.add(schedule.getStop().getZone().getName());
                    }
                }
            }
            
            String message = String.format(
                "Card is only valid for zone '%s'. This trip serves zones: %s",
                card.getZone().getName(),
                tripZones.isEmpty() ? "none" : String.join(", ", tripZones)
            );
            
            logger.warn("Card zone validation failed - {}", message);
            saveValidationRecord(card, trip, false);
            return new CheckInResponseDTO(false, message);
        }
        
        // Ativar/Validar o card
        String currentState = card.getStateName();
        
        if ("UNUSED".equals(currentState)) {
            card.activate();
            logger.info("Card activated - New State: {}", card.getStateName());
        } else if ("ACTIVE".equals(currentState)) {
            logger.info("Card already active - keeping state");
        }
        
        titleRepository.save(card);
        
        logger.info("Card checked in - Final State: {}", card.getStateName());
        saveValidationRecord(card, trip, true);
        
        return new CheckInResponseDTO(true, "Card checked in successfully");
    }

    private boolean validateTicketRoute(Trip trip, Stop from, Stop to) {
        if (from == null || to == null) return false;
        
        boolean hasFrom = trip.getRoute().schedules.stream()
            .anyMatch(schedule -> schedule.stop != null && 
                (schedule.stop.equals(from) || 
                (schedule.stop.getName() != null && schedule.stop.getName().equalsIgnoreCase(from.getName()))));
        boolean hasTo = trip.getRoute().schedules.stream()
            .anyMatch(schedule -> schedule.stop != null && 
                (schedule.stop.equals(to) || 
                (schedule.stop.getName() != null && schedule.stop.getName().equalsIgnoreCase(to.getName()))));
        return hasFrom && hasTo;
    }

private boolean validateCardZone(Trip trip, Zone cardZone) {
    if (cardZone == null) return true; // Card sem zona específica pode viajar em qualquer lugar
    
    // Verificar se algum stop da trip pertence à zona do cartão
    for (StopSchedule schedule : trip.getRoute().getSchedules()) {
        if (schedule.getStop() != null) {
            Zone stopZone = schedule.getStop().getZone();
            if (stopZone != null && stopZone.getId().equals(cardZone.getId())) {
                logger.info("Found matching zone: {} - Stop: {}", cardZone.getName(), schedule.getStop().getName());
                return true;
            }
        }
    }
    
    // Log das zonas disponíveis na trip para debug
    Set<String> availableZones = new HashSet<>();
    for (StopSchedule schedule : trip.getRoute().getSchedules()) {
        if (schedule.getStop() != null && schedule.getStop().getZone() != null) {
            availableZones.add(schedule.getStop().getZone().getName());
        }
    }
    logger.warn("Card zone {} not found in trip zones: {}", cardZone.getName(), availableZones);
    
    return false;
}

    private void saveValidationRecord(Title title, Trip trip, boolean success) {
        ValidationRecord validationRecord = new ValidationRecord();
        validationRecord.setTimestamp(LocalDateTime.now());
        validationRecord.setResult(success);
        validationRecord.titles.add(title);
        validationRecord.setTrip(trip);
        validationRecord.setStop(trip.getCurrentStop());
        validationRepository.save(validationRecord);
        logger.info("Validation record saved - Success: {}", success);
    }
}