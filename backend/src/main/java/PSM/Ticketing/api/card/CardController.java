package PSM.Ticketing.api.card;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    
@GetMapping("/{id}/qrcode")
public ResponseEntity<byte[]> getCardQrCode(@PathVariable UUID id) {
    try {
        Card card = service.findById(id);
        byte[] qrCode = card.getQrCode();
        
        if (qrCode == null || qrCode.length == 0) {
            // Gerar QR code se não existir
            String qrText = "CARD:" + id;
            card.generateQrCode(qrText, 300);
            service.update(id, card);
            qrCode = card.getQrCode();
        }
        
        if (qrCode == null || qrCode.length == 0) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    } catch (Exception e) {
        return ResponseEntity.internalServerError().build();
    }
}
}