package com.akiwiksten.worktime30.di

import android.content.Context
import androidx.room.Room
import com.akiwiksten.worktime30.data.database.AppDatabase
import com.akiwiksten.worktime30.data.database.dao.ProjectDao
import com.akiwiksten.worktime30.data.database.dao.ProjectDetailsDao
import com.akiwiksten.worktime30.data.database.dao.ProjectNameDao
import com.akiwiksten.worktime30.data.database.dao.SettingsDao
import com.akiwiksten.worktime30.data.database.dao.WorkdayDao
import com.akiwiksten.worktime30.data.database.dao.WorkStatsDao
import com.akiwiksten.worktime30.data.database.dao.WorkTypeDao
import com.akiwiksten.worktime30.data.database.migration.AppMigrations
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepositoryImpl
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepositoryImpl
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepositoryImpl
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

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                AppDatabase.DB_NAME
            ).addMigrations(
                AppMigrations.MIGRATION_3_4,
                AppMigrations.MIGRATION_4_5
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }

        @Provides
        fun provideProjectDetailsDao(database: AppDatabase): ProjectDetailsDao = database.projectDetailsDao()

        @Provides
        fun provideWorkStatsDao(database: AppDatabase): WorkStatsDao = database.workStatsDao()

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
    }
}
