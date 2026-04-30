package PSM.UserManagement;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import PSM.Location.Stop;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_notifications")
public class UserNotification {

	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;

	private UUID stopId;
	private String stopName;
	private String message;
	private LocalDateTime createdAt;

	@ManyToOne
	@JsonIgnore
	private User user;

	public UserNotification() {
	}

	public UserNotification(Stop stop, String message) {
		this.stopId = stop != null ? stop.getId() : null;
		this.stopName = stop != null ? stop.getName() : null;
		this.message = message;
		this.createdAt = LocalDateTime.now();
	}

	public UUID getId() {
		return this.id;
	}

	public void setId(UUID _id) {
		this.id = _id;
	}

	public UUID getStopId() {
		return this.stopId;
	}

	public void setStopId(UUID _stopId) {
		this.stopId = _stopId;
	}

	public String getStopName() {
		return this.stopName;
	}

	public void setStopName(String _stopName) {
		this.stopName = _stopName;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String _message) {
		this.message = _message;
	}

	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(LocalDateTime _createdAt) {
		this.createdAt = _createdAt;
	}

	public User getUser() {
		return this.user;
	}

	public void setUser(User _user) {
		this.user = _user;
	}
}