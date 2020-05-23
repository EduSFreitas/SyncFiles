package com.rafalk.syncfiles.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DirsPair::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dirsPairDao(): DirsPairDao
}
