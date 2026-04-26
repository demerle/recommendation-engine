package reccommendation_engine.RecEngine.services;

import reccommendation_engine.RecEngine.domain.dto.imports.ImportResponseDto;
import reccommendation_engine.RecEngine.domain.dto.imports.ImportStatusDto;

import java.util.Optional;

public interface ImportApplicationService {

    ImportResponseDto triggerAniListImport(Long userId, String mediaType);

    Optional<ImportStatusDto> getLatestAniListImportStatus(Long userId);
}
