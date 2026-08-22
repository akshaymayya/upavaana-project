package com.plantdoctor;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootTest(properties = {
		"spring.flyway.enabled=false",
		"spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
		"spring.jpa.hibernate.ddl-auto=none"
})
class PlantDoctorApiApplicationTests {

	@TestConfiguration
	static class TestConfig {
		@Bean
		public DataSource dataSource() {
			return Mockito.mock(DataSource.class);
		}
	}

	@Test
	void contextLoads() {
	}

}
