package reccommendation_engine.RecEngine.domain.dto.anilist.normalized;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import reccommendation_engine.RecEngine.domain.entities.Item;
import reccommendation_engine.RecEngine.domain.entities.ItemMetadata;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NormalizedAniListMedia {

    private Item item;
    private ItemMetadata itemMetadata;
}
