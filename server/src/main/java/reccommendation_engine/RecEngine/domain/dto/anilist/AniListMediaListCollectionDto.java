package reccommendation_engine.RecEngine.domain.dto.anilist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AniListMediaListCollectionDto {

    private List<AniListMediaListGroupDto> lists;
    private String userName;
    private String type;
}
