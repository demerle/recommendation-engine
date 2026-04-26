package reccommendation_engine.RecEngine.domain.dto.anilist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AniListMediaListGroupDto {

    private String name;
    private String status;
    private List<AniListMediaListEntryDto> entries;
}
