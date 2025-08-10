package com.example.gradingapp.di

import android.content.Context
import com.example.gradingapp.data.database.GradingDatabase
import com.example.gradingapp.data.repository.GradingRepository
import com.example.gradingapp.data.repository.GradeComputationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGradingDatabase(@ApplicationContext context: Context): GradingDatabase {
        return GradingDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideGradingRepository(database: GradingDatabase): GradingRepository {
        return GradingRepository(database)
    }

    @Provides
    @Singleton
    fun provideGradeComputationRepository(database: GradingDatabase): GradeComputationRepository {
        return GradeComputationRepository(database)
    }
}