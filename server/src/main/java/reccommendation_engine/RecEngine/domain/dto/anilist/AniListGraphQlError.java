package reccommendation_engine.RecEngine.domain.dto.anilist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AniListGraphQlError {

    private String message;
    private List<Integer> path;
    private Map<String, Object> extensions;
}
