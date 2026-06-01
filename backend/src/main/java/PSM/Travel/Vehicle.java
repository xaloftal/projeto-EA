package PSM.Travel;

import PSM.Location.Location;
import PSM.Location.Route;
import PSM.Location.Stop;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "vehicle")
public class Vehicle {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;

	private int capacity;

	@Enumerated(EnumType.STRING)
	private VehicleType type;

	@ManyToOne
	public Location location;

	@ManyToOne(optional = true)
	public Route activeRoute;

	@ManyToOne(optional = true)
	public Stop lastNotifiedStop;



	public void updateLocation() {
		throw new UnsupportedOperationException();
	}

	public void arrived() {
		throw new UnsupportedOperationException();
	}

	public void getOccupancy() {
		throw new UnsupportedOperationException();
	}

	public UUID getId() {
		return this.id;
	}

	private void setId(UUID _id) {
		this.id = _id;
	}

	public int getCapacity() {
		return this.capacity;
	}

	public void setCapacity(int _capacity) {
		this.capacity = _capacity;
	}

	public VehicleType getType() {
		return this.type;
	}

	public void setType(VehicleType _type) {
		this.type = _type;
	}

	public Location getLocation() {
		return this.location;
	}

	public void setLocation(Location _location) {
		this.location = _location;
	}

	public Route getRoute() {
		return this.activeRoute;
	}

	public void setRoute(Route _activeRoute) {
		this.activeRoute = _activeRoute;
	}

	public Stop getStop() {
		return this.lastNotifiedStop;
	}

	public void setStop(Stop _lastNotifiedStop) {
		this.lastNotifiedStop = _lastNotifiedStop;
	}
}