package com.storystream.reader_app.di

import com.storystream.reader_app.data.TokenProvider
import com.storystream.reader_app.data.TokenProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bind the concrete TokenProviderImpl to the TokenProvider interface for injection.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TokenProviderModule {
    @Binds
    @Singleton
    abstract fun bindTokenProvider(impl: TokenProviderImpl): TokenProvider
}

