package com.example.recallapp.data

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Tests recall requests and JSON parsing without Docker or an emulator.
 */
class RecallsApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: BrainwormApiService

    /** Directs Retrofit to a fresh test server before each test. */
    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BrainwormApiService::class.java)
    }

    /** Releases the server after each test. */
    @After
    fun tearDown() {
        server.shutdown()
    }

    /** Checks recall fields, nullable values, and pagination parameters. */
    @Test
    fun getRecallsReadsPage() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "content": [
                        {
                          "id": 10,
                          "product": "Test oats",
                          "contaminant": "Test contaminant",
                          "recallDate": "2026-10-01",
                          "manuDate": "2026-09-01",
                          "batch": "TEST-001"
                        },
                        {
                          "id": 11,
                          "product": "Test cereal",
                          "contaminant": null,
                          "recallDate": null,
                          "manuDate": null,
                          "batch": null
                        }
                      ],
                      "number": 0,
                      "last": false,
                      "totalElements": 3
                    }
                    """.trimIndent(),
                ),
        )

        val response = api.getRecalls(page = 0, size = 2)

        assertTrue(response.isSuccessful)
        val page = requireNotNull(response.body())

        assertEquals(2, page.content.size)
        assertEquals(0, page.number)
        assertFalse(page.last)
        assertEquals(3L, page.totalElements)

        val first = page.content[0]
        assertEquals(10L, first.id)
        assertEquals("Test oats", first.product)
        assertEquals("Test contaminant", first.contaminant)
        assertEquals("2026-10-01", first.recallDate)
        assertEquals("2026-09-01", first.manuDate)
        assertEquals("TEST-001", first.batch)

        val second = page.content[1]
        assertEquals(11L, second.id)
        assertNull(second.contaminant)
        assertNull(second.recallDate)
        assertNull(second.manuDate)
        assertNull(second.batch)

        val request = requireNotNull(
            server.takeRequest(1, TimeUnit.SECONDS),
        )
        assertEquals("GET", request.method)
        assertEquals("/api/v1/recalls?page=0&size=2", request.path)
    }

    /** An empty recall database should parse successfully. */
    @Test
    fun getRecallsHandlesEmptyPage() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "content": [],
                      "number": 0,
                      "last": true,
                      "totalElements": 0
                    }
                    """.trimIndent(),
                ),
        )

        val response = api.getRecalls()

        assertTrue(response.isSuccessful)
        val page = requireNotNull(response.body())

        assertTrue(page.content.isEmpty())
        assertTrue(page.last)
        assertEquals(0L, page.totalElements)
    }

    /** Verifies that product search is sent as a query parameter. */
    @Test
    fun getRecallsSendsProductFilter() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "content": [],
                      "number": 0,
                      "last": true,
                      "totalElements": 0
                    }
                    """.trimIndent(),
                ),
        )

        val response = api.getRecalls(product = "rolled oats")
        assertTrue(response.isSuccessful)

        val request = requireNotNull(
            server.takeRequest(1, TimeUnit.SECONDS),
        )
        val url = requireNotNull(request.requestUrl)

        assertEquals("/api/v1/recalls", url.encodedPath)
        assertEquals("rolled oats", url.queryParameter("product"))
        assertEquals("0", url.queryParameter("page"))
        assertEquals("20", url.queryParameter("size"))
    }

    /** A server failure must not be mistaken for an empty recall list. */
    @Test
    fun getRecallsPreservesServerError() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(500),
        )

        val response = api.getRecalls()

        assertFalse(response.isSuccessful)
        assertEquals(500, response.code())
        assertNull(response.body())
    }
}