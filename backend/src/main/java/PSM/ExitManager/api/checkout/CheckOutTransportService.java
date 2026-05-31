package PSM.ExitManager.api.checkout;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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


@Service
public class CheckOutTransportService {
    private final TitleRepository titleRepository;
    private final TripRepository tripRepository;
    private final ExitRecordRepository exitRecordRepository;

    public CheckOutTransportService(TitleRepository titleRepository, TripRepository tripRepository, ExitRecordRepository exitRecordRepository) {
        this.titleRepository = titleRepository;
        this.tripRepository = tripRepository;
        this.exitRecordRepository = exitRecordRepository;
    }

    public CheckOutResponseDTO checkOut(CheckOutRequestDTO request) {
        Title title = titleRepository.findById(request.titleId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Title not found"));
        Trip trip = tripRepository.findById(request.tripId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

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

    public void getValidatedTitles() {
        List<Title> titles = titleRepository.findByStateName("VALIDATED");
        for (Title title : titles) {
            title.use();
            titleRepository.save(title);
            //saveExitRecord(title, , null, true);
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
