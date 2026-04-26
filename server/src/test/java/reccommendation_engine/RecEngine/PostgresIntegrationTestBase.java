package reccommendation_engine.RecEngine;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class PostgresIntegrationTestBase {

    private static EmbeddedPostgres embeddedPostgres;
    private static final String TEST_DATABASE = "postgres";
    private static final String TEST_USERNAME = "postgres";
    private static final String TEST_PASSWORD = "postgres";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        EmbeddedPostgres postgres = getEmbeddedPostgres();
        String jdbcUrl = postgres.getJdbcUrl(TEST_USERNAME, TEST_DATABASE);

        resetSchema(jdbcUrl);

        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> TEST_USERNAME);
        registry.add("spring.datasource.password", () -> TEST_PASSWORD);
        registry.add("spring.docker.compose.enabled", () -> "false");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add(
                "spring.autoconfigure.exclude",
                () -> "org.springframework.boot.neo4j.autoconfigure.Neo4jAutoConfiguration"
        );
    }

    private static synchronized EmbeddedPostgres getEmbeddedPostgres() {
        if (embeddedPostgres == null) {
            try {
                embeddedPostgres = EmbeddedPostgres.builder().start();
            } catch (java.io.IOException ex) {
                throw new IllegalStateException("Failed to start embedded PostgreSQL for integration tests", ex);
            }
        }
        return embeddedPostgres;
    }

    private static synchronized void resetSchema(String jdbcUrl) {
        Flyway flyway = Flyway.configure()
                .cleanDisabled(false)
                .dataSource(jdbcUrl, TEST_USERNAME, TEST_PASSWORD)
                .locations("classpath:db/migration")
                .load();

        flyway.clean();
        flyway.migrate();
    }
}
