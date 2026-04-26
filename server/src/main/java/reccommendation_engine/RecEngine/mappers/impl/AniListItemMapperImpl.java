package reccommendation_engine.RecEngine.mappers.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStaffEdgeDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListStudioEdgeDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListTagDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.normalized.NormalizedAniListMedia;
import reccommendation_engine.RecEngine.domain.entities.Item;
import reccommendation_engine.RecEngine.domain.entities.ItemMetadata;
import reccommendation_engine.RecEngine.mappers.AniListEnumMapper;
import reccommendation_engine.RecEngine.mappers.AniListItemMapper;

import java.util.ArrayList;
import java.util.List;

@Component
public class AniListItemMapperImpl implements AniListItemMapper {

    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final AniListEnumMapper aniListEnumMapper;

    public AniListItemMapperImpl(ModelMapper modelMapper, ObjectMapper objectMapper, AniListEnumMapper aniListEnumMapper) {
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.aniListEnumMapper = aniListEnumMapper;
    }

    @Override
    public NormalizedAniListMedia fromAniListMediaDto(AniListMediaDto aniListMediaDto) {
        Item item = modelMapper.map(aniListMediaDto, Item.class);
        item.setId(null);
        item.setExternalSource("anilist");
        item.setExternalId(String.valueOf(aniListMediaDto.getId()));
        item.setMediaType(aniListEnumMapper.toMediaType(aniListMediaDto.getType()));
        item.setCanonicalTitle(resolveCanonicalTitle(aniListMediaDto));
        item.setTitleEnglish(aniListMediaDto.getTitle() != null ? aniListMediaDto.getTitle().getEnglish() : null);
        item.setTitleRomaji(aniListMediaDto.getTitle() != null ? aniListMediaDto.getTitle().getRomaji() : null);
        item.setTitleNative(aniListMediaDto.getTitle() != null ? aniListMediaDto.getTitle().getNativeTitle() : null);
        item.setIsActive(Boolean.TRUE);

        ItemMetadata itemMetadata = ItemMetadata.builder()
                .itemId(null)
                .item(item)
                .genres(toArray(aniListMediaDto.getGenres()))
                .tags(extractTags(aniListMediaDto))
                .studios(extractStudios(aniListMediaDto))
                .authors(extractAuthors(aniListMediaDto))
                .format(aniListMediaDto.getFormat())
                .status(aniListMediaDto.getStatus())
                .episodesOrChapters(resolveEpisodesOrChapters(aniListMediaDto))
                .yearStart(aniListMediaDto.getStartDate() != null ? aniListMediaDto.getStartDate().getYear() : null)
                .synopsis(aniListMediaDto.getDescription())
                .metadataJson(writeJson(aniListMediaDto))
                .metadataVersion("v1")
                .build();

        return NormalizedAniListMedia.builder()
                .item(item)
                .itemMetadata(itemMetadata)
                .build();
    }

    private String resolveCanonicalTitle(AniListMediaDto aniListMediaDto) {
        if (aniListMediaDto.getTitle() == null) {
            return "anilist:" + aniListMediaDto.getId();
        }
        if (hasText(aniListMediaDto.getTitle().getEnglish())) {
            return aniListMediaDto.getTitle().getEnglish();
        }
        if (hasText(aniListMediaDto.getTitle().getRomaji())) {
            return aniListMediaDto.getTitle().getRomaji();
        }
        if (hasText(aniListMediaDto.getTitle().getNativeTitle())) {
            return aniListMediaDto.getTitle().getNativeTitle();
        }
        return "anilist:" + aniListMediaDto.getId();
    }

    private Integer resolveEpisodesOrChapters(AniListMediaDto aniListMediaDto) {
        return aniListMediaDto.getEpisodes() != null ? aniListMediaDto.getEpisodes() : aniListMediaDto.getChapters();
    }

    private String[] extractTags(AniListMediaDto aniListMediaDto) {
        if (aniListMediaDto.getTags() == null) {
            return new String[0];
        }

        List<String> tags = new ArrayList<>();
        for (AniListTagDto tag : aniListMediaDto.getTags()) {
            if (tag != null && hasText(tag.getName())) {
                tags.add(tag.getName());
            }
        }
        return tags.toArray(String[]::new);
    }

    private String[] extractStudios(AniListMediaDto aniListMediaDto) {
        if (aniListMediaDto.getStudios() == null || aniListMediaDto.getStudios().getEdges() == null) {
            return new String[0];
        }

        List<String> studios = new ArrayList<>();
        for (AniListStudioEdgeDto edge : aniListMediaDto.getStudios().getEdges()) {
            if (edge != null && edge.getNode() != null && hasText(edge.getNode().getName())) {
                studios.add(edge.getNode().getName());
            }
        }
        return studios.toArray(String[]::new);
    }

    private String[] extractAuthors(AniListMediaDto aniListMediaDto) {
        if (aniListMediaDto.getStaff() == null || aniListMediaDto.getStaff().getEdges() == null) {
            return new String[0];
        }

        List<String> authors = new ArrayList<>();
        for (AniListStaffEdgeDto edge : aniListMediaDto.getStaff().getEdges()) {
            if (edge != null
                    && edge.getNode() != null
                    && edge.getNode().getName() != null
                    && hasText(edge.getNode().getName().getFull())) {
                authors.add(edge.getNode().getName().getFull());
            }
        }
        return authors.toArray(String[]::new);
    }

    private String[] toArray(List<String> values) {
        if (values == null) {
            return new String[0];
        }
        return values.stream()
                .filter(this::hasText)
                .toArray(String[]::new);
    }

    private String writeJson(AniListMediaDto aniListMediaDto) {
        try {
            return objectMapper.writeValueAsString(aniListMediaDto);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize AniList media payload", ex);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
