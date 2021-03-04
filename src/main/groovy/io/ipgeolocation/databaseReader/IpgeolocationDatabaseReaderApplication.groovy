package io.ipgeolocation.databaseReader

import io.ipgeolocation.databaseReader.services.database.DatabaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext

import javax.annotation.PostConstruct

@SpringBootApplication
class IpgeolocationDatabaseReaderApplication {
	private static ConfigurableApplicationContext context

	private final DatabaseService databaseService

	IpgeolocationDatabaseReaderApplication(@Autowired DatabaseService databaseService) {
		this.databaseService = databaseService
	}

	@PostConstruct
	void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

		databaseService.loadDatabases()
	}

	static void main(String[] args) {
		context = SpringApplication.run(IpgeolocationDatabaseReaderApplication, args)
	}

	static void restart() {
		ApplicationArguments args = context.getBean(ApplicationArguments.class)

		Thread thread = new Thread( {
			context.close()
			context = SpringApplication.run(IpgeolocationDatabaseReaderApplication.class, args.getSourceArgs())
		})

		thread.daemon = false
		thread.start()
	}
}
