package reccommendation_engine.RecEngine.services;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("User not found: " + userId);
    }
}
