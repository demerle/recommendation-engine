package reccommendation_engine.RecEngine.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListCollectionDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListEntryDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListGroupDto;
import reccommendation_engine.RecEngine.domain.dto.anilist.normalized.NormalizedAniListMedia;
import reccommendation_engine.RecEngine.domain.entities.IngestionError;
import reccommendation_engine.RecEngine.domain.entities.IngestionRun;
import reccommendation_engine.RecEngine.domain.entities.IngestionRunStatus;
import reccommendation_engine.RecEngine.domain.entities.Item;
import reccommendation_engine.RecEngine.domain.entities.ItemMetadata;
import reccommendation_engine.RecEngine.domain.entities.MediaType;
import reccommendation_engine.RecEngine.domain.entities.User;
import reccommendation_engine.RecEngine.domain.entities.UserItemInteraction;
import reccommendation_engine.RecEngine.repositories.IngestionErrorRepository;
import reccommendation_engine.RecEngine.repositories.IngestionRunRepository;
import reccommendation_engine.RecEngine.repositories.ItemMetadataRepository;
import reccommendation_engine.RecEngine.repositories.ItemRepository;
import reccommendation_engine.RecEngine.repositories.UserItemInteractionRepository;
import reccommendation_engine.RecEngine.services.AniListClient;
import reccommendation_engine.RecEngine.services.AniListImportService;
import reccommendation_engine.RecEngine.services.AniListNormalizationService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AniListImportServiceImpl implements AniListImportService {

    private final AniListClient aniListClient;
    private final AniListNormalizationService aniListNormalizationService;
    private final IngestionRunRepository ingestionRunRepository;
    private final IngestionErrorRepository ingestionErrorRepository;
    private final ItemRepository itemRepository;
    private final ItemMetadataRepository itemMetadataRepository;
    private final UserItemInteractionRepository userItemInteractionRepository;
    private final ObjectMapper objectMapper;

    public AniListImportServiceImpl(
            AniListClient aniListClient,
            AniListNormalizationService aniListNormalizationService,
            IngestionRunRepository ingestionRunRepository,
            IngestionErrorRepository ingestionErrorRepository,
            ItemRepository itemRepository,
            ItemMetadataRepository itemMetadataRepository,
            UserItemInteractionRepository userItemInteractionRepository,
            ObjectMapper objectMapper
    ) {
        this.aniListClient = aniListClient;
        this.aniListNormalizationService = aniListNormalizationService;
        this.ingestionRunRepository = ingestionRunRepository;
        this.ingestionErrorRepository = ingestionErrorRepository;
        this.itemRepository = itemRepository;
        this.itemMetadataRepository = itemMetadataRepository;
        this.userItemInteractionRepository = userItemInteractionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public IngestionRun importUserMediaList(User user, MediaType mediaType) {
        OffsetDateTime importStartedAt = OffsetDateTime.now(ZoneOffset.UTC);
        IngestionRun ingestionRun = ingestionRunRepository.save(
                IngestionRun.builder()
                        .user(user)
                        .status(IngestionRunStatus.RUNNING)
                        .startedAt(importStartedAt)
                        .build()
        );

        AniListMediaListCollectionDto collection;
        try {
            collection = aniListClient.fetchUserMediaList(resolveUsername(user), mediaType);
        } catch (RuntimeException ex) {
            ingestionRun.setStatus(IngestionRunStatus.FAILED);
            ingestionRun.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
            ingestionRun.setSummaryJson(writeSummaryJson(Map.of(
                    "entriesSeen", 0,
                    "itemsUpserted", 0,
                    "interactionsInserted", 0,
                    "skippedRecords", 0,
                    "errorCount", 1
            )));
            ingestionRun.setErrorCount(1);
            return ingestionRunRepository.save(ingestionRun);
        }

        List<AniListMediaListEntryDto> entries = flattenEntries(collection);
        ImportCounters counters = new ImportCounters();

        for (AniListMediaListEntryDto entry : entries) {
            counters.entriesSeen++;

            if (entry == null || entry.getMedia() == null || entry.getMedia().getId() == null) {
                counters.skippedRecords++;
                persistIngestionError(ingestionRun, "INVALID_ENTRY", "AniList entry or media identity was missing", entry);
                counters.errorCount++;
                continue;
            }

            try {
                NormalizedAniListMedia normalizedMedia = aniListNormalizationService.normalizeMedia(entry);
                Item persistedItem = upsertItem(normalizedMedia.getItem());
                upsertItemMetadata(persistedItem, normalizedMedia.getItemMetadata());

                UserItemInteraction interaction = aniListNormalizationService.normalizeInteraction(
                        entry,
                        user,
                        persistedItem,
                        importStartedAt
                );

                if (persistInteractionIfNew(interaction)) {
                    counters.interactionsInserted++;
                } else {
                    counters.skippedRecords++;
                }

                counters.itemsUpserted++;
            } catch (RuntimeException ex) {
                counters.skippedRecords++;
                persistIngestionError(ingestionRun, "ENTRY_IMPORT_FAILED", ex.getMessage(), entry);
                counters.errorCount++;
            }
        }

        ingestionRun.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        ingestionRun.setErrorCount(counters.errorCount);
        ingestionRun.setStatus(resolveFinalStatus(counters));
        ingestionRun.setSummaryJson(writeSummaryJson(Map.of(
                "entriesSeen", counters.entriesSeen,
                "itemsUpserted", counters.itemsUpserted,
                "interactionsInserted", counters.interactionsInserted,
                "skippedRecords", counters.skippedRecords,
                "errorCount", counters.errorCount
        )));
        return ingestionRunRepository.save(ingestionRun);
    }

    private String resolveUsername(User user) {
        if (user == null || user.getAnilistUsername() == null || user.getAnilistUsername().isBlank()) {
            throw new IllegalArgumentException("User must have an AniList username before import");
        }
        return user.getAnilistUsername();
    }

    private List<AniListMediaListEntryDto> flattenEntries(AniListMediaListCollectionDto collection) {
        List<AniListMediaListEntryDto> entries = new ArrayList<>();
        if (collection == null || collection.getLists() == null) {
            return entries;
        }

        for (AniListMediaListGroupDto group : collection.getLists()) {
            if (group == null || group.getEntries() == null) {
                continue;
            }
            entries.addAll(group.getEntries());
        }
        return entries;
    }

    private Item upsertItem(Item normalizedItem) {
        Optional<Item> existingItem = itemRepository.findByExternalSourceAndExternalId(
                normalizedItem.getExternalSource(),
                normalizedItem.getExternalId()
        );

        if (existingItem.isPresent()) {
            Item persistedItem = existingItem.get();
            persistedItem.setMediaType(normalizedItem.getMediaType());
            persistedItem.setCanonicalTitle(normalizedItem.getCanonicalTitle());
            persistedItem.setTitleEnglish(normalizedItem.getTitleEnglish());
            persistedItem.setTitleRomaji(normalizedItem.getTitleRomaji());
            persistedItem.setTitleNative(normalizedItem.getTitleNative());
            persistedItem.setIsActive(normalizedItem.getIsActive());
            return itemRepository.save(persistedItem);
        }

        return itemRepository.save(normalizedItem);
    }

    private void upsertItemMetadata(Item persistedItem, ItemMetadata normalizedMetadata) {
        Optional<ItemMetadata> existingMetadata = itemMetadataRepository.findByItemId(persistedItem.getId());

        ItemMetadata metadataToSave = existingMetadata.orElseGet(ItemMetadata::new);
        metadataToSave.setItemId(persistedItem.getId());
        metadataToSave.setItem(persistedItem);
        metadataToSave.setGenres(normalizedMetadata.getGenres());
        metadataToSave.setTags(normalizedMetadata.getTags());
        metadataToSave.setStudios(normalizedMetadata.getStudios());
        metadataToSave.setAuthors(normalizedMetadata.getAuthors());
        metadataToSave.setFormat(normalizedMetadata.getFormat());
        metadataToSave.setStatus(normalizedMetadata.getStatus());
        metadataToSave.setEpisodesOrChapters(normalizedMetadata.getEpisodesOrChapters());
        metadataToSave.setYearStart(normalizedMetadata.getYearStart());
        metadataToSave.setSynopsis(normalizedMetadata.getSynopsis());
        metadataToSave.setMetadataJson(normalizedMetadata.getMetadataJson());
        metadataToSave.setMetadataVersion(normalizedMetadata.getMetadataVersion());
        itemMetadataRepository.save(metadataToSave);
    }

    private boolean persistInteractionIfNew(UserItemInteraction interaction) {
        if (interaction.getSourceEventId() != null
                && userItemInteractionRepository.findBySourceAndSourceEventId(interaction.getSource(), interaction.getSourceEventId()).isPresent()) {
            return false;
        }

        if (userItemInteractionRepository.findByUserIdAndItemIdAndStatusAndInteractionTimestampAndSource(
                interaction.getUser().getId(),
                interaction.getItem().getId(),
                interaction.getStatus(),
                interaction.getInteractionTimestamp(),
                interaction.getSource()
        ).isPresent()) {
            return false;
        }

        userItemInteractionRepository.save(interaction);
        return true;
    }

    private void persistIngestionError(IngestionRun ingestionRun, String errorCode, String errorMessage, Object payload) {
        ingestionErrorRepository.save(
                IngestionError.builder()
                        .ingestionRun(ingestionRun)
                        .errorCode(errorCode)
                        .errorMessage(errorMessage != null ? errorMessage : "Unknown import error")
                        .payloadJson(writePayloadJson(payload))
                        .build()
        );
        ingestionRun.setErrorCount((ingestionRun.getErrorCount() == null ? 0 : ingestionRun.getErrorCount()) + 1);
    }

    private IngestionRunStatus resolveFinalStatus(ImportCounters counters) {
        if (counters.errorCount == 0) {
            return IngestionRunStatus.SUCCEEDED;
        }
        if (counters.entriesSeen == 0 || counters.interactionsInserted == 0 && counters.itemsUpserted == 0) {
            return IngestionRunStatus.FAILED;
        }
        return IngestionRunStatus.PARTIAL;
    }

    private String writeSummaryJson(Map<String, Object> summary) {
        return writePayloadJson(new LinkedHashMap<>(summary));
    }

    private String writePayloadJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize import payload", ex);
        }
    }

    private static class ImportCounters {
        private int entriesSeen;
        private int itemsUpserted;
        private int interactionsInserted;
        private int skippedRecords;
        private int errorCount;
    }
}
