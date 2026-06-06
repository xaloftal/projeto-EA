package PSM.Ticketing.api.ticket;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import PSM.Ticketing.Ticket;

@Service
public class TicketService {
    private final TicketRepository repository;

    public TicketService(TicketRepository repository) {
        this.repository = repository;
    }

    public List<Ticket> findAll() {
        return repository.findAll();
    }

    public Ticket findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Ticket not found"));
    }

    public Ticket create(Ticket entity) {
        return repository.save(entity);
    }

    public Ticket update(UUID id, Ticket entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    public List<TicketDTO> findTicketsByUserId(UUID userId) {
    return repository.findTicketsSummaryByUserId(userId);
    }

    public byte[] getQrCode(UUID id) {
    Ticket ticket = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("Ticket not found"));
    return ticket.getQrCode();
    }
}
