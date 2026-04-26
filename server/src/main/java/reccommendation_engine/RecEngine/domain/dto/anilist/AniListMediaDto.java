package reccommendation_engine.RecEngine.domain.dto.anilist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AniListMediaDto {

    private Long id;
    private String type;
    private String format;
    private String status;
    private AniListMediaTitleDto title;
    private List<String> genres;
    private List<AniListTagDto> tags;
    private AniListStudioConnectionDto studios;
    private AniListStaffConnectionDto staff;
    private Integer episodes;
    private Integer chapters;
    private AniListDateDto startDate;
    private String description;
}
