package reccommendation_engine.RecEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reccommendation_engine.RecEngine.domain.entities.IngestionError;

import java.util.List;

@Repository
public interface IngestionErrorRepository extends JpaRepository<IngestionError, Long> {

    List<IngestionError> findByIngestionRunIdOrderByCreatedAtAsc(Long ingestionRunId);
}
