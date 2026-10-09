package com.civic.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ReportEntity::class, CommentEntity::class], version = 4, exportSchema = true)
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

        /**
         * v4: accounts. Reports and comments gain an author, reports gain a remote photo URL, a public/private
         * visibility flag and a per-user upvote flag; comments gain sync state. All additive, so existing rows
         * survive — a report written before accounts existed simply has a null author and shows as "Unknown".
         *
         * The unique index on reports.remoteId is what makes pulling the shared feed idempotent. SQLite treats
         * NULLs as distinct in a unique index, so the many not-yet-synced local reports do not collide.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reports ADD COLUMN authorId TEXT")
                db.execSQL("ALTER TABLE reports ADD COLUMN authorName TEXT")
                db.execSQL("ALTER TABLE reports ADD COLUMN authorAvatar TEXT")
                db.execSQL("ALTER TABLE reports ADD COLUMN imageUrl TEXT")
                db.execSQL("ALTER TABLE reports ADD COLUMN visibility TEXT NOT NULL DEFAULT 'public'")
                db.execSQL("ALTER TABLE reports ADD COLUMN upvotedByMe INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_reports_remoteId ON reports (remoteId)")
                db.execSQL("ALTER TABLE comments ADD COLUMN authorId TEXT")
                db.execSQL("ALTER TABLE comments ADD COLUMN authorAvatar TEXT")
                db.execSQL("ALTER TABLE comments ADD COLUMN remoteId TEXT")
                db.execSQL("ALTER TABLE comments ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_comments_remoteId ON comments (remoteId)")
                // Safety reports were always author-private; mark them so the server's RLS keeps them that way.
                db.execSQL(
                    "UPDATE reports SET visibility = 'private' WHERE category IN " +
                        "('UNSAFE_WOMEN', 'UNSAFE_CHILDREN', 'UNSAFE_GENERAL')",
                )
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
    }
}
