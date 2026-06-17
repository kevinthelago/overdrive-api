package com.overdrive.scenario

import com.fasterxml.jackson.databind.ObjectMapper
import com.overdrive.scenario.domain.OverrideType
import com.overdrive.scenario.domain.ScenarioRepository
import com.overdrive.scenario.web.OverrideRequest
import com.overdrive.scenario.web.ScenarioCreateRequest
import com.overdrive.scenario.web.ScenarioUpdateRequest
import com.overdrive.support.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.util.UUID

@AutoConfigureMockMvc
class ScenarioCrudTest : AbstractIntegrationTest() {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper
    @Autowired lateinit var scenarioRepository: ScenarioRepository

    @BeforeEach
    fun clean() = scenarioRepository.deleteAll()

    @Test
    fun `create and retrieve a scenario`() {
        val warehouseId = UUID.randomUUID()
        val request = ScenarioCreateRequest(
            name = "No East-Coast Warehouses",
            description = "Remove all EC WH to test West-Coast routing",
            overrides = listOf(
                OverrideRequest(OverrideType.REMOVE_WAREHOUSE, entityId = warehouseId),
            ),
        )

        val body = mvc.post("/api/scenarios") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andExpect { status { isCreated() } }
            .andReturn().response.contentAsString

        val created = mapper.readTree(body)
        val id = created["id"].asText()

        mvc.get("/api/scenarios/$id")
            .andExpect {
                status { isOk() }
                jsonPath("$.name") { value("No East-Coast Warehouses") }
                jsonPath("$.overrides[0].overrideType") { value("REMOVE_WAREHOUSE") }
                jsonPath("$.overrides[0].entityId") { value(warehouseId.toString()) }
            }
    }

    @Test
    fun `list returns all scenarios`() {
        scenarioRepository.save(com.overdrive.scenario.domain.Scenario(name = "A"))
        scenarioRepository.save(com.overdrive.scenario.domain.Scenario(name = "B"))

        mvc.get("/api/scenarios")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
            }
    }

    @Test
    fun `update replaces overrides atomically`() {
        val request = ScenarioCreateRequest(name = "Draft", overrides = emptyList())
        val body = mvc.post("/api/scenarios") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andReturn().response.contentAsString
        val id = mapper.readTree(body)["id"].asText()

        val carrierId = UUID.randomUUID()
        val update = ScenarioUpdateRequest(
            name = "Updated",
            overrides = listOf(OverrideRequest(OverrideType.ADD_CARRIER, entityId = carrierId)),
        )

        mvc.put("/api/scenarios/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(update)
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Updated") }
            jsonPath("$.overrides.length()") { value(1) }
            jsonPath("$.overrides[0].overrideType") { value("ADD_CARRIER") }
        }
    }

    @Test
    fun `delete removes the scenario`() {
        val request = ScenarioCreateRequest(name = "ToDelete")
        val body = mvc.post("/api/scenarios") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andReturn().response.contentAsString
        val id = mapper.readTree(body)["id"].asText()

        mvc.delete("/api/scenarios/$id").andExpect { status { isNoContent() } }
        mvc.get("/api/scenarios/$id").andExpect { status { isNotFound() } }
        assertThat(scenarioRepository.count()).isEqualTo(0)
    }

    @Test
    fun `duplicate name returns 409`() {
        mvc.post("/api/scenarios") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(ScenarioCreateRequest(name = "Dup"))
        }.andExpect { status { isCreated() } }

        mvc.post("/api/scenarios") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(ScenarioCreateRequest(name = "Dup"))
        }.andExpect { status { isConflict() } }
    }

    @Test
    fun `blank name returns 400`() {
        mvc.post("/api/scenarios") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(ScenarioCreateRequest(name = "  "))
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `overrides are persisted in position order`() {
        val wh1 = UUID.randomUUID()
        val wh2 = UUID.randomUUID()
        val request = ScenarioCreateRequest(
            name = "Ordered",
            overrides = listOf(
                OverrideRequest(OverrideType.ADD_WAREHOUSE, entityId = wh1),
                OverrideRequest(OverrideType.REMOVE_WAREHOUSE, entityId = wh2),
            ),
        )

        val body = mvc.post("/api/scenarios") {
            contentType = MediaType.APPLICATION_JSON
            content = mapper.writeValueAsString(request)
        }.andReturn().response.contentAsString

        val overrides = mapper.readTree(body)["overrides"]
        assertThat(overrides[0]["position"].asInt()).isLessThan(overrides[1]["position"].asInt())
        assertThat(overrides[0]["overrideType"].asText()).isEqualTo("ADD_WAREHOUSE")
        assertThat(overrides[1]["overrideType"].asText()).isEqualTo("REMOVE_WAREHOUSE")
    }
}
