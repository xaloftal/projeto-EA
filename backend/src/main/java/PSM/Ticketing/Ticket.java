package PSM.Ticketing;

import java.time.LocalDateTime;

import PSM.Location.Stop;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@DiscriminatorValue("ticket")
public class Ticket extends Title {
	@ManyToOne
	@JoinColumn(name = "to_id")
	public Stop toStop;

	@ManyToOne
	@JoinColumn(name = "from_id")
	public Stop fromStop;


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

	@Override
	public boolean isValid() {
		return LocalDateTime.now().isBefore(this.validUntil) && this.getStateName().equals("UNUSED");
	}

	public Stop getTo() { return this.toStop; }

	public void setTo(Stop _toStop) { this.toStop = _toStop; }

	public Stop getFrom() { return this.fromStop; }

	public void setFrom(Stop _fromStop) { this.fromStop = _fromStop; }
}