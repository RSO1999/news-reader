
package com.storystream.reader_app.di

import com.storystream.reader_app.data.TokenProvider
import com.storystream.reader_app.network.AuthApi
import com.storystream.reader_app.network.ArticlesApi
import com.storystream.reader_app.network.TokenAuthenticator
import com.storystream.reader_app.network.AuthInterceptor
import com.storystream.reader_app.network.RefreshApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module that provides network singletons (OkHttp + Retrofit + APIs).
 */
@Module
@InstallIn(SingletonComponent::class)
object DiNetworkModule {

    @Provides
    @Singleton
    @Named("BaseUrl")
    fun provideBaseUrl(): String = "http://10.0.0.85:8080"

    @Provides
    @Singleton
    fun provideLogging(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenProvider: TokenProvider): AuthInterceptor {
        return AuthInterceptor(tokenProvider)
    }

    @Provides
    @Singleton
    fun provideTokenAuthenticator(tokenProvider: com.storystream.reader_app.data.TokenProvider, refreshApi: RefreshApi): TokenAuthenticator {
        return TokenAuthenticator(tokenProvider, refreshApi)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        logging: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(logging)
            .build()
    }

    // Dedicated refresh client WITHOUT auth interceptor to avoid recursion
    @Provides
    @Singleton
    @Named("RefreshClient")
    fun provideRefreshOkHttpClient(logging: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @Named("RefreshRetrofit")
    fun provideRefreshRetrofit(@Named("BaseUrl") baseUrl: String, @Named("RefreshClient") client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideRefreshApi(@Named("RefreshRetrofit") refreshRetrofit: Retrofit): RefreshApi {
        return refreshRetrofit.create(RefreshApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        @Named("BaseUrl") baseUrl: String,
        client: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideArticlesApi(retrofit: Retrofit): ArticlesApi {
        return retrofit.create(ArticlesApi::class.java)
    }
}
