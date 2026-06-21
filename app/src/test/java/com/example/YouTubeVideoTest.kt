package com.example

import com.example.data.*
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class YouTubeVideoTest {

    private val moshi = Moshi.Builder().build()

    @Test
    fun testYouTubeSearchResponseDeserialization() {
        val mockJson = """
            {
              "items": [
                {
                  "id": {
                    "videoId": "dQw4w9WgXcQ"
                  },
                  "snippet": {
                    "title": "Bicep Curls Form Tutorial",
                    "description": "Learn the proper form for performing dumbell bicep curls.",
                    "thumbnails": {
                      "high": {
                        "url": "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"
                      }
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val adapter = moshi.adapter(YouTubeSearchResponse::class.java)
        val response = adapter.fromJson(mockJson)

        assertNotNull(response)
        assertNotNull(response?.items)
        assertEquals(1, response?.items?.size)

        val firstItem = response?.items?.first()
        assertEquals("dQw4w9WgXcQ", firstItem?.id?.videoId)
        assertEquals("Bicep Curls Form Tutorial", firstItem?.snippet?.title)
        assertEquals("Learn the proper form for performing dumbell bicep curls.", firstItem?.snippet?.description)
        assertEquals("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg", firstItem?.snippet?.thumbnails?.high?.url)
    }

    @Test
    fun testYouTubeSearchResponse_EmptyOrNullFields() {
        val mockJson = """
            {
              "items": [
                {
                  "id": null,
                  "snippet": null
                }
              ]
            }
        """.trimIndent()

        val adapter = moshi.adapter(YouTubeSearchResponse::class.java)
        val response = adapter.fromJson(mockJson)

        assertNotNull(response)
        assertEquals(1, response?.items?.size)
        val item = response?.items?.first()
        assertNull(item?.id)
        assertNull(item?.snippet)
    }

    @Test
    fun testYouTubeBridge_FindExerciseVideos_WithNoApiKey_GracefulEmptyList() = runBlocking {
        val bridge = YouTubeBridge()
        // Without YOUTUBE_API_KEY configured correctly in build variant properties,
        // search API should log a warning and return emptyList without throwing exceptions.
        val results = bridge.findExerciseVideos("Squats")
        assertNotNull(results)
        assertTrue(results.isEmpty())
    }
}
