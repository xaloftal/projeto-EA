package PSM.Location;

import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import PSM.Location.StopSchedule;

@Entity
@Table(name = "route")
public class Route {
	@Id
	@GeneratedValue(strategy=GenerationType.UUID)
	private UUID id;

	private String name;

	@OneToMany(cascade=CascadeType.ALL)
	@JoinColumn(name = "route_id")
	public List<StopSchedule> schedules = new ArrayList<StopSchedule>();



	public Stop getNextStop(Stop _stop) {
		throw new UnsupportedOperationException();
	}

	public LocalTime getEstimatedArrival() {
		throw new UnsupportedOperationException();
	}

	public UUID getId() {
		return this.id;
	}

	private void setId(UUID _id) { this.id = _id; }

	public String getName() {
		return this.name;
	}

	public void setName(String _name) { this.name = _name; }
}