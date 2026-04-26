package reccommendation_engine.RecEngine.domain.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportRequestDto {

    private Long userId;
    private String mediaType;
}
