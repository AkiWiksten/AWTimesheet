package com.akiwiksten.worktime30.di

import android.content.Context
import androidx.room.Room
import com.akiwiksten.worktime30.data.ProjectRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepositoryImpl
import com.akiwiksten.worktime30.data.SettingsRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepositoryImpl
import com.akiwiksten.worktime30.data.WorkDayRepository
import com.akiwiksten.worktime30.data.WorkDayRepositoryImpl
import com.akiwiksten.worktime30.data.database.AppDatabase
import com.akiwiksten.worktime30.data.database.ProjectDao
import com.akiwiksten.worktime30.data.database.ProjectNameDao
import com.akiwiksten.worktime30.data.database.SettingsDao
import com.akiwiksten.worktime30.data.database.WorkDayDao
import com.akiwiksten.worktime30.data.database.WorkDayOneRowDao
import com.akiwiksten.worktime30.data.database.WorkTypeDao
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
