package PSM.Ticketing.api.ticket;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import PSM.Ticketing.Ticket;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    @EntityGraph(attributePaths = {"fromStop", "toStop"})
    Optional<Ticket> findById(UUID id);

    // Nova query que busca os bilhetes do utilizador sem carregar o QR Code binário
    @Query("SELECT new PSM.Ticketing.api.ticket.TicketDTO(" +
           "t.id, t.createdAt, t.validFrom, t.validUntil, t.price, t.stateName, " +
           "f.id, f.name, o.id, o.name) " +
           "FROM Ticket t " +
           "JOIN t.fromStop f " +
           "JOIN t.toStop o " +
           "WHERE t.user.id = :userId")
    List<TicketDTO> findTicketsSummaryByUserId(@Param("userId") UUID userId);
}