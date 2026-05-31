package PSM.Ticketing.api.card;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.Ticketing.Card;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {

    @EntityGraph(attributePaths = {"zone"})
    Optional<Card> findById(UUID id);
}
