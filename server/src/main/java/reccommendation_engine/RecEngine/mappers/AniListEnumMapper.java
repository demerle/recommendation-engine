package reccommendation_engine.RecEngine.mappers;

import reccommendation_engine.RecEngine.domain.entities.InteractionStatus;
import reccommendation_engine.RecEngine.domain.entities.MediaType;

public interface AniListEnumMapper {

    InteractionStatus toInteractionStatus(String aniListStatus);

    MediaType toMediaType(String aniListMediaType);
}
