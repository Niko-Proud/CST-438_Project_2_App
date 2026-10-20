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
 * Tests our user-list HTTP request and JSON parsing
 * without depending on the running Spring Boot API.
 */
class UsersApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: BrainwormApiService

    /**
     * Gives each test a fresh server and directs Retrofit to it
     * instead of the Docker API.
     */
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

    /** Stops the server so each test releases its resources. */
    @After
    fun tearDown() {
        server.shutdown()
    }

    /** Checks the request URL and conversion of both user roles. */
    @Test
    fun getUsersReadsAdminAndNormie() = runTest {
        // Arrange: prepare the response our fake server will return.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "content": [
                        {
                          "id": 1,
                          "username": "admin_Kiya",
                          "admin": true
                        },
                        {
                          "id": 2,
                          "username": "normie",
                          "admin": false
                        }
                      ],
                      "number": 0,
                      "last": true,
                      "totalElements": 2
                    }
                    """.trimIndent(),
                ),
        )

        // Act: call the same Retrofit method the screen will use.
        val response = api.getUsers(page = 0, size = 20)

        // Assert: check the parsed response.
        assertTrue(response.isSuccessful)

        val page = requireNotNull(response.body())

        assertEquals(2, page.content.size)
        assertEquals(2L, page.totalElements)
        assertEquals(0, page.number)
        assertTrue(page.last)

        val admin = page.content[0]
        assertEquals(1L, admin.id)
        assertEquals("admin_Kiya", admin.username)
        assertTrue(admin.admin)

        val normie = page.content[1]
        assertEquals(2L, normie.id)
        assertEquals("normie", normie.username)
        assertFalse(normie.admin)

        // Check what Retrofit actually sent over HTTP.
        val request = requireNotNull(
            server.takeRequest(1, TimeUnit.SECONDS),
        )

        assertEquals("GET", request.method)
        assertEquals("/api/v1/users?page=0&size=20", request.path)
    }

    /** Checks that an empty database produces a valid empty page. */
    @Test
    fun getUsersHandlesEmptyPage() = runTest {
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

        val response = api.getUsers()

        assertTrue(response.isSuccessful)

        val page = requireNotNull(response.body())
        assertTrue(page.content.isEmpty())
        assertEquals(0L, page.totalElements)
        assertTrue(page.last)
    }

    /** Prevents a server failure from being mistaken for an empty user list. */
    @Test
    fun getUsersPreservesServerError() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(500),
        )

        val response = api.getUsers()

        assertFalse(response.isSuccessful)
        assertEquals(500, response.code())
        assertNull(response.body())
    }
}