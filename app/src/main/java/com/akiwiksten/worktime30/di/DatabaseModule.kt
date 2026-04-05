package com.akiwiksten.worktime30.di

import android.content.Context
import androidx.room.Room
import com.akiwiksten.worktime30.data.database.AppDatabase
import com.akiwiksten.worktime30.data.database.dao.ProjectDao
import com.akiwiksten.worktime30.data.database.dao.ProjectNameDao
import com.akiwiksten.worktime30.data.database.dao.SettingsDao
import com.akiwiksten.worktime30.data.database.dao.WorkDayDao
import com.akiwiksten.worktime30.data.database.dao.WorkDayOneRowDao
import com.akiwiksten.worktime30.data.database.dao.WorkTypeDao
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepositoryImpl
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepositoryImpl
import com.akiwiksten.worktime30.data.repository.WorkDayRepository
import com.akiwiksten.worktime30.data.repository.WorkDayRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    @Singleton
    fun bindWorkDayRepository(impl: WorkDayRepositoryImpl): WorkDayRepository

    @Binds
    @Singleton
    fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds
    @Singleton
    fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DB_NAME
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    @Provides
    fun provideWorkDayDao(database: AppDatabase): WorkDayDao = database.workDayDao()

    @Provides
    fun provideWorkDayOneRowDao(database: AppDatabase): WorkDayOneRowDao = database.workDayOneRowDao()

    @Provides
    fun provideProjectDao(database: AppDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideProjectNameDao(database: AppDatabase): ProjectNameDao = database.projectNameDao()

    @Provides
    fun provideSettingsDao(database: AppDatabase): SettingsDao = database.settingsDao()

    @Provides
    fun provideWorkTypeDao(database: AppDatabase): WorkTypeDao = database.workTypeDao()
}
