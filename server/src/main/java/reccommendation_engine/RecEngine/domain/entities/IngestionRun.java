package reccommendation_engine.RecEngine.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "ingestion_runs")
public class IngestionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ingestion_runs_seq_gen")
    @SequenceGenerator(name = "ingestion_runs_seq_gen", sequenceName = "ingestion_runs_seq", allocationSize = 50)
    private Long id;

    @Column(name = "run_id", nullable = false, unique = true)
    private UUID runId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IngestionSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IngestionRunStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary_json", nullable = false, columnDefinition = "jsonb")
    private String summaryJson;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount;

    @Builder.Default
    @OneToMany(mappedBy = "ingestionRun", fetch = FetchType.LAZY)
    private Set<IngestionError> errors = new HashSet<>();

    @PrePersist
    void prePersist() {
        if (runId == null) {
            runId = UUID.randomUUID();
        }
        if (source == null) {
            source = IngestionSource.ANILIST;
        }
        if (status == null) {
            status = IngestionRunStatus.QUEUED;
        }
        if (startedAt == null) {
            startedAt = OffsetDateTime.now();
        }
        if (summaryJson == null) {
            summaryJson = "{}";
        }
        if (errorCount == null) {
            errorCount = 0;
        }
    }
}
