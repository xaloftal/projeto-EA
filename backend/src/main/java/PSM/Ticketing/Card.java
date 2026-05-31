package PSM.Ticketing;

import PSM.Location.Zone;
import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.OneToOne;

@Entity
@DiscriminatorValue("card")
public class Card extends Title {
	@OneToOne
	public Zone zone;

	@Override
	public void renew() {
		this.status.activate(this);
	}

	@Override
	public void expire() {
		this.status.expire(this);
	}

	public Zone getZone() { return this.zone; }

	public void setZone(Zone _zone) { this.zone = _zone; }
}