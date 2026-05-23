package com.akiwiksten.awtimesheet.core.di

import android.content.Context
import com.akiwiksten.awtimesheet.core.APP_NAME_QUALIFIER
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppInfoModule {

    @Provides
    @Singleton
    @Named(APP_NAME_QUALIFIER)
    fun provideAppName(@ApplicationContext context: Context): String {
        val applicationInfo = context.applicationInfo
        return if (applicationInfo.labelRes == 0) {
            applicationInfo.nonLocalizedLabel?.toString() ?: "WorkTime 3.0"
        } else {
            context.getString(applicationInfo.labelRes)
        }
    }
}

