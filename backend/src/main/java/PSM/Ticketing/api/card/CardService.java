package PSM.Ticketing.api.card;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import PSM.Ticketing.Card;

@Service
public class CardService {
    private final CardRepository repository;

    public CardService(CardRepository repository) {
        this.repository = repository;
    }

    public List<Card> findAll() {
        return repository.findAll();
    }

    public Card findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Card not found"));
    }

    public Card create(Card entity) {
        return repository.save(entity);
    }

    public Card update(UUID id, Card entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
