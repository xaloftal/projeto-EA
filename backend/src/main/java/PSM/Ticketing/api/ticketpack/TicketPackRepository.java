package PSM.Ticketing.api.ticketpack;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.Ticketing.TicketPack;

@Repository
public interface TicketPackRepository extends JpaRepository<TicketPack, UUID> {
}
