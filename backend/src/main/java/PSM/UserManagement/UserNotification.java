package PSM.UserManagement;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import PSM.Location.Stop;
import PSM.Travel.VehicleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_notifications", schema = "catchit")
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "stop_id")
    private UUID stopId;

    @Column(name = "stop_name")
    private String stopName;
	
	@Column(name = "stop_code")
    private String stopCode;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "route_id")
    private UUID routeId;

    @Column(name = "route_name")
    private String routeName;

    @Column(name = "message")
    private String message;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type")
    private VehicleType vehicleType;

    @ManyToOne
	@JsonIgnore
    @JoinColumn(name = "user_id")
    private User user;

    public UserNotification() {}

    public UserNotification(Stop stop, UUID vehicleId, UUID routeId, String routeName, VehicleType vehicleType, String message) {
        this.stopId = stop != null ? stop.getId() : null;
        this.stopName = stop != null ? stop.getName() : null;
		this.stopCode = stop != null ? stop.getStopCode() : null;	
        this.vehicleId = vehicleId;
        this.routeId = routeId;
        this.routeName = routeName;
        this.vehicleType = vehicleType;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getStopId() { return stopId; }
    public String getStopName() { return stopName; }
    public UUID getVehicleId() { return vehicleId; }
    public UUID getRouteId() { return routeId; }
    public String getRouteName() { return routeName; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getUser() { return user; }
	public String getStopCode() { return stopCode; }

    public void setUser(User user) { this.user = user; }
	public void setStopCode(String stopCode) { this.stopCode = stopCode; }
    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }
}