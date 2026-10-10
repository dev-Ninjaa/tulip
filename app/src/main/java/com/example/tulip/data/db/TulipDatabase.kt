package com.example.tulip.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tulip.data.model.JournalEntry

@Database(entities = [JournalEntry::class], version = 1, exportSchema = false)
abstract class TulipDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: TulipDatabase? = null

        fun getInstance(context: Context): TulipDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TulipDatabase::class.java,
                    "tulip_journal.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
