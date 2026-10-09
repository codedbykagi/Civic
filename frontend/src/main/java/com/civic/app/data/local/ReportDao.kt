package com.civic.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY capturedAt DESC")
    fun observeAll(): Flow<List<ReportEntity>>

    /** Null arguments mean "any". Categories in [excluded] never appear (used to keep private safety reports out). */
    @Query(
        "SELECT * FROM reports WHERE (:category IS NULL OR category = :category) " +
            "AND (:status IS NULL OR status = :status) AND category NOT IN (:excluded) ORDER BY capturedAt DESC",
    )
    fun observeFiltered(category: String?, status: String?, excluded: List<String>): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE category IN (:categories) ORDER BY capturedAt DESC")
    fun observeInCategories(categories: List<String>): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE localId = :id")
    fun observeById(id: Long): Flow<ReportEntity?>

    @Query("SELECT * FROM reports WHERE isSynced = 0")
    suspend fun getUnsynced(): List<ReportEntity>

    @Query("UPDATE reports SET upvotes = upvotes + 1 WHERE localId = :id")
    suspend fun upvote(id: Long)

    @Query("UPDATE reports SET status = :status WHERE localId = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query(
        "UPDATE reports SET category = :category, description = :description, timeOfDay = :timeOfDay, tags = :tags " +
            "WHERE localId = :id",
    )
    suspend fun updateDetails(id: Long, category: String, description: String, timeOfDay: String?, tags: String?)

    @Query("UPDATE reports SET latitude = :latitude, longitude = :longitude WHERE localId = :id")
    suspend fun setLocation(id: Long, latitude: Double, longitude: Double)

    @Query("UPDATE reports SET localImagePath = :path WHERE localId = :id")
    suspend fun setPhoto(id: Long, path: String?)

    @Query("SELECT * FROM reports WHERE localId = :id")
    suspend fun getById(id: Long): ReportEntity?

    // ---------- accounts and sync ----------

    /** Reports this account wrote that the server has not accepted yet. */
    @Query("SELECT * FROM reports WHERE isSynced = 0 AND authorId = :authorId ORDER BY capturedAt ASC")
    suspend fun getPendingFor(authorId: String): List<ReportEntity>

    @Query("SELECT * FROM reports WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): ReportEntity?

    /** Records a successful push: the server's row id, its photo URL, and that there is nothing left to send. */
    @Query("UPDATE reports SET remoteId = :remoteId, imageUrl = :imageUrl, isSynced = 1 WHERE localId = :id")
    suspend fun markSynced(id: Long, remoteId: String, imageUrl: String?)

    /** Server state wins for counts; the local photo path and localId are kept. */
    @Query(
        "UPDATE reports SET upvotes = :upvotes, status = :status, upvotedByMe = :upvotedByMe, " +
            "imageUrl = :imageUrl, authorId = :authorId, authorName = :authorName, authorAvatar = :authorAvatar " +
            "WHERE localId = :id",
    )
    suspend fun refreshFromServer(
        id: Long,
        upvotes: Int,
        status: String,
        upvotedByMe: Boolean,
        imageUrl: String?,
        authorId: String?,
        authorName: String?,
        authorAvatar: String?,
    )

    /** Optimistic upvote toggle; the real one-vote-per-user rule is the server's primary key. */
    @Query("UPDATE reports SET upvotes = MAX(0, upvotes + :delta), upvotedByMe = :upvoted WHERE localId = :id")
    suspend fun setUpvoted(id: Long, upvoted: Boolean, delta: Int)

    /**
     * Hands reports written under a device profile (or before accounts existed) to the account that just signed
     * in, so nothing posted earlier is orphaned.
     */
    @Query(
        "UPDATE reports SET authorId = :newAuthorId, authorName = :newAuthorName, isSynced = 0 " +
            "WHERE authorId IS NULL OR authorId LIKE 'local:%'",
    )
    suspend fun reattributeLocalReports(newAuthorId: String, newAuthorName: String)

    /** Keeps the denormalised author name on this user's own reports in step with a profile rename. */
    @Query("UPDATE reports SET authorName = :name, authorAvatar = :avatar WHERE authorId = :authorId")
    suspend fun updateAuthorDetails(authorId: String, name: String, avatar: String?)

    @Insert
    suspend fun insert(report: ReportEntity): Long

    /** Used when caching a pulled server row; IGNORE so a race on the unique remoteId is not fatal. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(report: ReportEntity): Long

    @Update
    suspend fun update(report: ReportEntity)

    @Delete
    suspend fun delete(report: ReportEntity)
}
