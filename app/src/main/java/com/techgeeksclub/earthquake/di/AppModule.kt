package com.techgeeksclub.earthquake.di

import android.content.Context
import android.media.MediaPlayer
import com.techgeeksclub.earthquake.R
import com.techgeeksclub.earthquake.data.datasource.EarthquakeDataSource
import com.techgeeksclub.earthquake.data.datasource.EmergencyDataSource
import com.techgeeksclub.earthquake.data.datasource.InformationDataSource
import com.techgeeksclub.earthquake.data.repository.EarthquakeRepository
import com.techgeeksclub.earthquake.data.repository.EmergencyRepository
import com.techgeeksclub.earthquake.data.repository.InformationRepository
import com.techgeeksclub.earthquake.retrofit.ApiUtils
import com.techgeeksclub.earthquake.retrofit.EarthquakeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Singleton
    fun provideEarthquakeDao() : EarthquakeDao {
        return ApiUtils.getEarthquakeDao()
    }

    @Provides
    @Singleton
    fun provideEarthquakeDataSource(context: Context, earthquakeDao: EarthquakeDao) : EarthquakeDataSource {
        return EarthquakeDataSource(provideContext(context),earthquakeDao)
    }

    @Provides
    @Singleton
    fun provideEarthquakeRepository(earthquakeDataSource: EarthquakeDataSource) : EarthquakeRepository {
        return EarthquakeRepository(earthquakeDataSource)
    }

    @Provides
    @Singleton
    fun provideEmergencyDataSource(context: Context) : EmergencyDataSource {
        return EmergencyDataSource(provideContext(context))
    }
    @Provides
    @Singleton
    fun provideEmergencyRepository(emergencyDataSource: EmergencyDataSource) : EmergencyRepository {
        return EmergencyRepository(emergencyDataSource)
    }

    @Provides
    @Singleton
    fun provideInformationDataSource() : InformationDataSource{
        return InformationDataSource()
    }

    @Provides
    @Singleton
    fun provideInformationRepository(informationDataSource: InformationDataSource) : InformationRepository{
        return InformationRepository(informationDataSource)
    }




    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    fun provideMediaPlayer(@ApplicationContext context: Context): MediaPlayer {
        return MediaPlayer.create(context, R.raw.whistle_sound)
    }
}