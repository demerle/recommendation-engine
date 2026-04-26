package reccommendation_engine.RecEngine.services;

import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListCollectionDto;
import reccommendation_engine.RecEngine.domain.entities.MediaType;

public interface AniListClient {

    AniListMediaListCollectionDto fetchUserMediaList(String username, MediaType mediaType);
}
