package PSM.Location;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
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
}