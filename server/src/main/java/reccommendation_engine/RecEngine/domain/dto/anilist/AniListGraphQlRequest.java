package reccommendation_engine.RecEngine.domain.dto.anilist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AniListGraphQlRequest {

    private String query;
    private Map<String, Object> variables;
}
