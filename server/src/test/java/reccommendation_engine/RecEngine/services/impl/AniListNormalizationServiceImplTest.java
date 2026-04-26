package reccommendation_engine.RecEngine.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListDateDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListEntryDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaTitleDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStaffConnectionDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStaffEdgeDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStaffNameDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStaffNodeDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStudioConnectionDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStudioEdgeDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStudioNodeDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListTagDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.normalized.NormalizedAniListMedia;
import reccommendation_engine.RecEngine.domain.entities.InteractionSource;
import reccommendation_engine.RecEngine.domain.entities.InteractionStatus;
import reccommendation_engine.RecEngine.domain.entities.Item;
import reccommendation_engine.RecEngine.domain.entities.MediaType;
import reccommendation_engine.RecEngine.domain.entities.User;
import reccommendation_engine.RecEngine.domain.entities.UserItemInteraction;
import reccommendation_engine.RecEngine.mappers.AniListInteractionMapper;
import reccommendation_engine.RecEngine.mappers.AniListItemMapper;
import reccommendation_engine.RecEngine.mappers.impl.AniListEnumMapperImpl;
import reccommendation_engine.RecEngine.mappers.impl.AniListInteractionMapperImpl;
import reccommendation_engine.RecEngine.mappers.impl.AniListItemMapperImpl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AniListNormalizationServiceImplTest {

    private AniListNormalizationServiceImpl normalizationService;

    @BeforeEach
    void setUp() {
        ModelMapper modelMapper = new ModelMapper();
        ObjectMapper objectMapper = new ObjectMapper();
        AniListEnumMapperImpl enumMapper = new AniListEnumMapperImpl();
        AniListItemMapper itemMapper = new AniListItemMapperImpl(modelMapper, objectMapper, enumMapper);
        AniListInteractionMapper interactionMapper = new AniListInteractionMapperImpl(modelMapper, objectMapper, enumMapper);
        normalizationService = new AniListNormalizationServiceImpl(itemMapper, interactionMapper);
    }

    @Test
    void normalizeMediaAppliesTitlePriorityAndMetadataExtraction() {
        AniListMediaListEntryDto entry = sampleEntry();
        entry.getMedia().getTitle().setEnglish("");

        NormalizedAniListMedia normalizedMedia = normalizationService.normalizeMedia(entry);

        assertEquals("Shingeki no Kyojin", normalizedMedia.getItem().getCanonicalTitle());
        assertEquals(MediaType.ANIME, normalizedMedia.getItem().getMediaType());
        assertArrayEquals(new String[]{"Action", "Drama"}, normalizedMedia.getItemMetadata().getGenres());
        assertArrayEquals(new String[]{"Military", "Survival"}, normalizedMedia.getItemMetadata().getTags());
        assertArrayEquals(new String[]{"WIT STUDIO"}, normalizedMedia.getItemMetadata().getStudios());
        assertArrayEquals(new String[]{"Hajime Isayama"}, normalizedMedia.getItemMetadata().getAuthors());
        assertNotNull(normalizedMedia.getItemMetadata().getMetadataJson());
    }

    @Test
    void normalizeInteractionMapsStatusAndFallbackTimestampDeterministically() {
        AniListMediaListEntryDto entry = sampleEntry();
        entry.setId(null);
        entry.setUpdatedAt(null);

        User user = User.builder()
                .id(7L)
                .anilistUsername("eren")
                .build();
        Item item = Item.builder()
                .id(99L)
                .externalId("16498")
                .build();
        OffsetDateTime fallbackTimestamp = OffsetDateTime.of(2026, 4, 26, 0, 30, 0, 0, ZoneOffset.UTC);

        UserItemInteraction interaction = normalizationService.normalizeInteraction(entry, user, item, fallbackTimestamp);

        assertEquals(InteractionStatus.IN_PROGRESS, interaction.getStatus());
        assertEquals(InteractionSource.ANILIST_IMPORT, interaction.getSource());
        assertEquals(fallbackTimestamp, interaction.getInteractionTimestamp());
        assertEquals("anilist-fallback:eren:16498:CURRENT:0", interaction.getSourceEventId());
        assertNotNull(interaction.getSourcePayload());
    }

    @Test
    void normalizeInteractionUsesAniListEntryIdWhenAvailable() {
        AniListMediaListEntryDto entry = sampleEntry();
        User user = User.builder().id(1L).build();
        Item item = Item.builder().id(2L).externalId("16498").build();

        UserItemInteraction interaction = normalizationService.normalizeInteraction(
                entry,
                user,
                item,
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        assertEquals("anilist-entry:5001", interaction.getSourceEventId());
        assertEquals(OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(1714102800L), ZoneOffset.UTC),
                interaction.getInteractionTimestamp());
    }

    private AniListMediaListEntryDto sampleEntry() {
        AniListMediaDto media = new AniListMediaDto();
        media.setId(16498L);
        media.setType("ANIME");
        media.setFormat("TV");
        media.setStatus("FINISHED");
        media.setEpisodes(25);
        media.setDescription("Humanity fights titans.");
        media.setGenres(List.of("Action", "Drama"));
        media.setTags(List.of(
                new AniListTagDto("Military"),
                new AniListTagDto("Survival")
        ));
        media.setStartDate(new AniListDateDto(2013, 4, 7));
        media.setTitle(new AniListMediaTitleDto("", "Shingeki no Kyojin", "進撃の巨人"));
        media.setStudios(new AniListStudioConnectionDto(List.of(
                new AniListStudioEdgeDto(true, new AniListStudioNodeDto("WIT STUDIO"))
        )));
        media.setStaff(new AniListStaffConnectionDto(List.of(
                new AniListStaffEdgeDto("Original Creator", List.of(), new AniListStaffNodeDto(new AniListStaffNameDto("Hajime Isayama")))
        )));

        AniListMediaListEntryDto entry = new AniListMediaListEntryDto();
        entry.setId(5001L);
        entry.setStatus("CURRENT");
        entry.setProgress(12);
        entry.setScore(8.5);
        entry.setUpdatedAt(1714102800L);
        entry.setMedia(media);
        return entry;
    }
}
