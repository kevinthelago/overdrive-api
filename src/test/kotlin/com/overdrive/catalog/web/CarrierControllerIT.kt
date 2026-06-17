package com.overdrive.catalog.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.overdrive.catalog.domain.carrier.Carrier
import com.overdrive.catalog.domain.carrier.CarrierRepository
import com.overdrive.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@AutoConfigureMockMvc
@Transactional
class CarrierControllerIT @Autowired constructor(
    val mvc: MockMvc,
    val mapper: ObjectMapper,
    val repo: CarrierRepository
) : AbstractIntegrationTest() {

    private fun carrierJson(name: String = "Test Carrier", model: String = "PARCEL") = """
        {
          "name": "$name",
          "scac": "TCAR",
          "pricingModel": "$model",
          "liftgateSurcharge":    { "amount": 0.00, "currency": "USD" },
          "residentialSurcharge": { "amount": 4.90, "currency": "USD" },
          "dimFactor": 139.0,
          "fuelSurchargePct": 0.165
        }
    """.trimIndent()

    @Test
    fun `POST create parcel carrier returns 201`() {
        mvc.post("/api/catalog/carrier") {
            contentType = MediaType.APPLICATION_JSON
            content = carrierJson()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.pricingModel") { value("PARCEL") }
            jsonPath("$.residentialSurcharge.amount") { value(4.90) }
        }
    }

    @Test
    fun `GET list filters by pricingModel`() {
        repo.save(minimalCarrier("CAR-LTL", "LTL"))
        repo.save(minimalCarrier("CAR-FTL", "FTL"))

        mvc.get("/api/catalog/carrier?pricingModel=LTL").andExpect {
            status { isOk() }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content[0].pricingModel") { value("LTL") }
        }
    }

    @Test
    fun `POST lane then GET lanes returns lane`() {
        val createResult = mvc.post("/api/catalog/carrier") {
            contentType = MediaType.APPLICATION_JSON
            content = carrierJson("Lane Test Carrier")
        }.andExpect { status { isCreated() } }
            .andReturn()

        val carrierId = mapper.readTree(createResult.response.contentAsString).get("id").asText()

        mvc.post("/api/catalog/carrier/$carrierId/lanes") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "serviceLevel": "GROUND",
                  "originZone": "1",
                  "destZone": "2",
                  "transitDays": 2,
                  "baseRate": { "amount": 8.50, "currency": "USD" },
                  "perLbRate": 0.15
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.serviceLevel") { value("GROUND") }
            jsonPath("$.carrierId") { value(carrierId) }
        }

        mvc.get("/api/catalog/carrier/$carrierId/lanes").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
        }
    }

    private fun minimalCarrier(name: String, model: String) = Carrier(
        name                         = name,
        pricingModel                 = model,
        liftgateSurchargeAmount      = BigDecimal.ZERO,
        residentialSurchargeAmount   = BigDecimal.ZERO
    )
}
