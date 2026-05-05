package PSM.Travel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import PSM.ValidationManager.ValidationRecord;
import PSM.Location.Route;
import PSM.Location.Stop;
import jakarta.persistence.*;

@Entity
@Table(name = "trip", schema = "catchit")
public class Trip {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;
	private LocalDateTime startTime;
	private LocalDateTime endTime;

	@OneToMany(mappedBy = "trip", cascade = CascadeType.ALL)
	public ArrayList<ValidationRecord> validationRecords = new ArrayList<ValidationRecord>();

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
		throw new UnsupportedOperationException();
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