package com.akiwiksten.awtimesheet.data.di

import android.content.Context
import androidx.room.Room
import com.akiwiksten.awtimesheet.data.database.AppDatabase
import com.akiwiksten.awtimesheet.data.database.dao.AbsenceDao
import com.akiwiksten.awtimesheet.data.database.dao.CalculatedFlexTimeTotalDao
import com.akiwiksten.awtimesheet.data.database.dao.ProjectDao
import com.akiwiksten.awtimesheet.data.database.dao.ProjectDetailsDao
import com.akiwiksten.awtimesheet.data.database.dao.ProjectNameDao
import com.akiwiksten.awtimesheet.data.database.dao.SettingsDao
import com.akiwiksten.awtimesheet.data.database.dao.WorkTypeDao
import com.akiwiksten.awtimesheet.data.database.dao.WorkdayDao
import com.akiwiksten.awtimesheet.data.repository.AbsenceRepositoryImpl
import com.akiwiksten.awtimesheet.data.repository.DateRepositoryImpl
import com.akiwiksten.awtimesheet.data.repository.ProjectDetailsRepositoryImpl
import com.akiwiksten.awtimesheet.data.repository.ProjectRepositoryImpl
import com.akiwiksten.awtimesheet.data.repository.SettingsRepositoryImpl
import com.akiwiksten.awtimesheet.data.repository.WorkdayRepositoryImpl
import com.akiwiksten.awtimesheet.domain.repository.AbsenceRepository
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectDetailsRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindProjectDetailsRepository(impl: ProjectDetailsRepositoryImpl): ProjectDetailsRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindWorkdayRepository(impl: WorkdayRepositoryImpl): WorkdayRepository

    @Binds
    @Singleton
    abstract fun bindDateRepository(impl: DateRepositoryImpl): DateRepository

    @Binds
    @Singleton
    abstract fun bindAbsenceRepository(impl: AbsenceRepositoryImpl): AbsenceRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                AppDatabase.DB_NAME
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }

        @Provides
        fun provideProjectDetailsDao(database: AppDatabase): ProjectDetailsDao = database.projectDetailsDao()

        @Provides
        fun provideProjectDao(database: AppDatabase): ProjectDao = database.projectDao()

        @Provides
        fun provideWorkdayDao(database: AppDatabase): WorkdayDao = database.workdayDao()

        @Provides
        fun provideProjectNameDao(database: AppDatabase): ProjectNameDao = database.projectNameDao()

        @Provides
        fun provideSettingsDao(database: AppDatabase): SettingsDao = database.settingsDao()

        @Provides
        fun provideWorkTypeDao(database: AppDatabase): WorkTypeDao = database.workTypeDao()

        @Provides
        fun provideCalculatedFlexTimeTotalDao(database: AppDatabase): CalculatedFlexTimeTotalDao =
            database.calculatedFlexTimeTotalDao()

        @Provides
        fun provideAbsenceDao(database: AppDatabase): AbsenceDao = database.absenceDao()
    }
}
