package io.ipgeolocation.databaseReader.conf

import io.ipgeolocation.databaseReader.IpgeolocationDatabaseReaderApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
@ComponentScan(basePackages = "io.ipgeolocation.databaseReader", basePackageClasses = [IpgeolocationDatabaseReaderApplication.class])
class ThreadPoolTaskSchedulerConfig {

    @Bean
    ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler()

        threadPoolTaskScheduler.setPoolSize(5)
        threadPoolTaskScheduler.setThreadNamePrefix("ThreadPoolTaskScheduler")
        threadPoolTaskScheduler
    }
}
