package reccommendation_engine.RecEngine.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(AniListProperties.class)
public class AniListClientConfig {

    @Bean
    public RestClient aniListRestClient(AniListProperties aniListProperties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(aniListProperties.getConnectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(aniListProperties.getReadTimeout());

        return RestClient.builder()
                .baseUrl(aniListProperties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeader(HttpHeaders.USER_AGENT, aniListProperties.getUserAgent())
                .build();
    }
}
