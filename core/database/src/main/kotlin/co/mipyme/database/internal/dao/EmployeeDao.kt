package co.mipyme.database.internal.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import co.mipyme.contract.EmployeeStatus
import co.mipyme.database.internal.entity.EmployeeEntity
import co.mipyme.database.internal.entity.EmployeeLocationEntity
import co.mipyme.database.internal.relation.EmployeeLocationRow
import kotlinx.coroutines.flow.Flow

@Dao
internal interface EmployeeDao {
    @Upsert
    suspend fun upsertAll(items: List<EmployeeEntity>)

    @Query("DELETE FROM employee_locations WHERE employee_id IN (:employeeIds)")
    suspend fun deleteLocationLinksOf(employeeIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationLinks(items: List<EmployeeLocationEntity>)

    @Query(
        """
        SELECT sync_state.merchant_id AS merchant_id, employees.is_owner AS is_owner, locations.*
        FROM sync_state
        LEFT JOIN employees ON employees.id = :employeeId AND (employees.is_owner = 1 OR employees.status = :status)
        LEFT JOIN locations ON employees.is_owner = 1 OR locations.id IN (
            SELECT location_id FROM employee_locations WHERE employee_id = employees.id
        )
        ORDER BY locations.name COLLATE NOCASE, locations.id
        """,
    )
    fun observeLocationRows(employeeId: String, status: EmployeeStatus): Flow<List<EmployeeLocationRow>>

    @Query("DELETE FROM employee_locations")
    suspend fun deleteAllLocationLinks()

    @Query("DELETE FROM employees")
    suspend fun deleteAll()
}
