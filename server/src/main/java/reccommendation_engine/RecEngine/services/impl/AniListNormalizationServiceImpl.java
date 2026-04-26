package reccommendation_engine.RecEngine.services.impl;

import org.springframework.stereotype.Service;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListEntryDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.normalized.NormalizedAniListMedia;
import reccommendation_engine.RecEngine.domain.entities.Item;
import reccommendation_engine.RecEngine.domain.entities.User;
import reccommendation_engine.RecEngine.domain.entities.UserItemInteraction;
import reccommendation_engine.RecEngine.mappers.AniListInteractionMapper;
import reccommendation_engine.RecEngine.mappers.AniListItemMapper;
import reccommendation_engine.RecEngine.services.AniListNormalizationService;

import java.time.OffsetDateTime;

@Service
public class AniListNormalizationServiceImpl implements AniListNormalizationService {

    private final AniListItemMapper aniListItemMapper;
    private final AniListInteractionMapper aniListInteractionMapper;

    public AniListNormalizationServiceImpl(
            AniListItemMapper aniListItemMapper,
            AniListInteractionMapper aniListInteractionMapper
    ) {
        this.aniListItemMapper = aniListItemMapper;
        this.aniListInteractionMapper = aniListInteractionMapper;
    }

    @Override
    public NormalizedAniListMedia normalizeMedia(AniListMediaListEntryDto aniListEntry) {
        if (aniListEntry == null || aniListEntry.getMedia() == null) {
            throw new IllegalArgumentException("AniList entry media is required");
        }
        return aniListItemMapper.fromAniListMediaDto(aniListEntry.getMedia());
    }

    @Override
    public UserItemInteraction normalizeInteraction(
            AniListMediaListEntryDto aniListEntry,
            User user,
            Item item,
            OffsetDateTime fallbackTimestamp
    ) {
        return aniListInteractionMapper.fromAniListEntry(aniListEntry, user, item, fallbackTimestamp);
    }
}
