package reccommendation_engine.RecEngine.domain.dto.anilist;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AniListMediaListCollectionDataDto {

    @JsonProperty("MediaListCollection")
    private AniListMediaListCollectionDto mediaListCollection;
}
