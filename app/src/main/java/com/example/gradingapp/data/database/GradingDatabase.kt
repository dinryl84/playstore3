package com.example.gradingapp.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.gradingapp.data.dao.*
import com.example.gradingapp.data.entity.*

@Database(
    entities = [
        StudentEntity::class,
        SectionEntity::class,
        SubjectEntity::class,
        ScoreEntity::class,
        SubjectSectionCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GradingDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun sectionDao(): SectionDao
    abstract fun subjectDao(): SubjectDao
    abstract fun scoreDao(): ScoreDao
    abstract fun subjectSectionCrossRefDao(): SubjectSectionCrossRefDao

    companion object {
        @Volatile
        private var INSTANCE: GradingDatabase? = null

        fun getDatabase(context: Context): GradingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GradingDatabase::class.java,
                    "grading_database"
                )
                    .fallbackToDestructiveMigration() // For development only
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * For testing purposes - allows providing a custom database instance
         */
        fun getTestDatabase(context: Context): GradingDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                GradingDatabase::class.java
            ).build()
        }
    }
}