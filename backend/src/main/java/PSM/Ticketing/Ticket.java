package PSM.Ticketing;

import PSM.Location.Stop;
import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ManyToOne;

@Entity
@DiscriminatorValue("ticket")
public class Ticket extends Title {
	@ManyToOne
	public Stop to;

	@ManyToOne
	public Stop from;


	@Override
	public boolean validate() {
		try {
			this.status.validate(this);
			return true;
		} catch (UnsupportedOperationException e) {
			return false;
		}
	}

	@Override
	public void use() {
		this.status.use(this);
	}

	@Override
	public void expire() {
		this.status.expire(this);
	}

	public Stop getTo() { return this.to; }

	public void setTo(Stop _to) { this.to = _to; }

	public Stop getFrom() { return this.from; }

	public void setFrom(Stop _from) { this.from = _from; }
}