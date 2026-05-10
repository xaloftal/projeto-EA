package PSM.Ticketing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "ticketpack", schema = "catchit")
public class TicketPack {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;

	public BigDecimal discount;

	@OneToMany
	@JoinColumn(name = "ticketpack_id")
	public ArrayList<Ticket> pack = new ArrayList<Ticket>();


	public UUID getId() {
		return this.id;
	}

	private void setId(UUID _id) {
		this.id = _id;
	}

	public BigDecimal getDiscount() {
		return this.discount;
	}

	public void setDiscount(BigDecimal _discount) {
		this.discount = _discount;
	}

	public ArrayList<Ticket> getPack() {
		return this.pack;
	}

	public void setPack(ArrayList<Ticket> _pack) {
		this.pack = _pack;
	}
}