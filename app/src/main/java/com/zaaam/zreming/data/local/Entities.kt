package com.zaaam.zreming.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val id: String,
    val slug: String,
    val title: String,
    val posterUrl: String,
    val type: String,
    val rating: Float,
    val releaseDate: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "continue_watching")
data class ContinueWatchingEntity(
    @PrimaryKey val contentId: String,
    val slug: String,
    val title: String,
    val posterUrl: String,
    val episodeId: String?,
    val positionSec: Long,
    val durationSec: Long,
    val lastWatchedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis()
)
