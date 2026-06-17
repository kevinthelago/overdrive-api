package com.overdrive.catalog.web

import com.overdrive.catalog.domain.warehouse.Warehouse
import com.overdrive.catalog.domain.warehouse.WarehouseRepository
import com.overdrive.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@AutoConfigureMockMvc
@Transactional
class WarehouseControllerIT @Autowired constructor(
    val mvc: MockMvc,
    val repo: WarehouseRepository
) : AbstractIntegrationTest() {

    private fun warehouseJson(name: String = "Test DC") = """
        {
          "name": "$name",
          "type": "DC",
          "state": "IL",
          "zip": "60601",
          "lat": 41.8827,
          "lng": -87.6233,
          "palletCapacity": 5000,
          "pickFee":             { "amount": 1.25, "currency": "USD" },
          "receivingFee":        { "amount": 18.00, "currency": "USD" },
          "storageFeePerPallet": { "amount": 22.00, "currency": "USD" }
        }
    """.trimIndent()

    @Test
    fun `POST create returns 201`() {
        mvc.post("/api/catalog/warehouse") {
            contentType = MediaType.APPLICATION_JSON
            content = warehouseJson()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name") { value("Test DC") }
            jsonPath("$.type") { value("DC") }
            jsonPath("$.pickFee.amount") { exists() }
        }
    }

    @Test
    fun `GET list filters by type and state`() {
        repo.save(minimalWarehouse("WH-A", "DC", "IL"))
        repo.save(minimalWarehouse("WH-B", "FC", "GA"))

        mvc.get("/api/catalog/warehouse?type=DC&state=IL").andExpect {
            status { isOk() }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content[0].name") { value("WH-A") }
        }
    }

    @Test
    fun `GET by id returns 404 when missing`() {
        mvc.get("/api/catalog/warehouse/00000000-0000-0000-0000-000000000099").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `DELETE returns 204`() {
        val saved = repo.save(minimalWarehouse("WH-DEL", "DC", "TX"))
        mvc.delete("/api/catalog/warehouse/${saved.id}").andExpect {
            status { isNoContent() }
        }
    }

    private fun minimalWarehouse(name: String, type: String, state: String) = Warehouse(
        name                        = name,
        type                        = type,
        state                       = state,
        zip                         = "00000",
        lat                         = 0.0,
        lng                         = 0.0,
        palletCapacity              = 1000,
        pickFeeAmount               = BigDecimal.ZERO,
        receivingFeeAmount          = BigDecimal.ZERO,
        storageFeePerPalletAmount   = BigDecimal.ZERO
    )
}
