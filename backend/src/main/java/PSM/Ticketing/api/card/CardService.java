package PSM.Ticketing.api.card;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import PSM.Location.Zone;
import PSM.Location.api.zone.ZoneRepository;
import PSM.Ticketing.Card;

@Service
public class CardService {
    private static final Logger logger = LoggerFactory.getLogger(CardService.class);
    private final CardRepository repository;
    private final ZoneRepository zoneRepository;

    public CardService(CardRepository repository, ZoneRepository zoneRepository) {
        this.repository = repository;
        this.zoneRepository = zoneRepository;
    }

    public List<Card> findAll() {
        return repository.findAll();
    }

    public Card findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Card not found"));
    }

public Card create(Card entity) {
    
    if (entity.getZone() != null && entity.getZone().getId() != null) {
        Zone zone = zoneRepository.findById(entity.getZone().getId())
                .orElseThrow(() -> new RuntimeException("Zone not found with id: " + entity.getZone().getId()));
        entity.setZone(zone);
        System.out.println("Zone found and assigned: " + zone.getName());
    } else {
        System.out.println("No zone associated with this card");
    }
    
    Card saved = repository.save(entity);
    return saved;
}

    public Card update(UUID id, Card entity) {
        findById(id);
        if (entity.getZone() != null && entity.getZone().getId() != null) {
            Zone zone = zoneRepository.findById(entity.getZone().getId())
                    .orElseThrow(() -> new RuntimeException("Zone not found"));
            entity.setZone(zone);
        }
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
    
public byte[] getQrCode(UUID id) {
    Card card = findById(id);
    if (card.getQrCode() == null || card.getQrCode().length == 0) {
        String qrText = "CARD:" + id;
        card.generateQrCode(qrText, 300);
        return update(id, card).getQrCode();
    }
    return card.getQrCode();
}
}