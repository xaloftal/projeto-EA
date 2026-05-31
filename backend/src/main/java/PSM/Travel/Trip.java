package PSM.Travel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

import PSM.ValidationManager.ValidationRecord;
import PSM.ExitManager.ExitRecord;
import PSM.Location.Route;
import PSM.Location.Stop;
import PSM.Location.StopSchedule;
import jakarta.persistence.*;

@Entity
@Table(name = "trip")
public class Trip {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;
	private LocalDateTime startTime;
	private LocalDateTime endTime;

	@OneToMany(mappedBy = "trip", cascade = CascadeType.ALL)
	public ArrayList<ValidationRecord> validationRecords = new ArrayList<ValidationRecord>();

	@OneToMany(mappedBy = "trip", cascade = CascadeType.ALL)
	public ArrayList<ExitRecord> exitRecords = new ArrayList<ExitRecord>();

	@ManyToOne
	public Vehicle vehicle;

	@ManyToOne
	public Route route;



	public void start() {
		throw new UnsupportedOperationException();
	}

	public void end() {
		throw new UnsupportedOperationException();
	}

	public void getDuration() {
		throw new UnsupportedOperationException();
	}

	public Stop getCurrentStop() {
		LocalDateTime now = LocalDateTime.now();
		return route.schedules.stream()
			.filter(schedule -> schedule.getDepartureTime().isBefore(now))
			.max(Comparator.comparingInt(StopSchedule::getSequence))
			.map(schedule -> schedule.stop)
			.orElse(null);
	}

	public UUID getId() {
		return this.id;
	}

	private void setId(UUID _id) {
		this.id = _id;
	}

	public LocalDateTime getStartTime() {
		return this.startTime;
	}

	public void setStartTime(LocalDateTime _startTime) {
		this.startTime = _startTime;
	}

	public ArrayList<ValidationRecord> getValidationRecords() {
		return this.validationRecords;
	}

	public void setValidationRecords(ArrayList<ValidationRecord> _validationRecords) {
		this.validationRecords = _validationRecords;
	}

	public Vehicle getVehicle() {
		return this.vehicle;
	}

	public void setVehicle(Vehicle _vehicle) {
		this.vehicle = _vehicle;
	}

	public Route getRoute() {
		return this.route;
	}

	public void setRoute(Route _route) {
		this.route = _route;
	}
}