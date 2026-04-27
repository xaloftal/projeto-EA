package PSM.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import PSM.Location.Stop;
import jakarta.persistence.*;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Table(name = "zone")
public class Zone {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;

	private String name;

	private String colorHexCode;

	@OneToMany(mappedBy = "zone")
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

	public String getColorHexCode() {
		return this.colorHexCode;
	}

	public void setColorHexCode(String _colorHexCode) {
		this.colorHexCode = _colorHexCode;
	}

	public List<Stop> getStops() {
		return this.stops;
	}

	public void setStops(List<Stop> _stops) {
		this.stops = _stops;
	}

}