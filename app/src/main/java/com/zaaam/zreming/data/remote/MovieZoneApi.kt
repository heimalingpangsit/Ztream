package com.zaaam.zreming.data.remote

import com.zaaam.zreming.data.model.EpisodeListResponse
import com.zaaam.zreming.data.model.GenreListResponse
import com.zaaam.zreming.data.model.MovieDetailDto
import com.zaaam.zreming.data.model.MovieListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieZoneApi {

    @GET("api/movies/hero")
    suspend fun getHero(): MovieListResponse

    @GET("api/movies/trending")
    suspend fun getTrending(@Query("page") page: Int = 1): MovieListResponse

    @GET("api/movies/popular")
    suspend fun getPopular(
        @Query("type") type: String = "all",
        @Query("page") page: Int = 1
    ): MovieListResponse

    @GET("api/movies/top-rated")
    suspend fun getTopRated(
        @Query("type") type: String = "movie",
        @Query("page") page: Int = 1
    ): MovieListResponse

    @GET("api/movies/latest")
    suspend fun getLatest(
        @Query("type") type: String = "all",
        @Query("page") page: Int = 1
    ): MovieListResponse

    @GET("api/movies/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 1
    ): MovieListResponse

    @GET("api/movies/genres")
    suspend fun getGenres(): GenreListResponse

    @GET("api/movies/discover")
    suspend fun getDiscover(
        @Query("genre") genreId: Int,
        @Query("page") page: Int = 1
    ): MovieListResponse

    @GET("api/movies/detail/{slug}")
    suspend fun getDetail(@Path("slug") slug: String): MovieDetailDto

    @GET("api/movies/episodes/{slug}")
    suspend fun getEpisodes(
        @Path("slug") slug: String,
        @Query("season") season: Int = 1
    ): EpisodeListResponse
}
