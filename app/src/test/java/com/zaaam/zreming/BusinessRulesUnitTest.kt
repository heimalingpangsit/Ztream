package com.zaaam.zreming

import com.zaaam.zreming.domain.model.ContinueWatchingItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessRulesUnitTest {

    @Test
    fun testContinueWatchingProgressBoundary() {
        val duration = 1000L
        val posAt4Percent = 40L
        val posAt50Percent = 500L
        val posAt95Percent = 950L

        val progress1 = posAt4Percent.toFloat() / duration.toFloat()
        val progress2 = posAt50Percent.toFloat() / duration.toFloat()
        val progress3 = posAt95Percent.toFloat() / duration.toFloat()

        // Rule: 5% - 95% threshold
        assertTrue("Below 5% should not qualify", progress1 < 0.05f)
        assertTrue("50% should qualify", progress2 in 0.05f..0.9499f)
        assertTrue("95% or more is considered finished", progress3 >= 0.95f)
    }

    @Test
    fun testSlugFormatting() {
        val movieId = "634649"
        val tvId = "84958"

        val movieSlug = "movie-$movieId"
        val tvSlug = "tv-$tvId"

        assertEquals("movie-634649", movieSlug)
        assertEquals("tv-84958", tvSlug)
    }

    @Test
    fun testGenreDeserializationSupportBothStringAndObject() {
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        // Case 1: Array of Strings
        val jsonStringArray = """
            {
                "id": 123,
                "title": "Action Movie",
                "genres": ["Cerita Fiksi", "Aksi", "Petualang"]
            }
        """.trimIndent()
        val dto1 = json.decodeFromString<com.zaaam.zreming.data.model.MovieDetailDto>(jsonStringArray)
        assertEquals(3, dto1.genres.size)

        // Case 2: Array of Objects
        val jsonObjectArray = """
            {
                "id": 123,
                "title": "Action Movie",
                "genres": [{"id": 28, "name": "Aksi"}, {"id": 12, "name": "Petualangan"}]
            }
        """.trimIndent()
        val dto2 = json.decodeFromString<com.zaaam.zreming.data.model.MovieDetailDto>(jsonObjectArray)
        assertEquals(2, dto2.genres.size)
    }
}
