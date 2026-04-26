package reccommendation_engine.RecEngine.domain.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportStatusDto {

    private Long ingestionRunId;
    private UUID runId;
    private Long userId;
    private String status;
    private Integer errorCount;
    private String summaryJson;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
}
