package com.overdrive.catalog.app.product

import com.overdrive.catalog.domain.product.Product
import com.overdrive.catalog.domain.product.ProductRepository
import com.overdrive.catalog.web.product.ProductCreateRequest
import com.overdrive.catalog.web.product.ProductUpdateRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
                weightLbs            = req.weight.pounds,
                lengthIn             = req.dimensions.lengthIn,
                widthIn              = req.dimensions.widthIn,
                heightIn             = req.dimensions.heightIn,
                hazardous            = req.hazardous,
                fragile              = req.fragile,
                temperatureSensitive = req.temperatureSensitive,
                stackable            = req.stackable,
                palletQty            = req.palletQty,
                costAmount           = req.cost.amount,
                costCurrency         = req.cost.currency,
                msrpAmount           = req.msrp.amount,
                msrpCurrency         = req.msrp.currency,
                marketSize           = req.marketSize,
                orderFrequency       = req.orderFrequency,
                categoryGrowth       = req.categoryGrowth,
                logisticsComplexity  = req.logisticsComplexity
            )
        )
    }

    fun update(id: UUID, req: ProductUpdateRequest): Product {
        val p = get(id)
        if (req.sku != p.sku && products.existsBySkuAndIdNot(req.sku, id)) {
            throw IllegalArgumentException("SKU '${req.sku}' already exists")
        }
        p.sku                  = req.sku
        p.name                 = req.name
        p.category             = req.category
        p.weightLbs            = req.weight.pounds
        p.lengthIn             = req.dimensions.lengthIn
        p.widthIn              = req.dimensions.widthIn
        p.heightIn             = req.dimensions.heightIn
        p.hazardous            = req.hazardous
        p.fragile              = req.fragile
        p.temperatureSensitive = req.temperatureSensitive
        p.stackable            = req.stackable
        p.palletQty            = req.palletQty
        p.costAmount           = req.cost.amount
        p.costCurrency         = req.cost.currency
        p.msrpAmount           = req.msrp.amount
        p.msrpCurrency         = req.msrp.currency
        p.marketSize           = req.marketSize
        p.orderFrequency       = req.orderFrequency
        p.categoryGrowth       = req.categoryGrowth
        p.logisticsComplexity  = req.logisticsComplexity
        p.updatedAt            = Instant.now()
        return products.save(p)
    }

    fun delete(id: UUID) = products.delete(get(id))
}
