package com.civic.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ReportEntity::class], version = 1, exportSchema = false)
abstract class CivicDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
}
