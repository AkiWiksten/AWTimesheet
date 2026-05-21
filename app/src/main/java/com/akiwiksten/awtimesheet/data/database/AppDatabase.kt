package com.akiwiksten.awtimesheet.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.akiwiksten.awtimesheet.data.database.dao.CalculatedFlexTimeTotalDao
import com.akiwiksten.awtimesheet.data.database.dao.ProjectDao
import com.akiwiksten.awtimesheet.data.database.dao.ProjectDetailsDao
import com.akiwiksten.awtimesheet.data.database.dao.ProjectNameDao
import com.akiwiksten.awtimesheet.data.database.dao.SettingsDao
import com.akiwiksten.awtimesheet.data.database.dao.WorkTypeDao
import com.akiwiksten.awtimesheet.data.database.dao.WorkdayDao
import com.akiwiksten.awtimesheet.data.database.entity.CalculatedFlextimeTotalEntity
import com.akiwiksten.awtimesheet.data.database.entity.ProjectDetailsEntity
import com.akiwiksten.awtimesheet.data.database.entity.ProjectEntity
import com.akiwiksten.awtimesheet.data.database.entity.ProjectNameEntity
import com.akiwiksten.awtimesheet.data.database.entity.SettingsEntity
import com.akiwiksten.awtimesheet.data.database.entity.WorkTypeEntity
import com.akiwiksten.awtimesheet.data.database.entity.WorkdayEntity

@Database(
    entities = [
        ProjectDetailsEntity::class,
        ProjectEntity::class,
        WorkdayEntity::class,
        ProjectNameEntity::class,
        SettingsEntity::class,
        WorkTypeEntity::class,
        CalculatedFlextimeTotalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDetailsDao(): ProjectDetailsDao
    abstract fun projectDao(): ProjectDao
    abstract fun workdayDao(): WorkdayDao
    abstract fun projectNameDao(): ProjectNameDao
    abstract fun settingsDao(): SettingsDao
    abstract fun workTypeDao(): WorkTypeDao
    abstract fun calculatedFlexTimeTotalDao(): CalculatedFlexTimeTotalDao

    companion object {
        const val DB_NAME = "ajvw-db"
    }
}
