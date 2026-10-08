package com.civic.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE reportId = :reportId ORDER BY createdAt ASC")
    fun observeForReport(reportId: Long): Flow<List<CommentEntity>>

    @Insert
    suspend fun insert(comment: CommentEntity): Long
}
