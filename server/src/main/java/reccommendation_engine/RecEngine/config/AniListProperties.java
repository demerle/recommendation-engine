package reccommendation_engine.RecEngine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "anilist")
public class AniListProperties {

    private String baseUrl;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);
    private String userAgent = "recommendation-engine";
}
