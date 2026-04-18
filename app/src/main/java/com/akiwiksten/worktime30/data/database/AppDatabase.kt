package com.akiwiksten.worktime30.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.akiwiksten.worktime30.data.database.dao.ProjectDao
import com.akiwiksten.worktime30.data.database.dao.ProjectDetailsDao
import com.akiwiksten.worktime30.data.database.dao.ProjectNameDao
import com.akiwiksten.worktime30.data.database.dao.SettingsDao
import com.akiwiksten.worktime30.data.database.dao.WorkStatsDao
import com.akiwiksten.worktime30.data.database.dao.WorkTypeDao
import com.akiwiksten.worktime30.data.database.entity.ProjectDetailsEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.database.entity.SettingsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity

@Database(
    entities = [
        ProjectDetailsEntity::class,
        WorkStatsEntity::class,
        ProjectEntity::class,
        ProjectNameEntity::class,
        SettingsEntity::class,
        WorkTypeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDetailsDao(): ProjectDetailsDao
    abstract fun workStatsDao(): WorkStatsDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectNameDao(): ProjectNameDao
    abstract fun settingsDao(): SettingsDao
    abstract fun workTypeDao(): WorkTypeDao

    companion object {
        const val DB_NAME = "ajvw-db"
    }
}
