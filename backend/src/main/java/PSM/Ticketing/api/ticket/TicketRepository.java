package PSM.Ticketing.api.ticket;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.Ticketing.Ticket;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
}
