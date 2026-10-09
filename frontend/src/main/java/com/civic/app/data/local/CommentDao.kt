package com.civic.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE reportId = :reportId ORDER BY createdAt ASC")
    fun observeForReport(reportId: Long): Flow<List<CommentEntity>>

    @Insert
    suspend fun insert(comment: CommentEntity): Long

    @Query("SELECT * FROM comments WHERE isSynced = 0 AND authorId = :authorId ORDER BY createdAt ASC")
    suspend fun getPendingFor(authorId: String): List<CommentEntity>

    @Query("SELECT * FROM comments WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): CommentEntity?

    @Query("UPDATE comments SET remoteId = :remoteId, isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long, remoteId: String)

    /** Pulled server comments: a duplicate remoteId means we already have it. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(comment: CommentEntity): Long

    @Query("UPDATE comments SET author = :name, authorAvatar = :avatar WHERE authorId = :authorId")
    suspend fun updateAuthorDetails(authorId: String, name: String, avatar: String?)
}
