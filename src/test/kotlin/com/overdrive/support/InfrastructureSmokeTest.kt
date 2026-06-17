package com.overdrive.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import javax.sql.DataSource

class InfrastructureSmokeTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var stringRedisTemplate: StringRedisTemplate

    @Test
    fun `datasource is reachable`() {
        dataSource.connection.use { conn ->
            assertTrue(conn.isValid(5), "DataSource connection must be valid within 5 seconds")
        }
    }

    @Test
    fun `redis can round-trip a string value`() {
        val key = "smoke:ping"
        stringRedisTemplate.opsForValue().set(key, "pong")
        val value = stringRedisTemplate.opsForValue().get(key)
        assertEquals("pong", value, "Redis round-trip must return the stored value")
        stringRedisTemplate.delete(key)
    }
}
