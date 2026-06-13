package PSM.ValidationManager.api.checkin;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import PSM.Location.Stop;
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
        
        if (!validateCardZone(trip, card.getZone())) {
            logger.warn("Card zone validation failed - Zone: {}", card.getZone() != null ? card.getZone().getName() : "null");
            saveValidationRecord(card, trip, false);
            return new CheckInResponseDTO(false, "Trip is not within the card's zone");
        }
        
        // IMPORTANTE: Ativar/Validar o card para que o checkout funcione
        // O card precisa estar no estado VALIDATED ou ACTIVE para permitir checkout
        String currentState = card.getStateName();
        
        if ("UNUSED".equals(currentState)) {
            // Card novo - ativar
            card.activate();
            logger.info("Card activated - New State: {}", card.getStateName());
        } else if ("ACTIVE".equals(currentState)) {
            // Card já ativo - precisa ser validado para esta viagem
            // Podemos usar o método validate() se existir, ou manter como ACTIVE
            logger.info("Card already active - keeping state");
        }
        
        // Garantir que o card fica num estado que o checkout reconhece
        // O CheckOutService verifica "VALIDATED" para tickets, mas para cards pode verificar "ACTIVE"
        titleRepository.save(card);
        
        logger.info("Card checked in - Final State: {}", card.getStateName());
        saveValidationRecord(card, trip, true);
        
        return new CheckInResponseDTO(true, "Card checked in successfully");
    }

    private boolean validateTicketRoute(Trip trip, Stop from, Stop to) {
        if (from == null || to == null) return false;
        
        boolean hasFrom = trip.getRoute().schedules.stream()
            .anyMatch(schedule -> schedule.stop != null && schedule.stop.equals(from));
        boolean hasTo = trip.getRoute().schedules.stream()
            .anyMatch(schedule -> schedule.stop != null && schedule.stop.equals(to));
        return hasFrom && hasTo;
    }

    private boolean validateCardZone(Trip trip, Zone zone) {
        if (zone == null) return true; // Card sem zona específica pode viajar em qualquer lugar
        
        return trip.getRoute().schedules.stream()
                .anyMatch(schedule -> schedule.stop != null && zone.getStops().contains(schedule.stop));
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