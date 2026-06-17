package com.overdrive.catalog.web

import com.overdrive.catalog.domain.product.Product
import com.overdrive.catalog.domain.product.ProductRepository
import com.overdrive.support.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@AutoConfigureMockMvc
@Transactional
class ProductControllerIT @Autowired constructor(
    val mvc: MockMvc,
    val productRepo: ProductRepository
) : AbstractIntegrationTest() {

    private fun productJson(sku: String = "TEST-001", name: String = "Test Widget") = """
        {
          "sku": "$sku",
          "name": "$name",
          "category": "ELECTRONICS",
          "weight": { "pounds": 1.5 },
          "dimensions": { "lengthIn": 10.0, "widthIn": 5.0, "heightIn": 3.0 },
          "hazardous": false,
          "fragile": false,
          "temperatureSensitive": false,
          "stackable": true,
          "palletQty": 48,
          "cost": { "amount": 12.00, "currency": "USD" },
          "msrp": { "amount": 39.99, "currency": "USD" }
        }
    """.trimIndent()

    @Test
    fun `POST create returns 201 with product`() {
        mvc.post("/api/catalog/product") {
            contentType = MediaType.APPLICATION_JSON
            content = productJson()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.sku") { value("TEST-001") }
            jsonPath("$.category") { value("ELECTRONICS") }
            jsonPath("$.id") { exists() }
            jsonPath("$.version") { value(0) }
            jsonPath("$.weight.pounds") { value(1.5) }
            jsonPath("$.cost.amount") { value(12.00) }
        }
    }

    @Test
    fun `GET list returns paginated products`() {
        productRepo.save(minimalProduct("SKU-A"))
        productRepo.save(minimalProduct("SKU-B"))

        mvc.get("/api/catalog/product?size=10").andExpect {
            status { isOk() }
            jsonPath("$.content.length()") { value(2) }
            jsonPath("$.totalElements") { value(2) }
        }
    }

    @Test
    fun `GET list filters by category`() {
        productRepo.save(minimalProduct("CAT-A", "ELECTRONICS"))
        productRepo.save(minimalProduct("CAT-B", "HARDWARE_TOOLS"))

        mvc.get("/api/catalog/product?category=ELECTRONICS").andExpect {
            status { isOk() }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content[0].category") { value("ELECTRONICS") }
        }
    }

    @Test
    fun `GET by id returns product`() {
        val saved = productRepo.save(minimalProduct("FIND-ME"))
        mvc.get("/api/catalog/product/${saved.id}").andExpect {
            status { isOk() }
            jsonPath("$.sku") { value("FIND-ME") }
        }
    }

    @Test
    fun `GET by id returns 404 when not found`() {
        mvc.get("/api/catalog/product/00000000-0000-0000-0000-000000000099").andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `PUT update returns updated product`() {
        val saved = productRepo.save(minimalProduct("UPDATE-ME"))
        mvc.put("/api/catalog/product/${saved.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = productJson("UPDATE-ME", "Updated Widget")
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Updated Widget") }
        }
    }

    @Test
    fun `DELETE returns 204`() {
        val saved = productRepo.save(minimalProduct("DELETE-ME"))
        mvc.delete("/api/catalog/product/${saved.id}").andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `POST with missing required fields returns 400`() {
        mvc.post("/api/catalog/product") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"sku":"","name":"","category":""}"""
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `POST duplicate SKU returns 400`() {
        productRepo.save(minimalProduct("DUP-SKU"))
        mvc.post("/api/catalog/product") {
            contentType = MediaType.APPLICATION_JSON
            content = productJson("DUP-SKU")
        }.andExpect { status { isBadRequest() } }
    }

    private fun minimalProduct(sku: String, category: String = "ELECTRONICS") = Product(
        sku        = sku,
        name       = "Test Product",
        category   = category,
        weightLbs  = BigDecimal("1.0"),
        lengthIn   = BigDecimal("10.0"),
        widthIn    = BigDecimal("5.0"),
        heightIn   = BigDecimal("3.0"),
        costAmount = BigDecimal("10.00"),
        msrpAmount = BigDecimal("30.00"),
        palletQty  = 48
    )
}
