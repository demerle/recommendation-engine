package reccommendation_engine.RecEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reccommendation_engine.RecEngine.domain.entities.Item;

import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    Optional<Item> findByExternalSourceAndExternalId(String externalSource, String externalId);
}
