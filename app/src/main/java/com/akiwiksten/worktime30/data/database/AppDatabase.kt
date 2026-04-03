package com.akiwiksten.worktime30.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.akiwiksten.worktime30.core.DB_NAME

@Database(entities = [
    WorkDay::class,
    WorkDayOneRow::class,
    Project::class,
    ProjectName::class,
    Settings::class,
    WorkType::class],
    version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workDayDao(): WorkDayDao
    abstract fun workDayOneRowDao(): WorkDayOneRowDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectNameDao(): ProjectNameDao
    abstract fun settingsDao(): SettingsDao
    abstract fun workTypeDao(): WorkTypeDao

    companion object {

        // For Singleton instantiation
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(ctx: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(ctx).also { instance = it }
            }
        }

        private fun buildDatabase(ctx: Context) =
            Room.databaseBuilder(ctx, AppDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration(false)
                .build()
    }
}