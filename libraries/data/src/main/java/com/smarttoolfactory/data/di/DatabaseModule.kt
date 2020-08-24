package com.smarttoolfactory.data.di

import android.app.Application
import androidx.room.Room
import com.smarttoolfactory.data.db.DATABASE_NAME
import com.smarttoolfactory.data.db.PostDao
import com.smarttoolfactory.data.db.PostDaoRxJava3
import com.smarttoolfactory.data.db.PostDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
class DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(application: Application): PostDatabase {
        return Room.databaseBuilder(
            application,
            PostDatabase::class.java,
            DATABASE_NAME
        ).build()
    }

    @Singleton
    @Provides
    fun provideWPostDao(appDatabase: PostDatabase): PostDao {
        return appDatabase.postDao()
    }

    @Provides
    fun provideWPostDaoRxJava3(appDatabase: PostDatabase): PostDaoRxJava3 {
        return appDatabase.postDaoRxJava()
    }
}