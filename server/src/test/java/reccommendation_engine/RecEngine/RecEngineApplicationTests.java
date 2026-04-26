package reccommendation_engine.RecEngine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.mockito.Mockito;
import reccommendation_engine.RecEngine.mappers.ImportRunMapper;
import reccommendation_engine.RecEngine.repositories.UserRepository;
import reccommendation_engine.RecEngine.repositories.IngestionErrorRepository;
import reccommendation_engine.RecEngine.repositories.IngestionRunRepository;
import reccommendation_engine.RecEngine.repositories.ItemMetadataRepository;
import reccommendation_engine.RecEngine.repositories.ItemRepository;
import reccommendation_engine.RecEngine.repositories.UserItemInteractionRepository;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
@Import(RecEngineApplicationTests.TestBeans.class)
class RecEngineApplicationTests {

	@Test
	void contextLoads() {
	}

	@TestConfiguration
	static class TestBeans {

		@Bean
		IngestionRunRepository ingestionRunRepository() {
			return Mockito.mock(IngestionRunRepository.class);
		}

		@Bean
		IngestionErrorRepository ingestionErrorRepository() {
			return Mockito.mock(IngestionErrorRepository.class);
		}

		@Bean
		ItemRepository itemRepository() {
			return Mockito.mock(ItemRepository.class);
		}

		@Bean
		ItemMetadataRepository itemMetadataRepository() {
			return Mockito.mock(ItemMetadataRepository.class);
		}

		@Bean
		UserItemInteractionRepository userItemInteractionRepository() {
			return Mockito.mock(UserItemInteractionRepository.class);
		}

		@Bean
		UserRepository userRepository() {
			return Mockito.mock(UserRepository.class);
		}

		@Bean
		ImportRunMapper importRunMapper() {
			return Mockito.mock(ImportRunMapper.class);
		}
	}

}
