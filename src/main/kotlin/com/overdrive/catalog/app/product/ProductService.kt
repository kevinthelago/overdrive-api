package com.overdrive.catalog.app.product

import com.overdrive.catalog.domain.product.Product
import com.overdrive.catalog.domain.product.ProductRepository
import com.overdrive.catalog.web.product.ProductCreateRequest
import com.overdrive.catalog.web.product.ProductUpdateRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class ProductService(private val products: ProductRepository) {

    @Transactional(readOnly = true)
    fun list(category: String?, search: String?, pageable: Pageable): Page<Product> =
        products.search(category, search, pageable)

    @Transactional(readOnly = true)
    fun get(id: UUID): Product = products.findById(id)
        .orElseThrow { NoSuchElementException("Product $id not found") }

    fun create(req: ProductCreateRequest): Product {
        if (products.existsBySku(req.sku)) {
            throw IllegalArgumentException("SKU '${req.sku}' already exists")
        }
        return products.save(
            Product(
                sku                  = req.sku,
                name                 = req.name,
                category             = req.category,
                weight               = req.weight,
                dimensions           = req.dimensions,
                hazardous            = req.hazardous,
                fragile              = req.fragile,
                temperatureSensitive = req.temperatureSensitive,
                stackable            = req.stackable,
                palletQty            = req.palletQty,
                cost                 = req.cost,
                msrp                 = req.msrp,
                marketSize           = req.marketSize,
                orderFrequency       = req.orderFrequency,
                categoryGrowth       = req.categoryGrowth,
                logisticsComplexity  = req.logisticsComplexity
            )
        )
    }

    fun update(id: UUID, req: ProductUpdateRequest): Product {
        val product = get(id)
        if (req.sku != product.sku && products.existsBySkuAndIdNot(req.sku, id)) {
            throw IllegalArgumentException("SKU '${req.sku}' already exists")
        }
        product.sku                  = req.sku
        product.name                 = req.name
        product.category             = req.category
        product.weight               = req.weight
        product.dimensions           = req.dimensions
        product.hazardous            = req.hazardous
        product.fragile              = req.fragile
        product.temperatureSensitive = req.temperatureSensitive
        product.stackable            = req.stackable
        product.palletQty            = req.palletQty
        product.cost                 = req.cost
        product.msrp                 = req.msrp
        product.marketSize           = req.marketSize
        product.orderFrequency       = req.orderFrequency
        product.categoryGrowth       = req.categoryGrowth
        product.logisticsComplexity  = req.logisticsComplexity
        product.updatedAt            = Instant.now()
        return products.save(product)
    }

    /** Deletes the product. Callers are responsible for referential-integrity checks. */
    fun delete(id: UUID) {
        val product = get(id)
        products.delete(product)
    }
}
