package PSM.Ticketing.api.ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TicketDTO {
    private UUID id;
    private LocalDateTime createdAt;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private BigDecimal price;
    private String status;
    private UUID fromStopId;
    private String fromStopName;
    private UUID toStopId;
    private String toStopName;

    public TicketDTO(UUID id, LocalDateTime createdAt, LocalDateTime validFrom, LocalDateTime validUntil, 
                     BigDecimal price, String status, UUID fromStopId, String fromStopName, UUID toStopId, String toStopName) {
        this.id = id;
        this.createdAt = createdAt;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.price = price;
        this.status = status;
        this.fromStopId = fromStopId;
        this.fromStopName = fromStopName;
        this.toStopId = toStopId;
        this.toStopName = toStopName;
    }

    // Getters e Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getFromStopId() { return fromStopId; }
    public void setFromStopId(UUID fromStopId) { this.fromStopId = fromStopId; }
    public String getFromStopName() { return fromStopName; }
    public void setFromStopName(String fromStopName) { this.fromStopName = fromStopName; }
    public UUID getToStopId() { return toStopId; }
    public void setToStopId(UUID toStopId) { this.toStopId = toStopId; }
    public String getToStopName() { return toStopName; }
    public void setToStopName(String toStopName) { this.toStopName = toStopName; }
}