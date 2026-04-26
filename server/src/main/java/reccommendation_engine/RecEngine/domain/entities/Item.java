package reccommendation_engine.RecEngine.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "items_seq_gen")
    @SequenceGenerator(name = "items_seq_gen", sequenceName = "items_seq", allocationSize = 50)
    private Long id;

    @Column(name = "external_source", nullable = false, length = 32)
    private String externalSource;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 16)
    private MediaType mediaType;

    @Column(name = "canonical_title", nullable = false)
    private String canonicalTitle;

    @Column(name = "title_english")
    private String titleEnglish;

    @Column(name = "title_romaji")
    private String titleRomaji;

    @Column(name = "title_native")
    private String titleNative;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (externalSource == null) {
            externalSource = "anilist";
        }
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
