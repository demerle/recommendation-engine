package reccommendation_engine.RecEngine.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import reccommendation_engine.RecEngine.PostgresIntegrationTestBase;
import reccommendation_engine.RecEngine.TestDataUtil;
import reccommendation_engine.RecEngine.domain.entities.IngestionRun;
import reccommendation_engine.RecEngine.domain.entities.IngestionRunStatus;
import reccommendation_engine.RecEngine.domain.entities.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
class IngestionRunRepositoryIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IngestionRunRepository ingestionRunRepository;

    @Test
    void testThatIngestionRunRepositoryCanCreateReadUpdateAndFindLatestRun() {
        User savedUser = userRepository.save(TestDataUtil.createTestUserA());
        IngestionRun ingestionRun = TestDataUtil.createTestIngestionRunA(savedUser);

        IngestionRun savedRun = ingestionRunRepository.save(ingestionRun);
        Optional<IngestionRun> byRunId = ingestionRunRepository.findByRunId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        List<IngestionRun> byUser = ingestionRunRepository.findByUserIdOrderByStartedAtDesc(savedUser.getId());
        Optional<IngestionRun> latestByUser = ingestionRunRepository.findFirstByUserIdOrderByStartedAtDesc(savedUser.getId());

        assertThat(byRunId).isPresent();
        assertThat(byUser).hasSize(1);
        assertThat(latestByUser).isPresent();
        assertThat(latestByUser.get().getId()).isEqualTo(savedRun.getId());

        savedRun.setStatus(IngestionRunStatus.PARTIAL);
        savedRun.setErrorCount(1);
        ingestionRunRepository.save(savedRun);

        List<IngestionRun> byStatus = ingestionRunRepository.findByStatusOrderByStartedAtDesc(IngestionRunStatus.PARTIAL);

        assertThat(byStatus).hasSize(1);
        assertThat(byStatus.get(0).getErrorCount()).isEqualTo(1);
    }
}
