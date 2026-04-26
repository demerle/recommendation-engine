package reccommendation_engine.RecEngine.services.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import reccommendation_engine.RecEngine.PostgresIntegrationTestBase;
import reccommendation_engine.RecEngine.TestDataUtil;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListCollectionDto;
import reccommendation_engine.RecEngine.domain.entities.IngestionError;
import reccommendation_engine.RecEngine.domain.entities.IngestionRun;
import reccommendation_engine.RecEngine.domain.entities.IngestionRunStatus;
import reccommendation_engine.RecEngine.domain.entities.Item;
import reccommendation_engine.RecEngine.domain.entities.User;
import reccommendation_engine.RecEngine.repositories.IngestionErrorRepository;
import reccommendation_engine.RecEngine.repositories.IngestionRunRepository;
import reccommendation_engine.RecEngine.repositories.ItemMetadataRepository;
import reccommendation_engine.RecEngine.repositories.ItemRepository;
import reccommendation_engine.RecEngine.repositories.UserItemInteractionRepository;
import reccommendation_engine.RecEngine.repositories.UserRepository;
import reccommendation_engine.RecEngine.services.AniListClient;
import reccommendation_engine.RecEngine.services.AniListImportService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
class AniListImportServiceIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private AniListImportService aniListImportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemMetadataRepository itemMetadataRepository;

    @Autowired
    private UserItemInteractionRepository userItemInteractionRepository;

    @Autowired
    private IngestionRunRepository ingestionRunRepository;

    @Autowired
    private IngestionErrorRepository ingestionErrorRepository;

    @MockitoBean
    private AniListClient aniListClient;

    @Test
    void testThatAniListImportServicePersistsImportDataAndReImportDoesNotDuplicateInteractions() {
        User savedUser = userRepository.save(TestDataUtil.createTestUserA());
        AniListMediaListCollectionDto collection = TestDataUtil.createAniListCollectionA();

        when(aniListClient.fetchUserMediaList("test-anilist-a", reccommendation_engine.RecEngine.domain.entities.MediaType.ANIME))
                .thenReturn(collection);

        IngestionRun firstRun = aniListImportService.importUserMediaList(
                savedUser,
                reccommendation_engine.RecEngine.domain.entities.MediaType.ANIME
        );
        IngestionRun secondRun = aniListImportService.importUserMediaList(
                savedUser,
                reccommendation_engine.RecEngine.domain.entities.MediaType.ANIME
        );

        List<IngestionError> ingestionErrors = ingestionErrorRepository.findAll();
        String errorSummary = ingestionErrors.stream()
                .map(error -> "%s: %s".formatted(error.getErrorCode(), error.getErrorMessage()))
                .reduce((left, right) -> left + " | " + right)
                .orElse("none");

        assertThat(firstRun.getStatus())
                .describedAs("first run errors: %s", errorSummary)
                .isEqualTo(IngestionRunStatus.SUCCEEDED);
        assertThat(secondRun.getStatus())
                .describedAs("second run errors: %s", errorSummary)
                .isEqualTo(IngestionRunStatus.SUCCEEDED);

        assertThat(itemRepository.findAll()).hasSize(1);
        Item persistedItem = itemRepository.findAll().getFirst();
        assertThat(persistedItem.getExternalId()).isEqualTo("16498");
        assertThat(itemMetadataRepository.findByItemId(persistedItem.getId())).isPresent();
        assertThat(userItemInteractionRepository.findByUserIdOrderByInteractionTimestampDesc(savedUser.getId())).hasSize(1);
        assertThat(ingestionRunRepository.findByUserIdOrderByStartedAtDesc(savedUser.getId())).hasSize(2);
        assertThat(ingestionErrorRepository.findAll()).isEmpty();
    }
}
