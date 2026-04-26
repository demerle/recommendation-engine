package reccommendation_engine.RecEngine.mappers;

import reccommendation_engine.RecEngine.domain.dto.imports.ImportResponseDto;
import reccommendation_engine.RecEngine.domain.dto.imports.ImportStatusDto;
import reccommendation_engine.RecEngine.domain.entities.IngestionRun;

public interface ImportRunMapper {

    ImportResponseDto toImportResponseDto(IngestionRun ingestionRun);

    ImportStatusDto toImportStatusDto(IngestionRun ingestionRun);
}
