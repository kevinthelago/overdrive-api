package com.overdrive.catalog.domain.supplier

import com.overdrive.catalog.domain.product.Product
import jakarta.persistence.*
import java.io.Serializable
import java.util.UUID

@Embeddable
data class SupplierProductId(
    val supplierId: UUID,
    val productId: UUID
) : Serializable

@Entity
@Table(name = "supplier_product")
class SupplierProduct(

    @EmbeddedId
    val id: SupplierProductId,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("supplierId")
    @JoinColumn(name = "supplier_id")
    val supplier: Supplier,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    val product: Product,

    @Column(nullable = false) var isPrimary: Boolean = false
)
