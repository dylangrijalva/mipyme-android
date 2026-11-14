package co.mipyme.database.internal.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import co.mipyme.contract.Collection
import java.time.LocalDateTime

@Entity(tableName = "collections")
internal data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val slug: String,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime,
)

internal fun Collection.toEntity(): CollectionEntity {
    return CollectionEntity(
        id = id,
        name = name,
        slug = slug,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

internal fun CollectionEntity.toContract(): Collection {
    return Collection(
        id = id,
        name = name,
        slug = slug,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

