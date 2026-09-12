package com.zaaam.zreming

import com.zaaam.zreming.domain.model.ContentItem
import com.zaaam.zreming.domain.model.ContentType
import com.zaaam.zreming.domain.repository.ContentRepository
import com.zaaam.zreming.domain.usecase.SearchContentUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchContentUseCaseTest {

    private val repository: ContentRepository = mockk()
    private val useCase = SearchContentUseCase(repository)

    @Test
    fun testSearchContentReturnsResults() = runTest {
        val dummyItems = listOf(
            ContentItem(
                id = "1",
                slug = "movie-1",
                title = "Spider-Man",
                posterUrl = "",
                backdropUrl = "",
                type = ContentType.MOVIE,
                rating = 8.5f,
                releaseDate = "2021",
                overview = "Desc"
            )
        )

        coEvery { repository.search("Spider", 1) } returns dummyItems

        val results = useCase("Spider", 1)

        assertEquals(1, results.size)
        assertEquals("Spider-Man", results[0].title)
        coVerify(exactly = 1) { repository.search("Spider", 1) }
    }
}
