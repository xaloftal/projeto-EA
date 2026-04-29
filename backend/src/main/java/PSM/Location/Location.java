package PSM.Location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "location")
public class Location {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;
	private double latitude;
	private double longitude;



	public UUID getId() {
		return this.id;
	}

	private void setId(UUID _id) { this.id = _id; }

	public double getLatitude() {
		return this.latitude;
	}

	public void setLatitude(double _latitude) { this.latitude = _latitude; }

	public double getLongitude() {
		return this.longitude;
	}

	public void setLongitude(double _longitude) { this.longitude = _longitude; }
}