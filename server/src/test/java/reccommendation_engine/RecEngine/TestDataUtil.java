package reccommendation_engine.RecEngine;

import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListCollectionDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListEntryDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListGroupDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaTitleDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListTagDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStudioConnectionDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStudioEdgeDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStudioNodeDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStaffConnectionDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStaffEdgeDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStaffNodeDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStaffNameDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListDateDto;
import reccommendation_engine.RecEngine.domain.entities.IngestionRun;
import reccommendation_engine.RecEngine.domain.entities.IngestionRunStatus;
import reccommendation_engine.RecEngine.domain.entities.IngestionSource;
import reccommendation_engine.RecEngine.domain.entities.InteractionSource;
import reccommendation_engine.RecEngine.domain.entities.InteractionStatus;
import reccommendation_engine.RecEngine.domain.entities.Item;
import reccommendation_engine.RecEngine.domain.entities.ItemMetadata;
import reccommendation_engine.RecEngine.domain.entities.MediaType;
import reccommendation_engine.RecEngine.domain.entities.User;
import reccommendation_engine.RecEngine.domain.entities.UserItemInteraction;
import reccommendation_engine.RecEngine.domain.entities.UserStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public final class TestDataUtil {

    private TestDataUtil() {
    }

    public static User createTestUserA() {
        return User.builder()
                .uuid(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .email("user-a@example.com")
                .username("user-a")
                .anilistUsername("test-anilist-a")
                .status(UserStatus.ACTIVE)
                .createdAt(OffsetDateTime.of(2026, 4, 26, 12, 0, 0, 0, ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.of(2026, 4, 26, 12, 0, 0, 0, ZoneOffset.UTC))
                .build();
    }

    public static Item createTestItemA() {
        return Item.builder()
                .externalSource("anilist")
                .externalId("16498")
                .mediaType(MediaType.ANIME)
                .canonicalTitle("Attack on Titan")
                .titleEnglish("Attack on Titan")
                .titleRomaji("Shingeki no Kyojin")
                .titleNative("進撃の巨人")
                .isActive(true)
                .createdAt(OffsetDateTime.of(2026, 4, 26, 12, 0, 0, 0, ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.of(2026, 4, 26, 12, 0, 0, 0, ZoneOffset.UTC))
                .build();
    }

    public static ItemMetadata createTestItemMetadataA(Item item) {
        return ItemMetadata.builder()
                .item(item)
                .genres(new String[]{"Action", "Drama"})
                .tags(new String[]{"Military", "Survival"})
                .studios(new String[]{"WIT STUDIO"})
                .authors(new String[]{"Hajime Isayama"})
                .format("TV")
                .status("FINISHED")
                .episodesOrChapters(25)
                .yearStart(2013)
                .synopsis("Humanity fights titans.")
                .metadataJson("{\"source\":\"test\"}")
                .metadataVersion("v1")
                .updatedAt(OffsetDateTime.of(2026, 4, 26, 12, 0, 0, 0, ZoneOffset.UTC))
                .build();
    }

    public static UserItemInteraction createTestInteractionA(User user, Item item) {
        return UserItemInteraction.builder()
                .user(user)
                .item(item)
                .status(InteractionStatus.COMPLETED)
                .progress(java.math.BigDecimal.valueOf(25))
                .rating(java.math.BigDecimal.valueOf(9.0))
                .interactionTimestamp(OffsetDateTime.of(2026, 4, 26, 12, 5, 0, 0, ZoneOffset.UTC))
                .source(InteractionSource.ANILIST_IMPORT)
                .sourceEventId("anilist-entry:5001")
                .sourcePayload("{\"entryId\":5001}")
                .insertedAt(OffsetDateTime.of(2026, 4, 26, 12, 5, 0, 0, ZoneOffset.UTC))
                .build();
    }

    public static IngestionRun createTestIngestionRunA(User user) {
        return IngestionRun.builder()
                .user(user)
                .runId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .source(IngestionSource.ANILIST)
                .status(IngestionRunStatus.SUCCEEDED)
                .startedAt(OffsetDateTime.of(2026, 4, 26, 12, 10, 0, 0, ZoneOffset.UTC))
                .completedAt(OffsetDateTime.of(2026, 4, 26, 12, 11, 0, 0, ZoneOffset.UTC))
                .summaryJson("{\"entriesSeen\":1}")
                .errorCount(0)
                .build();
    }

    public static AniListMediaListCollectionDto createAniListCollectionA() {
        AniListMediaDto media = new AniListMediaDto();
        media.setId(16498L);
        media.setType("ANIME");
        media.setFormat("TV");
        media.setStatus("FINISHED");
        media.setTitle(new AniListMediaTitleDto("Attack on Titan", "Shingeki no Kyojin", "進撃の巨人"));
        media.setGenres(List.of("Action", "Drama"));
        media.setTags(List.of(new AniListTagDto("Military"), new AniListTagDto("Survival")));
        media.setStudios(new AniListStudioConnectionDto(List.of(
                new AniListStudioEdgeDto(true, new AniListStudioNodeDto("WIT STUDIO"))
        )));
        media.setStaff(new AniListStaffConnectionDto(List.of(
                new AniListStaffEdgeDto("Original Creator", List.of(), new AniListStaffNodeDto(new AniListStaffNameDto("Hajime Isayama")))
        )));
        media.setEpisodes(25);
        media.setStartDate(new AniListDateDto(2013, 4, 7));
        media.setDescription("Humanity fights titans.");

        AniListMediaListEntryDto entry = new AniListMediaListEntryDto();
        entry.setId(5001L);
        entry.setStatus("COMPLETED");
        entry.setProgress(25);
        entry.setScore(9.0);
        entry.setUpdatedAt(1714102800L);
        entry.setMedia(media);

        AniListMediaListGroupDto group = new AniListMediaListGroupDto();
        group.setName("Completed");
        group.setStatus("COMPLETED");
        group.setEntries(List.of(entry));

        AniListMediaListCollectionDto collection = new AniListMediaListCollectionDto();
        collection.setUserName("test-anilist-a");
        collection.setType("ANIME");
        collection.setLists(List.of(group));
        return collection;
    }
}
