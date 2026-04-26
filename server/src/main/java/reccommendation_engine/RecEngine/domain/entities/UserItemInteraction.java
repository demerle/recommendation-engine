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
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_item_interactions")
public class UserItemInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_item_interactions_seq_gen")
    @SequenceGenerator(
            name = "user_item_interactions_seq_gen",
            sequenceName = "user_item_interactions_seq",
            allocationSize = 50
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InteractionStatus status;

    @Column(precision = 7, scale = 2)
    private BigDecimal progress;

    @Column(precision = 4, scale = 2)
    private BigDecimal rating;

    @Column(name = "interaction_timestamp", nullable = false)
    private OffsetDateTime interactionTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InteractionSource source;

    @Column(name = "source_event_id", length = 128)
    private String sourceEventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_payload", nullable = false, columnDefinition = "jsonb")
    private String sourcePayload;

    @Column(name = "inserted_at", nullable = false, updatable = false)
    private OffsetDateTime insertedAt;

    @PrePersist
    void prePersist() {
        if (sourcePayload == null) {
            sourcePayload = "{}";
        }
        if (insertedAt == null) {
            insertedAt = OffsetDateTime.now();
        }
    }
}
