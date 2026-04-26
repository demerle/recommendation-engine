package reccommendation_engine.RecEngine.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reccommendation_engine.RecEngine.domain.dto.imports.ImportRequestDto;
import reccommendation_engine.RecEngine.domain.dto.imports.ImportResponseDto;
import reccommendation_engine.RecEngine.domain.dto.imports.ImportStatusDto;
import reccommendation_engine.RecEngine.services.ImportApplicationService;
import reccommendation_engine.RecEngine.services.UserNotFoundException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImportController.class)
@WithMockUser
class ImportControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportApplicationService importApplicationService;

    @Test
    void triggerAniListImportReturnsCreatedForValidRequest() throws Exception {
        ImportResponseDto responseDto = ImportResponseDto.builder()
                .ingestionRunId(1L)
                .runId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
                .userId(7L)
                .source("ANILIST")
                .status("SUCCEEDED")
                .errorCount(0)
                .summaryJson("{\"entriesSeen\":1}")
                .startedAt(OffsetDateTime.parse("2026-04-26T04:00:00Z"))
                .completedAt(OffsetDateTime.parse("2026-04-26T04:01:00Z"))
                .build();

        when(importApplicationService.triggerAniListImport(7L, "anime")).thenReturn(responseDto);

        mockMvc.perform(post("/api/imports/anilist")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ImportRequestDto(7L, "anime"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingestionRunId").value(1))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void triggerAniListImportReturnsBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/imports/anilist")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ImportRequestDto(null, ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("userId and mediaType are required"));
    }

    @Test
    void triggerAniListImportReturnsNotFoundWhenUserDoesNotExist() throws Exception {
        when(importApplicationService.triggerAniListImport(99L, "anime"))
                .thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(post("/api/imports/anilist")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ImportRequestDto(99L, "anime"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found: 99"));
    }

    @Test
    void getLatestAniListImportStatusReturnsOkWhenRunExists() throws Exception {
        ImportStatusDto statusDto = ImportStatusDto.builder()
                .ingestionRunId(2L)
                .runId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"))
                .userId(7L)
                .status("PARTIAL")
                .errorCount(1)
                .summaryJson("{\"errorCount\":1}")
                .startedAt(OffsetDateTime.parse("2026-04-26T04:00:00Z"))
                .completedAt(OffsetDateTime.parse("2026-04-26T04:01:00Z"))
                .build();

        when(importApplicationService.getLatestAniListImportStatus(7L)).thenReturn(Optional.of(statusDto));

        mockMvc.perform(get("/api/imports/anilist/7/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingestionRunId").value(2))
                .andExpect(jsonPath("$.status").value("PARTIAL"));
    }

    @Test
    void getLatestAniListImportStatusReturnsNotFoundWhenNoRunExists() throws Exception {
        when(importApplicationService.getLatestAniListImportStatus(7L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/imports/anilist/7/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No import run found for user 7"));
    }
}
