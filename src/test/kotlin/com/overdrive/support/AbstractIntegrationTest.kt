package com.overdrive.support

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Base class for integration tests. Starts shared Postgres 16 and Redis 7 containers
 * once per JVM using the Testcontainers singleton pattern.
 *
 * Containers are started eagerly in the companion object init block rather than via
 * @Container so the JUnit Testcontainers extension never stops them between subclasses.
 * Without this, containers stopped after the first subclass and subsequent subclasses
 * got "Connection refused" on the same mapped port.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractIntegrationTest {

    companion object {

        @JvmField
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("overdrive")
            .withUsername("overdrive")
            .withPassword("overdrive")

        @JvmField
        val redis: GenericContainer<*> = GenericContainer("redis:7-alpine")
            .withExposedPorts(6379)

        init {
            if (DockerClientFactory.instance().isDockerAvailable) {
                postgres.start()
                redis.start()
            }
        }

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            if (postgres.isRunning) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl)
                registry.add("spring.datasource.username", postgres::getUsername)
                registry.add("spring.datasource.password", postgres::getPassword)
            }
            if (redis.isRunning) {
                registry.add("spring.data.redis.host", redis::getHost)
                registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
            }
        }
    }
}
