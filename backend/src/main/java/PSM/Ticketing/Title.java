package PSM.Ticketing;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import PSM.Ticketing.State.ActiveState;
import PSM.Ticketing.State.ExpiredState;
import PSM.Ticketing.State.TitleState;
import PSM.Ticketing.State.UnusedState;
import PSM.Ticketing.State.UsedState;
import PSM.Ticketing.State.ValidatedState;
import PSM.Travel.Trip;
import PSM.UserManagement.User;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "title", schema = "catchit")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "title_type")
public abstract class Title {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	private LocalDateTime createdAt;
	private LocalDateTime validFrom;
	public LocalDateTime validUntil;
	private BigDecimal price;
	private byte[] qrCode;

	@Column(name = "state_name")
	private String stateName;

	@Transient
	@JsonIgnore
	public TitleState status;

	@ManyToMany
	@JsonIgnore
	public List<Trip> trips = new ArrayList<Trip>();

	@PostLoad
	private void restoreState() {
		if (this.stateName == null) {
			this.status = new UnusedState();
			return;
		}
		switch (this.stateName) {
			case "UNUSED" -> this.status = new UnusedState();
			case "ACTIVE" -> this.status = new ActiveState();
			case "VALIDATED" -> this.status = new ValidatedState();
			case "EXPIRED" -> this.status = new ExpiredState();
			case "USED" -> this.status = new UsedState();
		}
	}

	@ManyToOne
	@JoinColumn(name = "user_id")
	@JsonIgnore
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ticketpack_id")
	@JsonBackReference
	private TicketPack ticketPack;

	public void activate() {
		throw new UnsupportedOperationException();
	}

	public void renew() {
		throw new UnsupportedOperationException();
	}

	public void expire() {
		throw new UnsupportedOperationException();
	}

	public void use() {
		throw new UnsupportedOperationException();
	}

	public boolean validate() {
		throw new UnsupportedOperationException();
	}

	@JsonIgnore
	public boolean isValid() {
		return LocalDateTime.now().isBefore(this.validUntil);
	}

	public void generateQrCode(String text, int size) {
		try {
			QRCodeWriter writer = new QRCodeWriter();
			BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size);
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
				this.qrCode = baos.toByteArray();
			}
		} catch (Exception e) {
			throw new RuntimeException("Unable to generate QR code", e);
		}
	}

	@JsonIgnore
	public Duration getRemainingValidity() {
		return Duration.between(LocalDateTime.now(), this.validUntil);
	}

	public UUID getId() {
		return this.id;
	}

	private void setId(UUID _id) {
		this.id = _id;
	}

	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(LocalDateTime _createdAt) {
		this.createdAt = _createdAt;
	}

	public LocalDateTime getValidFrom() {
		return this.validFrom;
	}

	public void setValidFrom(LocalDateTime _validFrom) {
		this.validFrom = _validFrom;
	}

	public LocalDateTime getValidUntil() {
		return this.validUntil;
	}

	public void setValidUntil(LocalDateTime _validUntil) {
		this.validUntil = _validUntil;
	}

	public BigDecimal getPrice() {
		return this.price;
	}

	public void setPrice(BigDecimal _price) {
		this.price = _price;
	}

	public byte[] getQrCode() {
		return this.qrCode;
	}

	public void setQrCode(byte[] _qrCode) {
		this.qrCode = _qrCode;
	}

	@JsonProperty("status")
	public String getStateName() {
		return this.stateName;
	}

	public void setStateName(String stateName) {
		this.stateName = stateName;
		if (stateName == null) {
			this.status = new UnusedState();
			return;
		}
		switch (stateName) {
			case "UNUSED" -> this.status = new UnusedState();
			case "ACTIVE" -> this.status = new ActiveState();
			case "VALIDATED" -> this.status = new ValidatedState();
			case "EXPIRED" -> this.status = new ExpiredState();
			case "USED" -> this.status = new UsedState();
		}
	}

	@JsonIgnore
	public TitleState getStatus() {
		return this.status;
	}

	public void setStatus(TitleState _status) {
		this.status = _status;
		this.stateName = _status != null ? _status.getStateName() : null;
	}

	public List<Trip> getTrips() {
		return this.trips;
	}

	public void setTrips(List<Trip> _trips) {
		this.trips = _trips;
	}

	public User getUser() {
		return this.user;
	}

	public void setUser(User _user) {
		this.user = _user;
	}

	public TicketPack getTicketPack() { return ticketPack; }
	public void setTicketPack(TicketPack ticketPack) { this.ticketPack = ticketPack; }

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Title title = (Title) o;
		return id != null && id.equals(title.id);
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}

}