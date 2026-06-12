package PSM.ExitManager.api.checkout;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import PSM.ExitManager.ExitRecord;
import PSM.ExitManager.api.exitrecord.ExitRecordRepository;
import PSM.Location.Stop;
import PSM.Ticketing.Card;
import PSM.Ticketing.Ticket;
import PSM.Ticketing.Title;
import PSM.Ticketing.api.title.TitleRepository;
import PSM.Travel.Trip;
import PSM.Travel.api.trip.TripRepository;
import PSM.ValidationManager.api.validationrecord.ValidationRecordRepository;
import PSM.ValidationManager.ValidationRecord;


@Service
public class CheckOutTransportService {
    private final TitleRepository titleRepository;
    private final TripRepository tripRepository;
    private final ExitRecordRepository exitRecordRepository;
    private final ValidationRecordRepository validationRecordRepository;

    public CheckOutTransportService(TitleRepository titleRepository, TripRepository tripRepository, ExitRecordRepository exitRecordRepository, ValidationRecordRepository validationRecordRepository) {
        this.titleRepository = titleRepository;
        this.tripRepository = tripRepository;
        this.exitRecordRepository = exitRecordRepository;
        this.validationRecordRepository = validationRecordRepository;
    }

    @Transactional
    public CheckOutResponseDTO checkOut(CheckOutRequestDTO request) {
        Title title = titleRepository.findById(request.titleId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Title not found"));
        Trip trip;
        if (request.tripId() == null) {
            if (title.trips == null || title.trips.isEmpty()) {
                List<ValidationRecord> validations = validationRecordRepository.findSuccessfulValidationsByTitle(title);
                if (validations == null || validations.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active trip found for this title");
                }
                trip = validations.get(0).getTrip();
            } else {
                trip = title.trips.get(title.trips.size() - 1);
            }
        } else {
            trip = tripRepository.findById(request.tripId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
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
        if (!validateTicketExit(trip, ticket.getTo())) {
            saveExitRecord(ticket, trip, false, false);
            return new CheckOutResponseDTO(false, "Passenger exited at wrong stop");
        }
        ticket.use();
        titleRepository.save(ticket);
        saveExitRecord(ticket, trip, true, false);
        
        return new CheckOutResponseDTO(true, "Successful CheckOut");
    }

    private CheckOutResponseDTO checkOutCard(Card card, Trip trip) {
        saveExitRecord(card, trip, null, null);

        return new CheckOutResponseDTO(true, "Successful CheckOut");
    }

    private boolean validateTicketExit(Trip trip, Stop to) {
        return to.equals(trip.getCurrentStop());
    }

    @Transactional
    public void processAutomatedCheckOut() {
        List<Title> titles = titleRepository.findByStateName("VALIDATED");
        for (Title title : titles) {
            Trip lastTrip;
            if (title.trips == null || title.trips.isEmpty()) {
                List<ValidationRecord> validations = validationRecordRepository.findSuccessfulValidationsByTitle(title);
                if (validations == null || validations.isEmpty()) continue;
                lastTrip = validations.get(0).getTrip();
            } else {
                lastTrip = title.trips.get(title.trips.size() - 1);
            }
            title.use();
            titleRepository.save(title);
            saveExitRecord(title, lastTrip, false, true);
        }
    }

    private void saveExitRecord(Title title, Trip trip, Boolean correctExit, Boolean automatedExit) {
        ExitRecord exitRecord = new ExitRecord();

        exitRecord.setTimestamp(LocalDateTime.now());
        exitRecord.setCorrectExit(correctExit);
        exitRecord.setAutomatedExit(automatedExit);
        exitRecord.titles.add(title);
        exitRecord.setTrip(trip);
        exitRecord.setStop(trip.getCurrentStop());

        exitRecordRepository.save(exitRecord);
    }

}
