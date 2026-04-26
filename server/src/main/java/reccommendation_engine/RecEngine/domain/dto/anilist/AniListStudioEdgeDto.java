package reccommendation_engine.RecEngine.domain.dto.anilist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AniListStudioEdgeDto {

    private Boolean isMain;
    private AniListStudioNodeDto node;
}
