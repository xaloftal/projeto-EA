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

	public boolean renew() {
		throw new UnsupportedOperationException();
	}

	public Zone getZone() { return this.zone; }

	public void setZone(Zone _zone) { this.zone = _zone; }
}