package PSM.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import PSM.Travel.VehicleType;
import PSM.UserManagement.Observer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "stop")
public class Stop implements Subject {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;

	private String name;

	private String stopCode;

	@Enumerated(EnumType.STRING)
	private VehicleType stopType;

	@ManyToOne
	@JoinColumn(name = "zone_id")
	@JsonIgnore
	private Zone zone;

	@OneToOne
	@JsonIgnore
	public Location location;

	@OneToMany(mappedBy = "stop",cascade = CascadeType.ALL)
	@JsonIgnore
	public List<StopSchedule> schedules = new ArrayList<StopSchedule>();

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

	public String getStopCode() {
		return this.stopCode;
	}

	public void setStopCode(String _stopCode) {
		this.stopCode = _stopCode;
	}

	public VehicleType getStopType() {
		return this.stopType;
	}

	public void setStopType(VehicleType _stopType) {
		this.stopType = _stopType;
	}

	@JsonProperty("latitude")
	public double getLatitude() {
		return this.location != null ? this.location.getLatitude() : 0;
	}

	@JsonProperty("longitude")
	public double getLongitude() {
		return this.location != null ? this.location.getLongitude() : 0;
	}

	public Location getLocation() {
		return this.location;
	}

	public void setLocation(Location _location) {
		this.location = _location;
	}

	public Zone getZone() {
		return this.zone;
	}

	public void setZone(Zone _zone) {
		this.zone = _zone;
	}
}
