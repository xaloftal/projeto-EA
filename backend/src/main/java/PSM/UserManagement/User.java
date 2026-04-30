package PSM.UserManagement;

import java.util.ArrayList;
import java.util.List;
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
@Table(name = "users")
public class User implements Observer {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;

	private String name;
	private String email;

	@JsonIgnore
	private String passwordHash;
	private float balance;

	@ManyToMany
	@JsonIgnore
	private List<Trip> trips = new ArrayList<Trip>();

	@OneToOne(optional = true)
	private Card card;

	@OneToMany(mappedBy = "user")
	private List<Ticket> tickets = new ArrayList<Ticket>();

	@ManyToMany
	@JsonIgnore
	private List<Stop> poi = new ArrayList<Stop>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<UserNotification> notifications = new ArrayList<UserNotification>();



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

		String message = "O autocarro chegou à paragem " + stop.getName();
		UserNotification notification = new UserNotification(stop, message);
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

	public List<Trip> getTrips() {
		return this.trips;
	}

	public void setTrips(List<Trip> _trips) {
		this.trips = _trips;
	}

	public Card getCard() {
		return this.card;
	}

	public void setCard(Card _card) {
		this.card = _card;
	}

	public List<Ticket> getTickets() {
		return this.tickets;
	}

	public void setCard(List<Ticket> _tickets) {
		this.tickets = _tickets;
	}

	public void addTicket(Ticket ticket) {
		if (ticket == null) {
			return;
		}
		this.tickets.add(ticket);
		ticket.setUser(this);
	}

	public List<Stop> getPOI() {
		return this.poi;
	}

	public void setPOI(List<Stop> _poi) {
		this.poi = _poi;
	}

	public List<UserNotification> getNotifications() {
		return this.notifications;
	}

	public void setNotifications(List<UserNotification> _notifications) {
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

		this.poi.removeIf(existingStop -> existingStop != null && existingStop.getId() != null && existingStop.getId().equals(stop.getId()));
	}

	public boolean hasPOI(Stop stop) {
		if (stop == null || stop.getId() == null) {
			return false;
		}

		return this.poi.stream().anyMatch(existingStop -> existingStop != null && existingStop.getId() != null && existingStop.getId().equals(stop.getId()));
	}

	public void addNotification(UserNotification notification) {
		if (notification == null) {
			return;
		}

		this.notifications.add(notification);
		notification.setUser(this);
	}
}