package PSM.ValidationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import PSM.Ticketing.Ticket;
import PSM.Ticketing.Title;
import PSM.Location.Stop;
import PSM.Travel.Trip;
import jakarta.persistence.*;

@Entity
@Table(name = "validationrecord", schema = "catchit")
public class ValidationRecord {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;
	private LocalDateTime timestamp;
	private boolean result;

	@ManyToMany
	public ArrayList<Title> titles = new ArrayList<Title>();

	@ManyToOne
	public Stop stop;

	@ManyToOne
	public Trip trip;



	public UUID getId() {
		return this.id;
	}

	private void setId(UUID _id) {
		this.id = _id;
	}

	public LocalDateTime getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(LocalDateTime _timestamp) {
		this.timestamp = _timestamp;
	}

	public boolean getResult() {
		return this.result;
	}

	public void setResult(boolean _result) {
		this.result = _result;
	}

	public ArrayList<Title> getTitles() {
		return this.titles;
	}

	public void setTitles(ArrayList<Title> _titles) {
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