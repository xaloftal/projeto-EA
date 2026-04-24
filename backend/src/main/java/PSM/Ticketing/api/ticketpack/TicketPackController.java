package PSM.Ticketing.api.ticketpack;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import PSM.Ticketing.TicketPack;

@RestController
@RequestMapping("/api/ticketpacks")
public class TicketPackController {
    private final TicketPackService service;

    public TicketPackController(TicketPackService service) {
        this.service = service;
    }

    @GetMapping
    public List<TicketPack> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public TicketPack getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public TicketPack create(@RequestBody TicketPack entity) {
        return service.create(entity);
    }

    @PutMapping("/{id}")
    public TicketPack update(@PathVariable UUID id, @RequestBody TicketPack entity) {
        return service.update(id, entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
