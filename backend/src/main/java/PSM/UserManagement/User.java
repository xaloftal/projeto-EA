package PSM.UserManagement;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import PSM.Travel.Trip;
import PSM.Ticketing.Card;
import PSM.Ticketing.Ticket;
import PSM.Location.Stop;
import PSM.Location.Subject;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User implements Observer {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;

	private String name;
	private String email;
	private float balance;

	@ManyToMany
	public ArrayList<Trip> trips = new ArrayList<Trip>();

	@OneToOne(optional = true)
	public Card card;

	@OneToMany
	public ArrayList<Ticket> tickets = new ArrayList<Ticket>();

	@ManyToMany
	public ArrayList<Stop> poi = new ArrayList<Stop>();



	public void purchaseTicket() {
		throw new UnsupportedOperationException();
	}

	public void addBalance() {
		throw new UnsupportedOperationException();
	}

	public List<Ticket> getActiveTickets() {
		throw new UnsupportedOperationException();
	}

	public void notifyUser(Subject _stop) {
		throw new UnsupportedOperationException();
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

	public float getBalance() {
		return this.balance;
	}

	public void setBalance(float _balance) {
		this.balance = _balance;
	}

	public ArrayList<Trip> getTrips() {
		return this.trips;
	}

	public void setTrips(ArrayList<Trip> _trips) {
		this.trips = _trips;
	}

	public Card getCard() {
		return this.card;
	}

	public void setCard(Card _card) {
		this.card = _card;
	}

	public ArrayList<Ticket> getTickets() {
		return this.tickets;
	}

	public void setCard(ArrayList<Ticket> _tickets) {
		this.tickets = _tickets;
	}

	public ArrayList<Stop> getPOI() {
		return this.poi;
	}

	public void setPOI(ArrayList<Stop> _poi) {
		this.poi = _poi;
	}
}