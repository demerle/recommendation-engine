package reccommendation_engine.RecEngine.services;

import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListEntryDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.normalized.NormalizedAniListMedia;
import reccommendation_engine.RecEngine.domain.entities.Item;
import reccommendation_engine.RecEngine.domain.entities.User;
import reccommendation_engine.RecEngine.domain.entities.UserItemInteraction;

import java.time.OffsetDateTime;

public interface AniListNormalizationService {

    NormalizedAniListMedia normalizeMedia(AniListMediaListEntryDto aniListEntry);

    UserItemInteraction normalizeInteraction(
            AniListMediaListEntryDto aniListEntry,
            User user,
            Item item,
            OffsetDateTime fallbackTimestamp
    );
}
