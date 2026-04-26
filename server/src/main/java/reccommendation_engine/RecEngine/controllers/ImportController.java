package reccommendation_engine.RecEngine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reccommendation_engine.RecEngine.domain.dto.imports.ImportRequestDto;
import reccommendation_engine.RecEngine.domain.dto.imports.ImportResponseDto;
import reccommendation_engine.RecEngine.domain.dto.imports.ImportStatusDto;
import reccommendation_engine.RecEngine.services.AniListClientException;
import reccommendation_engine.RecEngine.services.ImportApplicationService;
import reccommendation_engine.RecEngine.services.UserNotFoundException;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = "/api/imports/anilist")
public class ImportController {

    private final ImportApplicationService importApplicationService;

    public ImportController(ImportApplicationService importApplicationService) {
        this.importApplicationService = importApplicationService;
    }

    @PostMapping
    public ResponseEntity<?> triggerAniListImport(@RequestBody ImportRequestDto importRequestDto) {
        if (importRequestDto == null || importRequestDto.getUserId() == null || importRequestDto.getMediaType() == null
                || importRequestDto.getMediaType().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "userId and mediaType are required"));
        }

        try {
            ImportResponseDto responseDto = importApplicationService.triggerAniListImport(
                    importRequestDto.getUserId(),
                    importRequestDto.getMediaType()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (UserNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (AniListClientException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping(path = "/{userId}/latest")
    public ResponseEntity<?> getLatestAniListImportStatus(@PathVariable Long userId) {
        try {
            Optional<ImportStatusDto> latestStatus = importApplicationService.getLatestAniListImportStatus(userId);
            if (latestStatus.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No import run found for user " + userId));
            }
            return ResponseEntity.ok(latestStatus.get());
        } catch (UserNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        }
    }
}
