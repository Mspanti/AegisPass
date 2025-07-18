package com.pant.aegispass

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PasswordEntry::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aegispass_database"
                )
                    // !!! IMPORTANT: This will wipe existing data on schema changes !!!
                    // Use this for development/testing if you don't need to preserve old data.
                    // For production, you would write a proper Migration class.
                    .fallbackToDestructiveMigration() // Added this line to handle migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}