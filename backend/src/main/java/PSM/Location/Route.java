package PSM.Location;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "route")
public class Route {
	@Id
	@GeneratedValue(strategy=GenerationType.UUID)
	private UUID id;

	private String name;

	@OneToMany(mappedBy = "route", cascade = CascadeType.ALL)
	public List<StopSchedule> schedules = new ArrayList<StopSchedule>();



	@JsonIgnore
	public Stop getNextStop(Stop _stop) {
		throw new UnsupportedOperationException();
	}

	@JsonIgnore
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