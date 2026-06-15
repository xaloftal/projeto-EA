package PSM.Location.api.stop;

import java.util.UUID;

public class StopDTO {
    private UUID id;
    private String name;
    private String stopCode;
    private int sequence;
    private String zoneName;
    private UUID zoneId;
    private Double latitude;
    private Double longitude;

    public StopDTO() {}

    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getStopCode() { return stopCode; }
    public int getSequence() { return sequence; }
    public String getZoneName() { return zoneName; }
    public UUID getZoneId() { return zoneId; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setStopCode(String stopCode) { this.stopCode = stopCode; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
    public void setZoneId(UUID zoneId) { this.zoneId = zoneId; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}