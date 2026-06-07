package PSM.Ticketing.api.ticketpack;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import PSM.Ticketing.TicketPack;
import PSM.Ticketing.Title;
import PSM.Ticketing.api.title.TitleRepository;

@Service
public class TicketPackService {
    private final TicketPackRepository repository;
    private final TitleRepository titleRepository;


    public TicketPackService(TicketPackRepository repository, TitleRepository titleRepository) {
        this.repository = repository;
        this.titleRepository=titleRepository;
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

    public TicketPack createAndBindPack(List<Title> generatedTitles, double discountValue) {    
        
        if (generatedTitles == null || generatedTitles.isEmpty()) {
            throw new IllegalArgumentException("Error creating the pack.");
        }
        // 1. Instanciar e preencher o TicketPack baseado nas colunas da BD (id, discount)
        TicketPack pack = new TicketPack();
        pack.setId(UUID.randomUUID());
        pack.setDiscount(BigDecimal.valueOf(discountValue));

        // 2. Persistir o pack na tabela catchit.ticketpack
        TicketPack savedPack = repository.save(pack);

        // 3. Atualizar a coluna ticketpack_id em cada Title comprado
        for (Title title : generatedTitles) {
            title.setTicketPack(savedPack); // Método set que liga a chave @ManyToOne
        }

        // 4. Salvar em lote os títulos modificados com o vínculo do pack correto
        titleRepository.saveAll(generatedTitles);

        return savedPack;
    }

    public TicketPack update(UUID id, TicketPack entity) {
        findById(id);
        return repository.save(entity);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
