package PSM.Ticketing.api.ticket;

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

import PSM.Ticketing.Ticket;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @GetMapping
    public List<Ticket> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Ticket getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public Ticket create(@RequestBody Ticket entity) {
        return service.create(entity);
    }

    @PutMapping("/{id}")
    public Ticket update(@PathVariable UUID id, @RequestBody Ticket entity) {
        return service.update(id, entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
