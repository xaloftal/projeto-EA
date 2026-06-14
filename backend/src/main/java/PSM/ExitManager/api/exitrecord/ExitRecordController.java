package PSM.ExitManager.api.exitrecord;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PSM.ExitManager.ExitRecord;
import PSM.Location.Stop;
import PSM.Ticketing.Ticket;

@RestController
@RequestMapping("/api/exitrecords")
public class ExitRecordController {

    private final ExitRecordRepository exitRecordRepository;

    public ExitRecordController(ExitRecordRepository exitRecordRepository) {
        this.exitRecordRepository = exitRecordRepository;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ExitRecordDTO>> getUserHistory(@PathVariable UUID userId) {
        List<ExitRecord> records = exitRecordRepository.findHistoryByUserId(userId);
        
        List<ExitRecordDTO> dtos = records.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }
    
    private ExitRecordDTO toDTO(ExitRecord er) {
        // Converter titles
        List<ExitRecordDTO.TitleDTO> titleDTOs = er.titles.stream()
            .map(title -> {
                Stop fromStop = null;
                Stop toStop = null;
                Double price = null;
                
                if (title instanceof Ticket ticket) {
                    fromStop = ticket.getFrom();
                    toStop = ticket.getTo();
                    price = ticket.getPrice() != null ? ticket.getPrice().doubleValue() : null;
                }
                
                return new ExitRecordDTO.TitleDTO(
                    title.getId(),
                    title.getStateName(),
                    fromStop != null ? toStopInfoDTO(fromStop) : null,
                    toStop != null ? toStopInfoDTO(toStop) : null,
                    price
                );
            })
            .collect(Collectors.toList());
        
        // Converter stop
        ExitRecordDTO.StopDTO stopDTO = er.stop != null ? 
            new ExitRecordDTO.StopDTO(
                er.stop.getId(),
                er.stop.getName(),
                er.stop.getStopCode(),
                er.stop.getStopType() != null ? er.stop.getStopType().name() : null,
                er.stop.getLocation() != null ? er.stop.getLocation().getLatitude() : null,
                er.stop.getLocation() != null ? er.stop.getLocation().getLongitude() : null,
                er.stop.getDisplayCode()
            ) : null;
        
        // Converter trip
        ExitRecordDTO.TripDTO tripDTO = er.trip != null ?
            new ExitRecordDTO.TripDTO(er.trip.getId()) : null;
        
        return new ExitRecordDTO(
            er.getId(),
            er.getTimestamp(),
            er.getCorrectExit(),
            er.getAutomatedExit(),
            titleDTOs,
            stopDTO,
            tripDTO
        );
    }
    
    private ExitRecordDTO.StopInfoDTO toStopInfoDTO(Stop stop) {
        return new ExitRecordDTO.StopInfoDTO(
            stop.getId(),
            stop.getName(),
            stop.getStopCode(),
            stop.getStopType() != null ? stop.getStopType().name() : null,
            stop.getLocation() != null ? stop.getLocation().getLatitude() : null,
            stop.getLocation() != null ? stop.getLocation().getLongitude() : null,
            stop.getDisplayCode()
        );
    }
}