package PSM.Location;

import PSM.Travel.VehicleType;
import java.util.ArrayList;
import java.util.UUID;

import PSM.UserManagement.Observer;
import jakarta.persistence.*;

@Entity
@Table(name = "stop")
public class Stop implements Subject {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;

	private String name;

	@Enumerated(EnumType.STRING)
	private VehicleType stopType;

	@OneToOne
	public Location location;

	@OneToMany(mappedBy = "stop",cascade = CascadeType.ALL)
	public ArrayList<StopSchedule> schedules = new ArrayList<StopSchedule>();

	public void notifyObservers() {
		throw new UnsupportedOperationException();
	}

	public void addObserver(Observer _obs) {
		throw new UnsupportedOperationException();
	}

	public void removeObserver(Observer _obs) {
		throw new UnsupportedOperationException();
	}

	public UUID getId() {
		return this.id;
	}

	private void setId(UUID _id) {
		this.id = _id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String _name) {
		this.name = _name;
	}

	public VehicleType getStopType() {
		return this.stopType;
	}

	public void setStopType(VehicleType _stopType) {
		this.stopType = _stopType;
	}

	public Location getLocation() {
		return this.location;
	}

	public void setLocation(Location _location) {
		this.location = _location;
	}
}