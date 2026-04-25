package PSM.ExitManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import PSM.Ticketing.Title;
import PSM.Location.Stop;
import jakarta.persistence.*;

@Entity
@Table(name = "exitrecord")
public class ExitRecord {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;

	private LocalDateTime timestamp;
	private boolean result;

	@ManyToMany
	public ArrayList<Title> titles = new ArrayList<Title>();

	@ManyToOne
	public Stop stop;



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

	public boolean getResult() {
		return this.result;
	}

	public void setResult(boolean _result) {
		this.result = _result;
	}
}