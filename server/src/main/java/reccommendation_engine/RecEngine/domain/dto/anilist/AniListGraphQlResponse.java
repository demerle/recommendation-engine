package reccommendation_engine.RecEngine.domain.dto.anilist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AniListGraphQlResponse<T> {

    private T data;
    private List<AniListGraphQlError> errors;

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}
