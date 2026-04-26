package reccommendation_engine.RecEngine.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "ingestion_errors")
public class IngestionError {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ingestion_errors_seq_gen")
    @SequenceGenerator(name = "ingestion_errors_seq_gen", sequenceName = "ingestion_errors_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingestion_run_id", nullable = false)
    private IngestionRun ingestionRun;

    @Column(name = "error_code", nullable = false, length = 64)
    private String errorCode;

    @Column(name = "error_message", nullable = false, columnDefinition = "text")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (payloadJson == null) {
            payloadJson = "{}";
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
