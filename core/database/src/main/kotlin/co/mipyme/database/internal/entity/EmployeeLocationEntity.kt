package co.mipyme.database.internal.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "employee_locations",
    primaryKeys = ["employee_id", "location_id"],
    foreignKeys = [
        ForeignKey(
            entity = EmployeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["employee_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("location_id")],
)
internal data class EmployeeLocationEntity(
    @ColumnInfo(name = "employee_id") val employeeId: String,
    @ColumnInfo(name = "location_id") val locationId: String,
)
