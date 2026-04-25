package PSM.Ticketing.api.card;

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

import PSM.Ticketing.Card;

@RestController
@RequestMapping("/api/cards")
public class CardController {
    private final CardService service;

    public CardController(CardService service) {
        this.service = service;
    }

    @GetMapping
    public List<Card> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Card getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    public Card create(@RequestBody Card entity) {
        return service.create(entity);
    }

    @PutMapping("/{id}")
    public Card update(@PathVariable UUID id, @RequestBody Card entity) {
        return service.update(id, entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
