package reccommendation_engine.RecEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reccommendation_engine.RecEngine.domain.entities.ItemMetadata;

import java.util.Optional;

@Repository
public interface ItemMetadataRepository extends JpaRepository<ItemMetadata, Long> {

    Optional<ItemMetadata> findByItemId(Long itemId);
}
