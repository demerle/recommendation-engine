package reccommendation_engine.RecEngine.services;

public class AniListUserNotFoundException extends AniListClientException {

    public AniListUserNotFoundException(String username) {
        super("AniList user not found: " + username);
    }
}
