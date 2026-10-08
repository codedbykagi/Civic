package com.civic.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ReportEntity::class, CommentEntity::class], version = 2, exportSchema = false)
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
    }
}
