package reccommendation_engine.RecEngine.services;

import reccommendation_engine.RecEngine.domain.entities.IngestionRun;
import reccommendation_engine.RecEngine.domain.entities.MediaType;
import reccommendation_engine.RecEngine.domain.entities.User;

public interface AniListImportService {

    IngestionRun importUserMediaList(User user, MediaType mediaType);
}
