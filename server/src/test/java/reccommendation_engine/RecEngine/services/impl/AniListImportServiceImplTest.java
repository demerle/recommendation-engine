package reccommendation_engine.RecEngine.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListCollectionDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListEntryDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListGroupDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.normalized.NormalizedAniListMedia;
import reccommendation_engine.RecEngine.domain.entities.IngestionError;
import reccommendation_engine.RecEngine.domain.entities.IngestionRun;
import reccommendation_engine.RecEngine.domain.entities.IngestionRunStatus;
import reccommendation_engine.RecEngine.domain.entities.InteractionSource;
import reccommendation_engine.RecEngine.domain.entities.InteractionStatus;
import reccommendation_engine.RecEngine.domain.entities.Item;
import reccommendation_engine.RecEngine.domain.entities.ItemMetadata;
import reccommendation_engine.RecEngine.domain.entities.MediaType;
import reccommendation_engine.RecEngine.domain.entities.User;
import reccommendation_engine.RecEngine.domain.entities.UserItemInteraction;
import reccommendation_engine.RecEngine.repositories.IngestionErrorRepository;
import reccommendation_engine.RecEngine.repositories.IngestionRunRepository;
import reccommendation_engine.RecEngine.repositories.ItemMetadataRepository;
import reccommendation_engine.RecEngine.repositories.ItemRepository;
import reccommendation_engine.RecEngine.repositories.UserItemInteractionRepository;
import reccommendation_engine.RecEngine.services.AniListClient;
import reccommendation_engine.RecEngine.services.AniListNormalizationService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AniListImportServiceImplTest {

    @Mock
    private AniListClient aniListClient;

    @Mock
    private AniListNormalizationService aniListNormalizationService;

    @Mock
    private IngestionRunRepository ingestionRunRepository;

    @Mock
    private IngestionErrorRepository ingestionErrorRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMetadataRepository itemMetadataRepository;

    @Mock
    private UserItemInteractionRepository userItemInteractionRepository;

    @InjectMocks
    private AniListImportServiceImpl aniListImportService;

    @BeforeEach
    void setUp() {
        aniListImportService = new AniListImportServiceImpl(
                aniListClient,
                aniListNormalizationService,
                ingestionRunRepository,
                ingestionErrorRepository,
                itemRepository,
                itemMetadataRepository,
                userItemInteractionRepository,
                new ObjectMapper()
        );

        when(ingestionRunRepository.save(any(IngestionRun.class))).thenAnswer(invocation -> {
            IngestionRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(1L);
            }
            return run;
        });
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            if (item.getId() == null) {
                item.setId(100L);
            }
            return item;
        });
        when(itemMetadataRepository.save(any(ItemMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userItemInteractionRepository.save(any(UserItemInteraction.class))).thenAnswer(invocation -> {
            UserItemInteraction interaction = invocation.getArgument(0);
            if (interaction.getId() == null) {
                interaction.setId(200L);
            }
            return interaction;
        });
        when(ingestionErrorRepository.save(any(IngestionError.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void importUserMediaListPersistsNormalizedDataAndMarksRunSucceeded() {
        User user = sampleUser();
        AniListMediaListEntryDto entry = sampleEntry(5001L, 16498L);
        AniListMediaListCollectionDto collection = collectionWithEntries(entry);
        NormalizedAniListMedia normalizedMedia = sampleNormalizedMedia();
        UserItemInteraction interaction = sampleInteraction(user, normalizedMedia.getItem(), "anilist-entry:5001");

        when(aniListClient.fetchUserMediaList("eren", MediaType.ANIME)).thenReturn(collection);
        when(aniListNormalizationService.normalizeMedia(entry)).thenReturn(normalizedMedia);
        when(itemRepository.findByExternalSourceAndExternalId("anilist", "16498")).thenReturn(Optional.empty());
        when(itemMetadataRepository.findByItemId(anyLong())).thenReturn(Optional.empty());
        when(aniListNormalizationService.normalizeInteraction(eq(entry), eq(user), any(Item.class), any(OffsetDateTime.class)))
                .thenAnswer(invocation -> sampleInteraction(user, invocation.getArgument(2), "anilist-entry:5001"));
        when(userItemInteractionRepository.findBySourceAndSourceEventId(InteractionSource.ANILIST_IMPORT, "anilist-entry:5001"))
                .thenReturn(Optional.empty());
        when(userItemInteractionRepository.findByUserIdAndItemIdAndStatusAndInteractionTimestampAndSource(
                eq(7L), eq(100L), eq(InteractionStatus.COMPLETED), eq(interaction.getInteractionTimestamp()), eq(InteractionSource.ANILIST_IMPORT)))
                .thenReturn(Optional.empty());

        IngestionRun result = aniListImportService.importUserMediaList(user, MediaType.ANIME);

        assertEquals(IngestionRunStatus.SUCCEEDED, result.getStatus());
        assertEquals(0, result.getErrorCount());
        assertNotNull(result.getCompletedAt());
        assertTrue(result.getSummaryJson().contains("\"entriesSeen\":1"));
        verify(itemRepository).save(any(Item.class));
        verify(itemMetadataRepository).save(any(ItemMetadata.class));
        verify(userItemInteractionRepository).save(any(UserItemInteraction.class));
        verify(ingestionErrorRepository, never()).save(any(IngestionError.class));
    }

    @Test
    void importUserMediaListSkipsDuplicateInteractionWithoutFailingRun() {
        User user = sampleUser();
        AniListMediaListEntryDto entry = sampleEntry(5001L, 16498L);
        AniListMediaListCollectionDto collection = collectionWithEntries(entry);
        NormalizedAniListMedia normalizedMedia = sampleNormalizedMedia();
        UserItemInteraction interaction = sampleInteraction(user, normalizedMedia.getItem(), "anilist-entry:5001");

        when(aniListClient.fetchUserMediaList("eren", MediaType.ANIME)).thenReturn(collection);
        when(aniListNormalizationService.normalizeMedia(entry)).thenReturn(normalizedMedia);
        when(itemRepository.findByExternalSourceAndExternalId("anilist", "16498")).thenReturn(Optional.empty());
        when(itemMetadataRepository.findByItemId(anyLong())).thenReturn(Optional.empty());
        when(aniListNormalizationService.normalizeInteraction(eq(entry), eq(user), any(Item.class), any(OffsetDateTime.class)))
                .thenAnswer(invocation -> sampleInteraction(user, invocation.getArgument(2), "anilist-entry:5001"));
        when(userItemInteractionRepository.findBySourceAndSourceEventId(InteractionSource.ANILIST_IMPORT, "anilist-entry:5001"))
                .thenReturn(Optional.of(interaction));

        IngestionRun result = aniListImportService.importUserMediaList(user, MediaType.ANIME);

        assertEquals(IngestionRunStatus.SUCCEEDED, result.getStatus());
        assertTrue(result.getSummaryJson().contains("\"skippedRecords\":1"));
        verify(userItemInteractionRepository, never()).save(any(UserItemInteraction.class));
    }

    @Test
    void importUserMediaListMarksRunPartialWhenOneEntryFails() {
        User user = sampleUser();
        AniListMediaListEntryDto goodEntry = sampleEntry(5001L, 16498L);
        AniListMediaListEntryDto badEntry = sampleEntry(5002L, 16499L);
        AniListMediaListCollectionDto collection = collectionWithEntries(goodEntry, badEntry);
        NormalizedAniListMedia normalizedMedia = sampleNormalizedMedia();
        UserItemInteraction interaction = sampleInteraction(user, normalizedMedia.getItem(), "anilist-entry:5001");

        when(aniListClient.fetchUserMediaList("eren", MediaType.ANIME)).thenReturn(collection);
        when(aniListNormalizationService.normalizeMedia(goodEntry)).thenReturn(normalizedMedia);
        when(aniListNormalizationService.normalizeMedia(badEntry)).thenThrow(new IllegalStateException("bad record"));
        when(itemRepository.findByExternalSourceAndExternalId(anyString(), anyString())).thenReturn(Optional.empty());
        when(itemMetadataRepository.findByItemId(anyLong())).thenReturn(Optional.empty());
        when(aniListNormalizationService.normalizeInteraction(eq(goodEntry), eq(user), any(Item.class), any(OffsetDateTime.class)))
                .thenAnswer(invocation -> sampleInteraction(user, invocation.getArgument(2), "anilist-entry:5001"));
        when(userItemInteractionRepository.findBySourceAndSourceEventId(any(), anyString())).thenReturn(Optional.empty());
        when(userItemInteractionRepository.findByUserIdAndItemIdAndStatusAndInteractionTimestampAndSource(
                eq(7L), eq(100L), eq(InteractionStatus.COMPLETED), eq(interaction.getInteractionTimestamp()), eq(InteractionSource.ANILIST_IMPORT)))
                .thenReturn(Optional.empty());

        IngestionRun result = aniListImportService.importUserMediaList(user, MediaType.ANIME);

        assertEquals(IngestionRunStatus.PARTIAL, result.getStatus());
        assertEquals(1, result.getErrorCount());
        assertTrue(result.getSummaryJson().contains("\"errorCount\":1"));
        verify(ingestionErrorRepository).save(any(IngestionError.class));
    }

    @Test
    void importUserMediaListMarksRunFailedWhenAniListCallFails() {
        User user = sampleUser();
        when(aniListClient.fetchUserMediaList("eren", MediaType.ANIME)).thenThrow(new RuntimeException("AniList unavailable"));

        IngestionRun result = aniListImportService.importUserMediaList(user, MediaType.ANIME);

        assertEquals(IngestionRunStatus.FAILED, result.getStatus());
        assertEquals(1, result.getErrorCount());
        assertTrue(result.getSummaryJson().contains("\"errorCount\":1"));
        verify(itemRepository, never()).save(any(Item.class));
        verify(ingestionErrorRepository, never()).save(any(IngestionError.class));
    }

    private User sampleUser() {
        return User.builder()
                .id(7L)
                .anilistUsername("eren")
                .build();
    }

    private AniListMediaListEntryDto sampleEntry(Long entryId, Long mediaId) {
        AniListMediaDto media = new AniListMediaDto();
        media.setId(mediaId);

        AniListMediaListEntryDto entry = new AniListMediaListEntryDto();
        entry.setId(entryId);
        entry.setMedia(media);
        return entry;
    }

    private AniListMediaListCollectionDto collectionWithEntries(AniListMediaListEntryDto... entries) {
        AniListMediaListGroupDto group = new AniListMediaListGroupDto();
        group.setEntries(List.of(entries));

        AniListMediaListCollectionDto collection = new AniListMediaListCollectionDto();
        collection.setLists(List.of(group));
        return collection;
    }

    private NormalizedAniListMedia sampleNormalizedMedia() {
        Item item = Item.builder()
                .externalSource("anilist")
                .externalId("16498")
                .mediaType(MediaType.ANIME)
                .canonicalTitle("Attack on Titan")
                .isActive(true)
                .build();

        ItemMetadata metadata = ItemMetadata.builder()
                .item(item)
                .genres(new String[]{"Action"})
                .tags(new String[]{"Military"})
                .studios(new String[]{"WIT STUDIO"})
                .authors(new String[]{"Hajime Isayama"})
                .metadataJson("{}")
                .metadataVersion("v1")
                .build();

        return NormalizedAniListMedia.builder()
                .item(item)
                .itemMetadata(metadata)
                .build();
    }

    private UserItemInteraction sampleInteraction(User user, Item item, String sourceEventId) {
        return UserItemInteraction.builder()
                .user(user)
                .item(item)
                .status(InteractionStatus.COMPLETED)
                .source(InteractionSource.ANILIST_IMPORT)
                .sourceEventId(sourceEventId)
                .interactionTimestamp(OffsetDateTime.parse("2026-04-26T04:00:00Z"))
                .sourcePayload("{}")
                .build();
    }
}
