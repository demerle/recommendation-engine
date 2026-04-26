package reccommendation_engine.RecEngine.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import reccommendation_engine.RecEngine.PostgresIntegrationTestBase;
import reccommendation_engine.RecEngine.TestDataUtil;
import reccommendation_engine.RecEngine.domain.entities.InteractionSource;
import reccommendation_engine.RecEngine.domain.entities.InteractionStatus;
import reccommendation_engine.RecEngine.domain.entities.Item;
import reccommendation_engine.RecEngine.domain.entities.User;
import reccommendation_engine.RecEngine.domain.entities.UserItemInteraction;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
class UserItemInteractionRepositoryIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserItemInteractionRepository userItemInteractionRepository;

    @Test
    void testThatUserItemInteractionRepositoryCanCreateReadAndFindByIdempotencyKeys() {
        User savedUser = userRepository.save(TestDataUtil.createTestUserA());
        Item savedItem = itemRepository.save(TestDataUtil.createTestItemA());
        UserItemInteraction interaction = TestDataUtil.createTestInteractionA(savedUser, savedItem);

        UserItemInteraction savedInteraction = userItemInteractionRepository.save(interaction);

        List<UserItemInteraction> byUser = userItemInteractionRepository.findByUserIdOrderByInteractionTimestampDesc(savedUser.getId());
        List<UserItemInteraction> byItem = userItemInteractionRepository.findByItemIdOrderByInteractionTimestampDesc(savedItem.getId());
        Optional<UserItemInteraction> bySourceEvent = userItemInteractionRepository.findBySourceAndSourceEventId(
                InteractionSource.ANILIST_IMPORT,
                "anilist-entry:5001"
        );
        Optional<UserItemInteraction> byFallbackKey = userItemInteractionRepository
                .findByUserIdAndItemIdAndStatusAndInteractionTimestampAndSource(
                        savedUser.getId(),
                        savedItem.getId(),
                        InteractionStatus.COMPLETED,
                        OffsetDateTime.of(2026, 4, 26, 12, 5, 0, 0, java.time.ZoneOffset.UTC),
                        InteractionSource.ANILIST_IMPORT
                );

        assertThat(byUser).hasSize(1);
        assertThat(byItem).hasSize(1);
        assertThat(bySourceEvent).isPresent();
        assertThat(byFallbackKey).isPresent();
        assertThat(bySourceEvent.get().getId()).isEqualTo(savedInteraction.getId());

        userItemInteractionRepository.delete(savedInteraction);

        assertThat(userItemInteractionRepository.findByUserIdOrderByInteractionTimestampDesc(savedUser.getId())).isEmpty();
    }
}
