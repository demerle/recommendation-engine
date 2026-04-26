package reccommendation_engine.RecEngine.domain.dto.anilist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AniListStaffEdgeDto {

    private String role;
    private List<String> roleNotes;
    private AniListStaffNodeDto node;
}
