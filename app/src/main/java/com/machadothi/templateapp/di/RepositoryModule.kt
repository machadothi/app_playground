package com.machadothi.templateapp.di

import com.machadothi.templateapp.repository.data.DataRepository
import com.machadothi.templateapp.repository.data.DataRepositoryImpl
import com.machadothi.templateapp.repository.filter.FilterRepository
import com.machadothi.templateapp.repository.filter.FilterRepositoryImpl
import com.machadothi.templateapp.repository.sensor.SensorRepository
import com.machadothi.templateapp.repository.sensor.SensorRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindsSensorRepository(impl: SensorRepositoryImpl): SensorRepository

    @Binds
    abstract fun bindsFilterRepository(impl: FilterRepositoryImpl): FilterRepository

    @Binds
    abstract fun bindsDataRepository(impl: DataRepositoryImpl): DataRepository
}