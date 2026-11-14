package co.mipyme.database.internal.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import co.mipyme.contract.BusinessType
import co.mipyme.contract.Decimal
import co.mipyme.contract.Location
import java.time.LocalDateTime

@Entity(tableName = "locations")
internal data class LocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "business_type") val businessType: BusinessType,
    val address: String?,
    val phone: String?,
    @ColumnInfo(name = "tax_id") val taxId: String?,
    @ColumnInfo(name = "tax_rate") val taxRate: String?,
    @ColumnInfo(name = "next_sale_number") val nextSaleNumber: Int,
    @ColumnInfo(name = "next_purchase_order_number") val nextPurchaseOrderNumber: Int,
    @ColumnInfo(name = "allow_backorders") val allowBackorders: Boolean,
    @ColumnInfo(name = "receipt_header_text") val receiptHeaderText: String?,
    @ColumnInfo(name = "receipt_footer_text") val receiptFooterText: String?,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime,
    @ColumnInfo(name = "exchange_rate") val exchangeRate: String,
)

internal fun Location.toEntity(): LocationEntity {
    return LocationEntity(
        id = id,
        name = name,
        businessType = businessType,
        address = address,
        phone = phone,
        taxId = taxId,
        taxRate = taxRate?.value,
        nextSaleNumber = nextSaleNumber,
        nextPurchaseOrderNumber = nextPurchaseOrderNumber,
        allowBackorders = allowBackorders,
        receiptHeaderText = receiptHeaderText,
        receiptFooterText = receiptFooterText,
        createdAt = createdAt,
        updatedAt = updatedAt,
        exchangeRate = exchangeRate.value,
    )
}

internal fun LocationEntity.toContract(): Location {
    return Location(
        id = id,
        name = name,
        businessType = businessType,
        address = address,
        phone = phone,
        taxId = taxId,
        taxRate = taxRate?.let(::Decimal),
        nextSaleNumber = nextSaleNumber,
        nextPurchaseOrderNumber = nextPurchaseOrderNumber,
        allowBackorders = allowBackorders,
        receiptHeaderText = receiptHeaderText,
        receiptFooterText = receiptFooterText,
        createdAt = createdAt,
        updatedAt = updatedAt,
        exchangeRate = Decimal(exchangeRate),
    )
}

