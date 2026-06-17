package com.overdrive.scenario.web

import com.overdrive.scenario.domain.OverrideType
import com.overdrive.scenario.domain.Scenario
import com.overdrive.scenario.domain.ScenarioOverrideEntity
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

// ── Request DTOs ─────────────────────────────────────────────────────────────

data class ScenarioCreateRequest(
    @field:NotBlank val name: String,
    val description: String? = null,
    @field:Valid val overrides: List<OverrideRequest> = emptyList(),
)

data class ScenarioUpdateRequest(
    @field:NotBlank val name: String,
    val description: String? = null,
    @field:Valid val overrides: List<OverrideRequest> = emptyList(),
)

data class OverrideRequest(
    @field:NotNull val overrideType: OverrideType,
    val entityId: UUID? = null,
    val params: Map<String, String> = emptyMap(),
)

data class ScenarioCompareRequest(
    @field:NotNull @field:Size(min = 1) val productIds: List<UUID>,
    @field:NotBlank val destinationZip: String,
    val serviceLevel: String = "STANDARD",
)

// ── Response DTOs ─────────────────────────────────────────────────────────────

data class ScenarioResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val version: Long,
    val overrides: List<OverrideResponse>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class OverrideResponse(
    val id: UUID,
    val overrideType: OverrideType,
    val position: Int,
    val entityId: UUID?,
    val params: Map<String, String>,
)

data class ScenarioDiffResponse(
    val scenarioId: UUID,
    val scenarioName: String,
    val staleOverrideEntityIds: List<UUID>,
    val items: List<ScenarioDiffItem>,
)

data class ScenarioDiffItem(
    val productId: UUID,
    val status: DiffStatus,
    val uncostableReason: String?,
    val overConstrainedReason: String?,
    val baselineDeliveredCost: BigDecimal?,
    val scenarioDeliveredCost: BigDecimal?,
    val deliveredCostDelta: Double?,
    val baselineWinnerRoute: RouteSummary?,
    val scenarioWinnerRoute: RouteSummary?,
    val baselineSavingsPct: Double?,
    val scenarioSavingsPct: Double?,
    val savingsPctDelta: Double?,
    val baselineOpportunityScore: Double?,
    val scenarioOpportunityScore: Double?,
    val opportunityScoreDelta: Double?,
)

/** Route winner summary. warehouseId/carrierId are nullable: Route domain carries names not UUIDs. */
data class RouteSummary(
    val fulfillmentModel: String,
    val warehouseId: UUID?,
    val warehouseName: String,
    val carrierId: UUID?,
    val carrierName: String,
    val totalDeliveredCost: BigDecimal,
    val transitDays: Int,
)

enum class DiffStatus { CHANGED, UNCOSTABLE, OVER_CONSTRAINED }

// ── Mappers ───────────────────────────────────────────────────────────────────

fun Scenario.toResponse() = ScenarioResponse(
    id = id,
    name = name,
    description = description,
    version = version,
    overrides = overrides.map { it.toResponse() },
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ScenarioOverrideEntity.toResponse() = OverrideResponse(
    id = id,
    overrideType = overrideType,
    position = position,
    entityId = entityId,
    params = params,
)
