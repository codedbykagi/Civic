package com.civic.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ReportEntity::class, CommentEntity::class], version = 3, exportSchema = true)
abstract class CivicDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
    abstract fun commentDao(): CommentDao

    companion object {
        /** v2: report status + comments table. Keeps existing reports. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reports ADD COLUMN status TEXT NOT NULL DEFAULT 'REPORTED'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS comments (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, reportId INTEGER NOT NULL, " +
                        "author TEXT NOT NULL, text TEXT NOT NULL, createdAt INTEGER NOT NULL, " +
                        "FOREIGN KEY(reportId) REFERENCES reports(localId) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_comments_reportId ON comments (reportId)")
            }
        }

        /**
         * v3: photo becomes optional (localImagePath nullable); reports get timeOfDay and safety tags columns.
         * SQLite can't drop NOT NULL in place, so the table is rebuilt (same steps Room's auto-migrations use).
         * Foreign keys are not enforced during migrations, so dropping the old table keeps comments intact.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `_new_reports` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`remoteId` TEXT, `category` TEXT NOT NULL, `description` TEXT NOT NULL, `localImagePath` TEXT, " +
                        "`latitude` REAL, `longitude` REAL, `capturedAt` INTEGER NOT NULL, `upvotes` INTEGER NOT NULL, " +
                        "`isSynced` INTEGER NOT NULL, `status` TEXT NOT NULL DEFAULT 'REPORTED', `timeOfDay` TEXT, `tags` TEXT)",
                )
                db.execSQL(
                    "INSERT INTO `_new_reports` (localId, remoteId, category, description, localImagePath, latitude, " +
                        "longitude, capturedAt, upvotes, isSynced, status) SELECT localId, remoteId, category, description, " +
                        "localImagePath, latitude, longitude, capturedAt, upvotes, isSynced, status FROM `reports`",
                )
                db.execSQL("DROP TABLE `reports`")
                db.execSQL("ALTER TABLE `_new_reports` RENAME TO `reports`")
                db.query("PRAGMA foreign_key_check(`comments`)").use { cursor ->
                    check(!cursor.moveToFirst()) { "Foreign key violation in comments after migrating reports" }
                }
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}
