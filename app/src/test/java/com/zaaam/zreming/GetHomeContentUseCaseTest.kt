package com.zaaam.zreming

import com.zaaam.zreming.domain.model.ContentItem
import com.zaaam.zreming.domain.model.ContentType
import com.zaaam.zreming.domain.repository.ContentRepository
import com.zaaam.zreming.domain.usecase.GetHomeContentUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GetHomeContentUseCaseTest {

    private val repository: ContentRepository = mockk()
    private val useCase = GetHomeContentUseCase(repository)

    @Test
    fun testPartialFailureHandledGracefully() = runTest {
        val dummyItem = ContentItem(
            id = "1",
            slug = "movie-1",
            title = "Dummy",
            posterUrl = "",
            backdropUrl = "",
            type = ContentType.MOVIE,
            rating = 8.0f,
            releaseDate = "2024",
            overview = "Desc"
        )

        // Mock: hero & trending succeed, popular fails
        coEvery { repository.getHero() } returns listOf(dummyItem)
        coEvery { repository.getTrending(1) } returns listOf(dummyItem)
        coEvery { repository.getPopular(any(), any()) } throws RuntimeException("Network timeout")
        coEvery { repository.getTopRated(any(), any()) } returns emptyList()

        val (hero, sections) = useCase()

        assertNotNull(hero)
        assertEquals(1, sections.size)
        assertEquals("Trending Minggu Ini", sections[0].title)
    }
}
