package PSM.Ticketing;

import PSM.Location.Zone;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.DiscriminatorValue;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@DiscriminatorValue("card")
public class Card extends Title {
	@ManyToOne
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