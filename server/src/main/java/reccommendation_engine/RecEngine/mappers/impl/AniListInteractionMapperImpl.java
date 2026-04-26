package reccommendation_engine.RecEngine.mappers.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import reccommendation_engine.RecEngine.domain.dto.anilist.AniListMediaListEntryDto;
import reccommendation_engine.RecEngine.domain.entities.InteractionSource;
import reccommendation_engine.RecEngine.domain.entities.Item;
import reccommendation_engine.RecEngine.domain.entities.User;
import reccommendation_engine.RecEngine.domain.entities.UserItemInteraction;
import reccommendation_engine.RecEngine.mappers.AniListEnumMapper;
import reccommendation_engine.RecEngine.mappers.AniListInteractionMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class AniListInteractionMapperImpl implements AniListInteractionMapper {

    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final AniListEnumMapper aniListEnumMapper;

    public AniListInteractionMapperImpl(
            ModelMapper modelMapper,
            ObjectMapper objectMapper,
            AniListEnumMapper aniListEnumMapper
    ) {
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.aniListEnumMapper = aniListEnumMapper;
    }

    @Override
    public UserItemInteraction fromAniListEntry(
            AniListMediaListEntryDto aniListEntry,
            User user,
            Item item,
            OffsetDateTime fallbackTimestamp
    ) {
        UserItemInteraction interaction = modelMapper.map(aniListEntry, UserItemInteraction.class);
        interaction.setId(null);
        interaction.setUser(user);
        interaction.setItem(item);
        interaction.setStatus(aniListEnumMapper.toInteractionStatus(aniListEntry.getStatus()));
        interaction.setProgress(toBigDecimal(aniListEntry.getProgress()));
        interaction.setRating(toBigDecimal(aniListEntry.getScore()));
        interaction.setInteractionTimestamp(resolveTimestamp(aniListEntry.getUpdatedAt(), fallbackTimestamp));
        interaction.setSource(InteractionSource.ANILIST_IMPORT);
        interaction.setSourceEventId(resolveSourceEventId(user, item, aniListEntry));
        interaction.setSourcePayload(writeJson(aniListEntry));
        return interaction;
    }

    private BigDecimal toBigDecimal(Number number) {
        if (number == null) {
            return null;
        }
        return BigDecimal.valueOf(number.doubleValue());
    }

    private OffsetDateTime resolveTimestamp(Long updatedAtEpochSeconds, OffsetDateTime fallbackTimestamp) {
        if (updatedAtEpochSeconds != null && updatedAtEpochSeconds > 0) {
            return OffsetDateTime.ofInstant(Instant.ofEpochSecond(updatedAtEpochSeconds), ZoneOffset.UTC);
        }
        return fallbackTimestamp;
    }

    private String resolveSourceEventId(User user, Item item, AniListMediaListEntryDto aniListEntry) {
        if (aniListEntry.getId() != null) {
            return "anilist-entry:" + aniListEntry.getId();
        }

        String userKey = user.getAnilistUsername() != null ? user.getAnilistUsername() : String.valueOf(user.getId());
        String itemKey = item.getExternalId() != null ? item.getExternalId() : String.valueOf(item.getId());
        String statusKey = aniListEntry.getStatus() != null ? aniListEntry.getStatus() : "unknown";
        Long updatedAt = aniListEntry.getUpdatedAt() != null ? aniListEntry.getUpdatedAt() : 0L;
        return "anilist-fallback:%s:%s:%s:%d".formatted(userKey, itemKey, statusKey, updatedAt);
    }

    private String writeJson(AniListMediaListEntryDto aniListEntry) {
        try {
            return objectMapper.writeValueAsString(aniListEntry);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize AniList interaction payload", ex);
        }
    }
}
