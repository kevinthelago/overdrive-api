package com.overdrive.scenario.context.adapter

import com.overdrive.scenario.context.domain.ScenarioContext
import com.overdrive.scenario.context.port.ScenarioContextPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.UUID

/**
 * Provides a no-op ScenarioContextPort when no other implementation is present on the classpath.
 * The scenario-analytics stream replaces this by contributing its own ScenarioContextPort bean,
 * at which point this configuration backs off via @ConditionalOnMissingBean.
 */
@Configuration
class NoOpScenarioContextAdapter {

    @Bean
    @ConditionalOnMissingBean(ScenarioContextPort::class)
    fun noOpScenarioContextPort(): ScenarioContextPort = object : ScenarioContextPort {
        override fun findById(scenarioId: UUID): ScenarioContext? = null
    }
}
