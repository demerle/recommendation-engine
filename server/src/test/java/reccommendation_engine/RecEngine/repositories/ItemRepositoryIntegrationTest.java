package reccommendation_engine.RecEngine.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import reccommendation_engine.RecEngine.PostgresIntegrationTestBase;
import reccommendation_engine.RecEngine.TestDataUtil;
import reccommendation_engine.RecEngine.domain.entities.Item;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
class ItemRepositoryIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void testThatItemRepositoryCanCreateUpdateDeleteAndFindByExternalSourceAndExternalId() {
        Item item = TestDataUtil.createTestItemA();

        Item savedItem = itemRepository.save(item);
        Optional<Item> foundItem = itemRepository.findByExternalSourceAndExternalId("anilist", "16498");

        assertThat(foundItem).isPresent();
        assertThat(foundItem.get().getCanonicalTitle()).isEqualTo("Attack on Titan");
        assertThat(foundItem.get().getMediaType()).isEqualTo(reccommendation_engine.RecEngine.domain.entities.MediaType.ANIME);

        foundItem.get().setCanonicalTitle("Attack on Titan Updated");
        itemRepository.save(foundItem.get());

        Optional<Item> updatedItem = itemRepository.findByExternalSourceAndExternalId("anilist", "16498");
        assertThat(updatedItem).isPresent();
        assertThat(updatedItem.get().getCanonicalTitle()).isEqualTo("Attack on Titan Updated");

        itemRepository.deleteById(savedItem.getId());

        assertThat(itemRepository.findByExternalSourceAndExternalId("anilist", "16498")).isEmpty();
        assertThat(itemRepository.findAll()).isEmpty();
    }
}
