package reccommendation_engine.RecEngine.mappers;

import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListEntryDto;
import reccommendation_engine.RecEngine.domain.entities.Item;
import reccommendation_engine.RecEngine.domain.entities.User;
import reccommendation_engine.RecEngine.domain.entities.UserItemInteraction;

import java.time.OffsetDateTime;

public interface AniListInteractionMapper {

    UserItemInteraction fromAniListEntry(
            AniListMediaListEntryDto aniListEntry,
            User user,
            Item item,
            OffsetDateTime fallbackTimestamp
    );
}
