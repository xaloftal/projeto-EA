package PSM.Ticketing.api.card;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import PSM.Ticketing.Card;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {
}
