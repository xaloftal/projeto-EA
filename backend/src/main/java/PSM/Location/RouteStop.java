package PSM.Location;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "route_stop", schema = "catchit", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"route_id", "sequence"})
})
public class RouteStop {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne
	@JoinColumn(name = "route_id", nullable = false)
	@JsonIgnore
	private Route route;

	@ManyToOne
	@JoinColumn(name = "stop_id", nullable = false)
	private Stop stop;

	@Column(nullable = false)
	private Integer sequence;

	public RouteStop() {
	}

	public RouteStop(Route route, Stop stop, Integer sequence) {
		this.route = route;
		this.stop = stop;
		this.sequence = sequence;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Route getRoute() {
		return route;
	}

	public void setRoute(Route route) {
		this.route = route;
	}

	public Stop getStop() {
		return stop;
	}

	public void setStop(Stop stop) {
		this.stop = stop;
	}

	public Integer getSequence() {
		return sequence;
	}

	public void setSequence(Integer sequence) {
		this.sequence = sequence;
	}
}
