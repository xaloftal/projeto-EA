package PSM.Ticketing.api.title;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import PSM.Ticketing.Title;

public interface TitleRepository extends JpaRepository<Title, UUID>{
    
    @EntityGraph(attributePaths = {"from", "to", "zone"})
    Optional<Title> findById(UUID id);

    List<Title> findByStateName(String stateName);
}
