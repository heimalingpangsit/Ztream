package com.zaaam.zreming.di

import android.content.Context
import androidx.room.Room
import com.zaaam.zreming.data.local.AppDatabase
import com.zaaam.zreming.data.local.ContentDao
import com.zaaam.zreming.data.remote.MovieZoneApi
import com.zaaam.zreming.data.repository.ContentRepositoryImpl
import com.zaaam.zreming.domain.repository.ContentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

import com.zaaam.zreming.util.StringFog

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Obfuscated via StringFog to protect against static reverse engineering
    private val STANDARD_USER_AGENT = StringFog.decrypt("FzUgMzY2O3VvdGp6cg0zND41LSl6FA56a2p0amF6DTM0bG5heiJsbnN6GyoqNj8NPzgRMy51b2ltdGlsenIREg4XFnZ6NjMxP3odPzkxNXN6GTIoNTc/dWtobnRqdGp0anoJOzw7KDN1b2ltdGls")
    private val BASE_API_URL = StringFog.decrypt("Mi4uKilgdXU3NSwzPyA1ND90LT84dDM+dQ==")

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", STANDARD_USER_AGENT)
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("resolverClient")
    fun provideResolverOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", STANDARD_USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideMovieZoneApi(okHttpClient: OkHttpClient, json: Json): MovieZoneApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_API_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(MovieZoneApi::class.java)
    }

    // Backend auth/role terpisah (Cloudflare Worker JepVerse) — tidak lewat
    // interceptor User-Agent khusus MovieZone, base URL beda & tidak di-obfuscate
    // karena ini domain milik sendiri.
    private const val AUTH_BASE_URL = "https://jepverse.pokaycore.workers.dev/"

    @Provides
    @Singleton
    @Named("authClient")
    fun provideAuthOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(@Named("authClient") okHttpClient: OkHttpClient, json: Json): com.zaaam.zreming.data.remote.AuthApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(AUTH_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(com.zaaam.zreming.data.remote.AuthApi::class.java)
    }
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
            "ztream.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideContentDao(database: AppDatabase): ContentDao {
        return database.contentDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindContentRepository(
        impl: ContentRepositoryImpl
    ): ContentRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: com.zaaam.zreming.data.repository.AuthRepositoryImpl
    ): com.zaaam.zreming.domain.repository.AuthRepository

    @Binds
    @Singleton
    abstract fun bindSocialRepository(
        impl: com.zaaam.zreming.data.repository.SocialRepositoryImpl
    ): com.zaaam.zreming.domain.repository.SocialRepository
}
