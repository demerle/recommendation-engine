package reccommendation_engine.RecEngine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reccommendation_engine.RecEngine.domain.entities.InteractionSource;
import reccommendation_engine.RecEngine.domain.entities.InteractionStatus;
import reccommendation_engine.RecEngine.domain.entities.UserItemInteraction;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserItemInteractionRepository extends JpaRepository<UserItemInteraction, Long> {

    List<UserItemInteraction> findByUserIdOrderByInteractionTimestampDesc(Long userId);

    List<UserItemInteraction> findByItemIdOrderByInteractionTimestampDesc(Long itemId);

    Optional<UserItemInteraction> findBySourceAndSourceEventId(InteractionSource source, String sourceEventId);

    Optional<UserItemInteraction> findByUserIdAndItemIdAndStatusAndInteractionTimestampAndSource(
            Long userId,
            Long itemId,
            InteractionStatus status,
            OffsetDateTime interactionTimestamp,
            InteractionSource source
    );
}
