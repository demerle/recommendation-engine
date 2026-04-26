package reccommendation_engine.RecEngine.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "item_metadata")
public class ItemMetadata implements Persistable<Long> {

    @Id
    @Column(name = "item_id")
    private Long itemId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false, insertable = false, updatable = false)
    private Item item;

    @Column(name = "genres", nullable = false, columnDefinition = "text[]")
    private String[] genres;

    @Column(name = "tags", nullable = false, columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "studios", nullable = false, columnDefinition = "text[]")
    private String[] studios;

    @Column(name = "authors", nullable = false, columnDefinition = "text[]")
    private String[] authors;

    @Column(length = 32)
    private String format;

    @Column(length = 32)
    private String status;

    @Column(name = "episodes_or_chapters")
    private Integer episodesOrChapters;

    @Column(name = "year_start")
    private Integer yearStart;

    @Column(columnDefinition = "text")
    private String synopsis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", nullable = false, columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "metadata_version", nullable = false, length = 64)
    private String metadataVersion;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @PrePersist
    void prePersist() {
        if (genres == null) {
            genres = new String[0];
        }
        if (tags == null) {
            tags = new String[0];
        }
        if (studios == null) {
            studios = new String[0];
        }
        if (authors == null) {
            authors = new String[0];
        }
        if (metadataJson == null) {
            metadataJson = "{}";
        }
        if (metadataVersion == null) {
            metadataVersion = "v1";
        }
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        isNew = false;
    }

    public Long getId() {
        return itemId;
    }

    public boolean isNew() {
        return isNew;
    }
}
