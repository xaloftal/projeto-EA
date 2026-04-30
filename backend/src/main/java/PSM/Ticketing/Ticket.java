package PSM.Ticketing;

import PSM.Location.Stop;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
@DiscriminatorValue("ticket")
public class Ticket extends Title {
	@ManyToOne
	public Stop to;

	@ManyToOne
	public Stop from;



	public Stop getTo() { return this.to; }

	public void setTo(Stop _to) { this.to = _to; }

	public Stop getFrom() { return this.from; }

	public void setFrom(Stop _from) { this.from = _from; }
}