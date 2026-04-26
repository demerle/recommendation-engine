package reccommendation_engine.RecEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reccommendation_engine.RecEngine.domain.entities.IngestionRun;
import reccommendation_engine.RecEngine.domain.entities.IngestionRunStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IngestionRunRepository extends JpaRepository<IngestionRun, Long> {

    Optional<IngestionRun> findByRunId(UUID runId);

    List<IngestionRun> findByUserIdOrderByStartedAtDesc(Long userId);

    Optional<IngestionRun> findFirstByUserIdOrderByStartedAtDesc(Long userId);

    List<IngestionRun> findByStatusOrderByStartedAtDesc(IngestionRunStatus status);
}
