package com.civic.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY capturedAt DESC")
    fun observeAll(): Flow<List<ReportEntity>>

    /** Null arguments mean "any". */
    @Query(
        "SELECT * FROM reports WHERE (:category IS NULL OR category = :category) " +
            "AND (:status IS NULL OR status = :status) ORDER BY capturedAt DESC",
    )
    fun observeFiltered(category: String?, status: String?): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE localId = :id")
    fun observeById(id: Long): Flow<ReportEntity?>

    @Query("SELECT * FROM reports WHERE isSynced = 0")
    suspend fun getUnsynced(): List<ReportEntity>

    @Query("UPDATE reports SET upvotes = upvotes + 1 WHERE localId = :id")
    suspend fun upvote(id: Long)

    @Query("UPDATE reports SET status = :status WHERE localId = :id")
    suspend fun setStatus(id: Long, status: String)

    @Insert
    suspend fun insert(report: ReportEntity): Long

    @Update
    suspend fun update(report: ReportEntity)

    @Delete
    suspend fun delete(report: ReportEntity)
}
