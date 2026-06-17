package com.overdrive.cost.service

import com.overdrive.common.money.Money
import com.overdrive.cost.domain.CostCalculationContext
import com.overdrive.cost.domain.DeliveredCostResult
import com.overdrive.cost.domain.DutyCost
import com.overdrive.cost.domain.FeeComponent
import com.overdrive.cost.domain.FreightCost
import com.overdrive.cost.domain.TransportMode
import com.overdrive.explainability.domain.EngineType
import com.overdrive.explainability.domain.TraceStep
import com.overdrive.explainability.service.ExplainabilityService
import com.overdrive.scenario.context.service.ScenarioContextResolver
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

@Service
class CostCalculationService(
    private val explainabilityService: ExplainabilityService,
    private val scenarioContextResolver: ScenarioContextResolver,
) {
    @Cacheable(value = ["delivered-cost"], key = "#context.opportunityId + ':' + #context.scenarioContextId")
    fun calculate(context: CostCalculationContext): DeliveredCostResult {
        val overrides = buildOverrides(context)
        val steps = mutableListOf<TraceStep>()

        val freight = computeFreight(context, overrides, steps)
        val duty = computeDuty(context, overrides, steps)
        val fees = computeFees(context, overrides, steps)

        val total = (listOf(freight.amount, duty.amount) + fees.map { it.amount })
            .reduce(Money::plus)

        val trace = explainabilityService.record(
            engineType = EngineType.COST,
            opportunityId = context.opportunityId,
            scenarioContextId = context.scenarioContextId,
            inputs = context,
            outputs = mapOf("totalDeliveredCost" to total, "freightCost" to freight, "dutyCost" to duty),
            steps = steps,
        )

        return DeliveredCostResult(
            id = UUID.randomUUID(),
            opportunityId = context.opportunityId,
            scenarioContextId = context.scenarioContextId,
            freightCost = freight,
            dutyCost = duty,
            fees = fees,
            totalDeliveredCost = total,
            traceId = trace.id,
            calculatedAt = Instant.now(),
        )
    }

    private fun buildOverrides(context: CostCalculationContext): Map<String, BigDecimal> {
        val scenarioOverrides = context.scenarioContextId
            ?.let { scenarioContextResolver.resolve(it) }
            ?.assumptions
            ?.run { freightRateOverrides + dutyRateOverrides + feeOverrides }
            .orEmpty()
        return scenarioOverrides + context.scenarioOverrides
    }

    private fun computeFreight(
        ctx: CostCalculationContext,
        overrides: Map<String, BigDecimal>,
        steps: MutableList<TraceStep>,
    ): FreightCost {
        val ratePerLb = overrides["freight.rate.${ctx.carrier}"]
            ?: DEFAULT_FREIGHT_RATES[ctx.transportMode]
            ?: BigDecimal("1.80")
        val amount = Money.of(ctx.weight.pounds.multiply(ratePerLb).setScale(2, RoundingMode.HALF_UP))
        steps += TraceStep(
            step = 1,
            description = "Freight cost",
            inputs = mapOf("carrier" to ctx.carrier, "mode" to ctx.transportMode, "pounds" to ctx.weight.pounds, "ratePerLb" to ratePerLb),
            outputs = mapOf("freightCost" to amount.amount),
            rule = "freightCost = pounds × ratePerLb",
        )
        return FreightCost(ctx.carrier, ctx.transportMode, amount)
    }

    private fun computeDuty(
        ctx: CostCalculationContext,
        overrides: Map<String, BigDecimal>,
        steps: MutableList<TraceStep>,
    ): DutyCost {
        val dutyRate = overrides["duty.rate.${ctx.productHsCode}"]
            ?: DUTY_RATES[ctx.destinationCountry]
            ?: BigDecimal("0.05")
        val amount = ctx.declaredCargoValue * dutyRate
        steps += TraceStep(
            step = 2,
            description = "Import duty",
            inputs = mapOf("hsCode" to ctx.productHsCode, "dutyRate" to dutyRate, "cargoValue" to ctx.declaredCargoValue.amount),
            outputs = mapOf("dutyCost" to amount.amount),
            rule = "dutyCost = cargoValue × dutyRate",
        )
        return DutyCost(ctx.productHsCode, dutyRate, amount)
    }

    private fun computeFees(
        ctx: CostCalculationContext,
        overrides: Map<String, BigDecimal>,
        steps: MutableList<TraceStep>,
    ): List<FeeComponent> {
        val handlingRate = overrides["fee.handling"] ?: BigDecimal("0.02")
        val handling = FeeComponent("HANDLING", "Handling and documentation", ctx.declaredCargoValue * handlingRate)
        steps += TraceStep(
            step = 3,
            description = "Handling fees",
            inputs = mapOf("cargoValue" to ctx.declaredCargoValue.amount, "handlingRate" to handlingRate),
            outputs = mapOf("handlingFee" to handling.amount.amount),
            rule = "handlingFee = cargoValue × handlingRate",
        )
        return listOf(handling)
    }

    companion object {
        private val DEFAULT_FREIGHT_RATES = mapOf(
            TransportMode.TRUCK to BigDecimal("1.80"),
            TransportMode.RAIL to BigDecimal("1.20"),
            TransportMode.OCEAN to BigDecimal("0.60"),
            TransportMode.AIR to BigDecimal("5.50"),
        )
        private val DUTY_RATES = mapOf(
            "US" to BigDecimal("0.035"),
            "CA" to BigDecimal("0.06"),
            "MX" to BigDecimal("0.08"),
            "DE" to BigDecimal("0.065"),
            "GB" to BigDecimal("0.04"),
        )
    }
}
