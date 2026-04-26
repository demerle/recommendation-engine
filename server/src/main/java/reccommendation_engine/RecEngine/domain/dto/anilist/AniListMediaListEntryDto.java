package reccommendation_engine.RecEngine.domain.dto.anilist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AniListMediaListEntryDto {

    private Long id;
    private String status;
    private Integer progress;
    private Double score;
    private Long updatedAt;
    private AniListMediaDto media;
}
