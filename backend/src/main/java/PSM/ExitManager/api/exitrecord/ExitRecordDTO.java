package PSM.ExitManager.api.exitrecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExitRecordDTO(
    UUID id,
    LocalDateTime timestamp,
    Boolean correctExit,
    Boolean automatedExit,
    List<TitleDTO> titles,
    StopDTO stop,
    TripDTO trip
) {
    public record TitleDTO(
        UUID id,
        String status,
        StopInfoDTO fromStop,
        StopInfoDTO toStop,
        Double price
    ) {}
    
    public record StopInfoDTO(
        UUID id,
        String name,
        String stopCode,
        String stopType,
        Double latitude,
        Double longitude,
        String displayCode
    ) {}
    
    public record StopDTO(
        UUID id,
        String name,
        String stopCode,
        String stopType,
        Double latitude,
        Double longitude,
        String displayCode
    ) {}
    
    public record TripDTO(
        UUID id
    ) {}
}