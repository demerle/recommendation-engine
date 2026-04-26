package reccommendation_engine.RecEngine.mappers.impl;

import org.springframework.stereotype.Component;
import reccommendation_engine.RecEngine.domain.entities.InteractionStatus;
import reccommendation_engine.RecEngine.domain.entities.MediaType;
import reccommendation_engine.RecEngine.mappers.AniListEnumMapper;

@Component
public class AniListEnumMapperImpl implements AniListEnumMapper {

    @Override
    public InteractionStatus toInteractionStatus(String aniListStatus) {
        if (aniListStatus == null || aniListStatus.isBlank()) {
            return InteractionStatus.PLANNED;
        }

        return switch (aniListStatus.trim().toUpperCase()) {
            case "PLANNING" -> InteractionStatus.PLANNED;
            case "CURRENT" -> InteractionStatus.IN_PROGRESS;
            case "COMPLETED" -> InteractionStatus.COMPLETED;
            case "DROPPED" -> InteractionStatus.DROPPED;
            case "PAUSED" -> InteractionStatus.PAUSED;
            default -> InteractionStatus.PLANNED;
        };
    }

    @Override
    public MediaType toMediaType(String aniListMediaType) {
        if (aniListMediaType == null || aniListMediaType.isBlank()) {
            throw new IllegalArgumentException("AniList media type is required");
        }

        return switch (aniListMediaType.trim().toUpperCase()) {
            case "ANIME" -> MediaType.ANIME;
            case "MANGA" -> MediaType.MANGA;
            default -> throw new IllegalArgumentException("Unsupported AniList media type: " + aniListMediaType);
        };
    }
}
