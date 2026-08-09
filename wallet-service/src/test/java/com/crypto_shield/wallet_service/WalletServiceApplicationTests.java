package com.crypto_shield.wallet_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:wallet-service-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"eureka.client.enabled=false"
})
class WalletServiceApplicationTests {

	@Test
	void contextLoads_applicationContextWithH2_startsSuccessfully() {
	}

}
