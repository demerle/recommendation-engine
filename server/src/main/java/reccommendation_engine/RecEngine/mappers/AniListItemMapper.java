package reccommendation_engine.RecEngine.mappers;

import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.normalized.NormalizedAniListMedia;

public interface AniListItemMapper {

    NormalizedAniListMedia fromAniListMediaDto(AniListMediaDto aniListMediaDto);
}
