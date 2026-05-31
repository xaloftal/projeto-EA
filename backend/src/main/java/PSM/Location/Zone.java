package PSM.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import PSM.Location.Stop;
import jakarta.persistence.*;

@Entity
@Table(name = "zone")
public class Zone {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;

	private String name;

	@ManyToMany
	private List<Stop> stops = new ArrayList<Stop>();



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

	public List<Stop> getStops() {
		return this.stops;
	}

	public void setStops(ArrayList<Stop> _stops) {
		this.stops = _stops;
	}

}