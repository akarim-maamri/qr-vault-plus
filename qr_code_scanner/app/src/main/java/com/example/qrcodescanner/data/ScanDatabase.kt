package com.example.qrcodescanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScanResultEntity::class, PasswordEntity::class, SecureNoteEntity::class], version = 3, exportSchema = false)
abstract class ScanDatabase : RoomDatabase() {
    abstract val scanResultDao: ScanResultDao
    abstract val passwordDao: PasswordDao
    abstract val secureNoteDao: SecureNoteDao

    companion object {
        @Volatile
        private var INSTANCE: ScanDatabase? = null

        fun getDatabase(context: Context): ScanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScanDatabase::class.java,
                    "scan_history_database"
                )
                .fallbackToDestructiveMigration()
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
