package PSM.Ticketing;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "ticketpack", schema = "catchit")
public class TicketPack {

    @Id
    private UUID id;

    @Column(name = "discount", precision = 38, scale = 2)
    private BigDecimal discount;

    @OneToMany(mappedBy = "ticketPack", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Title> titles;

    // Getters e Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public List<Title> getTitles() { return titles; }
    public void setTitles(List<Title> titles) { this.titles = titles; }
}