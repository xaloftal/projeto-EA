package PSM.UserManagement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import PSM.Location.Stop;
import PSM.Location.Subject;
import PSM.Ticketing.Card;
import PSM.Ticketing.Ticket;
import PSM.Travel.Trip;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users", schema = "catchit")
public class User implements Observer {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private String name;
	private String email;

	@JsonIgnore
	private String passwordHash;
	private float balance;

	@ManyToMany
	@JsonIgnore
	private Set<Trip> trips = new HashSet<Trip>();

	@OneToOne(optional = true)
	private Card card;

	@OneToMany(mappedBy = "user")
	private Set<Ticket> tickets = new HashSet<Ticket>();

	@ManyToMany
	@JsonIgnore
	private Set<Stop> poi = new HashSet<Stop>();

	@OneToMany(mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, orphanRemoval = true)
	private Set<UserNotification> notifications = new HashSet<UserNotification>();

	public void purchaseTicket() {
		throw new UnsupportedOperationException();
	}

	public void addBalance() {
		throw new UnsupportedOperationException();
	}

	@JsonIgnore
	public List<Ticket> getActiveTickets() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void notifyUser(Subject _stop) {
		if (!(_stop instanceof Stop stop)) {
			return;
		}

		if (!this.hasPOI(stop)) {
			return;
		}

		UUID vehicleId = stop.getCurrentVehicleId();
		UUID routeId = stop.getCurrentRouteId();
		String routeName = stop.getCurrentRouteName();

		String message = String.format("O autocarro da linha %s chegou à paragem %s.",
				(routeName != null ? routeName : "parceira"), stop.getName());

		UserNotification notification = new UserNotification(stop, vehicleId, routeId, routeName, null, message);
		this.addNotification(notification);
	}

	public UUID getId() {
		return this.id;
	}

	public void setId(UUID _id) {
		this.id = _id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String _name) {
		this.name = _name;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String _email) {
		this.email = _email;
	}

	public String getPasswordHash() {
		return this.passwordHash;
	}

	public void setPasswordHash(String _passwordHash) {
		this.passwordHash = _passwordHash;
	}

	public float getBalance() {
		return this.balance;
	}

	public void setBalance(float _balance) {
		this.balance = _balance;
	}

	public Set<Trip> getTrips() {
		return this.trips;
	}

	public void setTrips(Set<Trip> _trips) {
		this.trips = _trips;
	}

	public Card getCard() {
		return this.card;
	}

	public void setCard(Card _card) {
		this.card = _card;
	}

	public Set<Ticket> getTickets() {
		return this.tickets;
	}

	public void setTickets(Set<Ticket> _tickets) {
		this.tickets = _tickets;
	}

	public void addTicket(Ticket ticket) {
		if (ticket == null) {
			return;
		}
		this.tickets.add(ticket);
		ticket.setUser(this);
	}

	public Set<Stop> getPOI() {
		return this.poi;
	}

	public void setPOI(Set<Stop> _poi) {
		this.poi = _poi;
	}

	public Set<UserNotification> getNotifications() {
		return this.notifications;
	}

	public void setNotifications(Set<UserNotification> _notifications) {
		this.notifications = _notifications;
	}

	public void addPOI(Stop stop) {
		if (stop == null || this.hasPOI(stop)) {
			return;
		}

		this.poi.add(stop);
	}

	public void removePOI(Stop stop) {
		if (stop == null) {
			return;
		}

		this.poi.removeIf(existingStop -> existingStop != null && existingStop.getId() != null
				&& existingStop.getId().equals(stop.getId()));
	}

	public boolean hasPOI(Stop stop) {
		if (stop == null || stop.getId() == null) {
			return false;
		}

		return this.poi.stream().anyMatch(existingStop -> existingStop != null && existingStop.getId() != null
				&& existingStop.getId().equals(stop.getId()));
	}

	public void addNotification(UserNotification notification) {
		if (notification == null) {
			return;
		}

		this.notifications.add(notification);
		notification.setUser(this);
	}
}