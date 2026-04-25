package PSM.Ticketing;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import PSM.Ticketing.State.TitleState;
import PSM.Travel.Trip;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "title")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Title {
	@Id
	@GeneratedValue(strategy= GenerationType.UUID)
	private UUID id;
	private LocalDateTime createdAt;
	private LocalDateTime validFrom;
	private LocalDateTime validUntil;
	private BigDecimal price;
	private byte[] qrCode;

	@Transient
	public TitleState status;

	@ManyToMany
	public ArrayList<Trip> titles = new ArrayList<Trip>();



	public void activate() {
		throw new UnsupportedOperationException();
	}

	public void expire() {
		throw new UnsupportedOperationException();
	}

	@JsonIgnore
	public boolean isValid() {
		throw new UnsupportedOperationException();
	}

	private void generateQrCode() {
		throw new UnsupportedOperationException();
	}

	public boolean validate() {
		throw new UnsupportedOperationException();
	}

	@JsonIgnore
	public Duration getRemainingValidity() {
		throw new UnsupportedOperationException();
	}

	@JsonIgnore
	public String getStateName() {
		throw new UnsupportedOperationException();
	}

	public void use() {
		throw new UnsupportedOperationException();
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

	public TitleState getStatus() {
		return this.status;
	}

	public void setStatus(TitleState _status) {
		this.status = _status;
	}
}