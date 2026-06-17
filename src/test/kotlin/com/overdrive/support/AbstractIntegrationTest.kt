package com.overdrive.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Base class for integration tests. Starts Postgres 16 and Redis 7 exactly once per JVM.
 *
 * Containers are started in the companion object init{} block rather than via @Container so
 * the JUnit 5 Testcontainers extension does not stop them between subclasses. Stopping between
 * subclasses exhausts HikariCP connections and causes timeouts in downstream test classes.
 * Testcontainers' Ryuk watchdog handles cleanup at JVM exit.
 */
@SpringBootTest
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    companion object {

        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("overdrive")
            .withUsername("overdrive")
            .withPassword("overdrive")

        val redis: GenericContainer<*> = GenericContainer("redis:7-alpine")
            .withExposedPorts(6379)

        init {
            postgres.start()
            redis.start()
        }

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
        }
    }
}
