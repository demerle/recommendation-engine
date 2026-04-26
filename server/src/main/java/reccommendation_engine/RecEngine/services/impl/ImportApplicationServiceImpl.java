package reccommendation_engine.RecEngine.services.impl;

import org.springframework.stereotype.Service;
import reccommendation_engine.RecEngine.domain.dto.imports.ImportResponseDto;
import reccommendation_engine.RecEngine.domain.dto.imports.ImportStatusDto;
import reccommendation_engine.RecEngine.domain.entities.IngestionRun;
import reccommendation_engine.RecEngine.domain.entities.MediaType;
import reccommendation_engine.RecEngine.domain.entities.User;
import reccommendation_engine.RecEngine.mappers.ImportRunMapper;
import reccommendation_engine.RecEngine.repositories.IngestionRunRepository;
import reccommendation_engine.RecEngine.repositories.UserRepository;
import reccommendation_engine.RecEngine.services.AniListImportService;
import reccommendation_engine.RecEngine.services.ImportApplicationService;
import reccommendation_engine.RecEngine.services.UserNotFoundException;

import java.util.Optional;

@Service
public class ImportApplicationServiceImpl implements ImportApplicationService {

    private final UserRepository userRepository;
    private final IngestionRunRepository ingestionRunRepository;
    private final AniListImportService aniListImportService;
    private final ImportRunMapper importRunMapper;

    public ImportApplicationServiceImpl(
            UserRepository userRepository,
            IngestionRunRepository ingestionRunRepository,
            AniListImportService aniListImportService,
            ImportRunMapper importRunMapper
    ) {
        this.userRepository = userRepository;
        this.ingestionRunRepository = ingestionRunRepository;
        this.aniListImportService = aniListImportService;
        this.importRunMapper = importRunMapper;
    }

    @Override
    public ImportResponseDto triggerAniListImport(Long userId, String mediaType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        MediaType resolvedMediaType = MediaType.valueOf(mediaType.trim().toUpperCase());
        IngestionRun ingestionRun = aniListImportService.importUserMediaList(user, resolvedMediaType);
        return importRunMapper.toImportResponseDto(ingestionRun);
    }

    @Override
    public Optional<ImportStatusDto> getLatestAniListImportStatus(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        return ingestionRunRepository.findFirstByUserIdOrderByStartedAtDesc(userId)
                .map(importRunMapper::toImportStatusDto);
    }
}
