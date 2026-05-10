package PSM.Ticketing;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.io.ByteArrayOutputStream;

import com.fasterxml.jackson.annotation.JsonIgnore;

import PSM.Ticketing.State.TitleState;
import PSM.Travel.Trip;
import PSM.UserManagement.User;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

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
	private LocalDateTime validUntil;
	private BigDecimal price;
	private byte[] qrCode;

	@Transient
	public TitleState status;

	@ManyToMany
	public List<Trip> trips = new ArrayList<Trip>();

	@ManyToOne
	@JoinColumn(name = "user_id")
	@JsonIgnore
	private User user;

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

}