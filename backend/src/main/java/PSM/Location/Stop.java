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
import PSM.UserManagement.User;
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
import jakarta.persistence.Transient;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "stop", schema = "catchit")
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

	@Transient
	@JsonIgnore
	private List<Observer> observers = new ArrayList<Observer>();

	@Override
	public void notifyObservers() {
		for (Observer observer : new ArrayList<Observer>(this.observers)) {
			observer.notifyUser(this);
		}
	}

	@Override
	public void addObserver(Observer _obs) {
		if (_obs == null) {
			return;
		}

		if (_obs instanceof User newUser) {
			boolean alreadyRegistered = this.observers.stream().anyMatch(existingObserver -> existingObserver instanceof User existingUser && existingUser.getId() != null && existingUser.getId().equals(newUser.getId()));
			if (!alreadyRegistered) {
				this.observers.add(_obs);
			}
			return;
		}

		if (!this.observers.contains(_obs)) {
			this.observers.add(_obs);
		}
	}

	@Override
	public void removeObserver(Observer _obs) {
		if (_obs == null) {
			return;
		}

		if (_obs instanceof User user) {
			this.observers.removeIf(existingObserver -> existingObserver instanceof User existingUser && existingUser.getId() != null && existingUser.getId().equals(user.getId()));
			return;
		}

		this.observers.remove(_obs);
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

	public List<Observer> getObservers() {
		return this.observers;
	}
	
	
	public Zone getZone() {
		return this.zone;
	}

	public void setZone(Zone _zone) {
		this.zone = _zone;
	}
}
