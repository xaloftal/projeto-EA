package PSM.Location;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "stopschedule")
public class StopSchedule {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;

	private LocalDateTime arrivalTime;
	private LocalDateTime departureTime;
	private int sequence;

	@ManyToOne
	public Stop stop;

	@ManyToOne
	@JoinColumn(name = "route_id")
	@JsonIgnore
	public Route route;

	public UUID getId() {
		return this.id;
	}

	private void setId(UUID _id) {
		this.id = _id;
	}

	public LocalDateTime getArrivalTime() {
		return this.arrivalTime;
	}

	public void setArrivalTime(LocalDateTime _arrivalTime) {
		this.arrivalTime = _arrivalTime;
	}

	public LocalDateTime getDepartureTime() {
		return this.departureTime;
	}

	public void setDepartureTime(LocalDateTime _departureTime) {
		this.departureTime = _departureTime;
	}

	public int getSequence() {
		return this.sequence;
	}

	public void setSequence(int _sequence) {
		this.sequence = _sequence;
	}

	public Stop getStop() {
		return this.stop;
	}

	public void setStop(Stop _stop) {
		this.stop = _stop;
	}

	public Route getRoute() {
		return this.route;
	}

	public void setRoute(Route _route) {
		this.route = _route;
	}
}