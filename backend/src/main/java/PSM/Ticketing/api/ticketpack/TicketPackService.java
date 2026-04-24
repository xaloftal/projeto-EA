package PSM.Ticketing.api.ticketpack;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import PSM.Ticketing.TicketPack;

@Service
public class TicketPackService {
    private final TicketPackRepository repository;

    public TicketPackService(TicketPackRepository repository) {
        this.repository = repository;
    }

    public List<TicketPack> findAll() {
        return repository.findAll();
    }

    public TicketPack findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("TicketPack not found"));
    }

    public TicketPack create(TicketPack entity) {
        return repository.save(entity);
    }

    public TicketPack update(UUID id, TicketPack entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
