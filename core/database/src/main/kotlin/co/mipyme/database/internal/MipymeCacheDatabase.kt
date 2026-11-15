package co.mipyme.database.internal

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import co.mipyme.database.internal.dao.CollectionDao
import co.mipyme.database.internal.dao.CustomerDao
import co.mipyme.database.internal.dao.EmployeeDao
import co.mipyme.database.internal.dao.LocationDao
import co.mipyme.database.internal.dao.MerchantDao
import co.mipyme.database.internal.dao.ProductDao
import co.mipyme.database.internal.dao.SupplierDao
import co.mipyme.database.internal.dao.SyncStateDao
import co.mipyme.database.internal.entity.CollectionEntity
import co.mipyme.database.internal.entity.CustomerEntity
import co.mipyme.database.internal.entity.EmployeeEntity
import co.mipyme.database.internal.entity.EmployeeLocationEntity
import co.mipyme.database.internal.entity.InventoryLevelEntity
import co.mipyme.database.internal.entity.LocationEntity
import co.mipyme.database.internal.entity.MerchantEntity
import co.mipyme.database.internal.entity.OptionGroupEntity
import co.mipyme.database.internal.entity.ProductEntity
import co.mipyme.database.internal.entity.ProductOptionEntity
import co.mipyme.database.internal.entity.ProductOptionGroupEntity
import co.mipyme.database.internal.entity.ProductVariantEntity
import co.mipyme.database.internal.entity.SupplierEntity
import co.mipyme.database.internal.entity.SyncStateEntity
import co.mipyme.database.internal.entity.VariantOptionEntity

@Database(
    entities = [
        MerchantEntity::class,
        CollectionEntity::class,
        CustomerEntity::class,
        SupplierEntity::class,
        LocationEntity::class,
        EmployeeEntity::class,
        EmployeeLocationEntity::class,
        ProductEntity::class,
        OptionGroupEntity::class,
        ProductOptionGroupEntity::class,
        ProductOptionEntity::class,
        ProductVariantEntity::class,
        VariantOptionEntity::class,
        InventoryLevelEntity::class,
        SyncStateEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(DatabaseTypeConverters::class)
internal abstract class MipymeCacheDatabase : RoomDatabase() {
    abstract fun merchantDao(): MerchantDao

    abstract fun collectionDao(): CollectionDao

    abstract fun customerDao(): CustomerDao

    abstract fun supplierDao(): SupplierDao

    abstract fun locationDao(): LocationDao

    abstract fun employeeDao(): EmployeeDao

    abstract fun productDao(): ProductDao

    abstract fun syncStateDao(): SyncStateDao
}
