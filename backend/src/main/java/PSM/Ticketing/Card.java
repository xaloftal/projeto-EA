package PSM.Ticketing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import PSM.Location.Zone;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Entity
@DiscriminatorValue("card")
public class Card extends Title {
    private static final Logger logger = LoggerFactory.getLogger(Card.class);

    @ManyToOne
    public Zone zone;

    @Override
    public void renew() {
        this.validUntil = this.validUntil.plusMonths(1);
        this.status.activate(this);
        generateQrCodeForCard();
    }

    @Override
    public void expire() {
        this.status.expire(this);
    }

    @Override
    public void activate() {
        if (this.status instanceof PSM.Ticketing.State.UnusedState) {
            this.status.activate(this);
            generateQrCodeForCard();
        }
    }

    /**
     * Gera QR code para o card (mesmo formato que os tickets)
     */
    private void generateQrCodeForCard() {
        try {
            String qrContent = generateQrContent();
            logger.info("Generating QR code for card {} with content: {}", this.getId(), qrContent);
            this.generateQrCode(qrContent, 300);
            logger.info("QR code generated for card {}, size: {} bytes", this.getId(),
                    this.getQrCode() != null ? this.getQrCode().length : 0);
        } catch (Exception e) {
            logger.error("Error generating QR code for card {}: {}", this.getId(), e.getMessage(), e);
        }
    }

    /**
     * Gera o conteúdo do QR code no mesmo formato que os tickets
     */
    private String generateQrContent() {
        return String.format(
                "CARD:%s|ZONE:%s|VALID_UNTIL:%s|HOLDER:%s",
                this.getId(),
                this.zone != null ? this.zone.getName() : "ALL_ZONES",
                this.getValidUntil(),
                this.getUser() != null ? this.getUser().getName() : "Unknown");
    }

    /**
     * Obtém o QR code, gerando se necessário
     */
    public byte[] getQrCode() {
        if (super.getQrCode() == null || super.getQrCode().length == 0) {
            logger.info("QR code not found for card {}, generating now...", this.getId());
            generateQrCodeForCard();
        }
        return super.getQrCode();
    }

    public Zone getZone() {
        return this.zone;
    }

    public void setZone(Zone _zone) {
        this.zone = _zone;
    }
}