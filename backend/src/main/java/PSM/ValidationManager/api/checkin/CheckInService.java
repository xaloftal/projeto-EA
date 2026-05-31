package PSM.ValidationManager.api.checkin;

import java.time.LocalDateTime;

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
    private final TitleRepository titleRepository;
    private final TripRepository tripRepository;
    private final ValidationRecordRepository validationRepository;

    public CheckInService(TitleRepository titleRepository, TripRepository tripRepository, ValidationRecordRepository validationRepository) {
        this.titleRepository = titleRepository;
        this.tripRepository = tripRepository;
        this.validationRepository = validationRepository;
    }

    public CheckInResponseDTO checkIn(CheckInRequestDTO request) {
        Title title = titleRepository.findById(request.titleId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Title not found"));
        Trip trip = tripRepository.findById(request.tripId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

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
        if (!ticket.isValid()) {
            saveValidationRecord(ticket, trip, false);
            return new CheckInResponseDTO(false, "Ticket is not valid");
        }
        if (!validateTicketRoute(trip, ticket.getFrom(), ticket.getTo())) {
            saveValidationRecord(ticket, trip, false);
            return new CheckInResponseDTO(false, "Trip does not match ticket route");
        }
        ticket.validate();
        titleRepository.save(ticket);
        saveValidationRecord(ticket, trip, true);
        
        return new CheckInResponseDTO(true, "Successful CheckIn");
    }

    private CheckInResponseDTO checkInCard(Card card, Trip trip) {
        if (!card.isValid()) {
            saveValidationRecord(card, trip, false);
            return new CheckInResponseDTO(false, "Card is not active");
        }
        if (!validateCardZone(trip, card.getZone())) {
            saveValidationRecord(card, trip, false);
            return new CheckInResponseDTO(false, "Trip is not within the card's zone");
        }
        saveValidationRecord(card, trip, true);
        return new CheckInResponseDTO(true, "Successful CheckIn");
    }

    private boolean validateTicketRoute(Trip trip, Stop from, Stop to) {

        boolean hasFrom = trip.getRoute().schedules.stream()
            .anyMatch(schedule -> schedule.stop.equals(from));
        boolean hasTo = trip.getRoute().schedules.stream()
            .anyMatch(schedule -> schedule.stop.equals(to));
        return hasFrom && hasTo;
    }

    private boolean validateCardZone(Trip trip, Zone zone) {

        return trip.getRoute().schedules.stream()
                .allMatch(schedule -> zone.getStops().contains(schedule.stop));
    }

    private void saveValidationRecord(Title title, Trip trip, boolean success) {
        ValidationRecord validationRecord = new ValidationRecord();

        validationRecord.setTimestamp(LocalDateTime.now());
        validationRecord.setResult(success);
        validationRecord.titles.add(title);
        validationRecord.setTrip(trip);
        validationRecord.setStop(trip.getCurrentStop());

        validationRepository.save(validationRecord);
    }
    
}
