package PSM.ExitManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import PSM.Ticketing.Title;
import PSM.Travel.Trip;
import PSM.Location.Stop;
import jakarta.persistence.*;

@Entity
@Table(name = "exitrecord", schema = "catchit")
public class ExitRecord {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;

	private LocalDateTime timestamp;
	private Boolean correctExit;
	private Boolean automatedExit;

	@ManyToMany
	public List<Title> titles = new ArrayList<Title>();

	@ManyToOne
	public Stop stop;

	@ManyToOne
	public Trip trip;
<<<<<<< HEAD

=======
	
>>>>>>> cr/feat-checkin

	public UUID getId() {
		return this.id;
	}

	private void setId(UUID _id) { this.id = _id; }

	public LocalDateTime getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(LocalDateTime _timestamp) {
		this.timestamp = _timestamp;
	}

	public Boolean getCorrectExit() {
		return this.correctExit;
	}

	public void setCorrectExit(Boolean correctExit) {
		this.correctExit = correctExit;
	}

	public Boolean getAutomatedExit() {
		return this.automatedExit;
	}

	public void setAutomatedExit(Boolean automatedExit) {
		this.automatedExit = automatedExit;
	}

	public List<Title> getTitles() {
		return this.titles;
	}

	public void setTitles(List<Title> _titles) {
		this.titles = _titles;
	}

	public Stop getStop() {
		return this.stop;
	}

	public void setStop(Stop _stop) {
		this.stop = _stop;
	}

	public Trip getTrip() {
		return this.trip;
	}

	public void setTrip(Trip _trip) {
		this.trip = _trip;
	}
}