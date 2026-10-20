package com.example.recallapp.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Contract tests for the API client. A MockWebServer stands in for the real API, so we can check
 * the path and headers the app sends, and that the JSON from the server maps into our models.
 */
class BrainwormApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: BrainwormApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = RetrofitClient.create(server.url("/api/v1/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getRegistryItems_sendsPathAndToken_andMapsPage() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "content": [
                    {"id": 1, "product": "Frozen dumplings", "batch": "24215",
                     "recalledFoods": [{"id": 7, "product": "Frozen dumplings", "contaminant": "Listeria", "recallDate": "2026-10-05", "batch": "24215"}]},
                    {"id": 2, "product": "Rolled oats", "batch": null}
                  ],
                  "page": {"size": 20, "number": 0, "totalElements": 2, "totalPages": 1}
                }
                """.trimIndent()
            )
        )

        val response = api.getRegistryItems("Bearer abc123", page = 0, size = 20)

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/registry?page=0&size=20", request.path)
        assertEquals("Bearer abc123", request.getHeader("Authorization"))

        val page = response.body()!!
        assertEquals(2, page.content.size)
        assertEquals("Frozen dumplings", page.content[0].product)
        assertEquals("Listeria", page.content[0].recalledFoods!![0].contaminant)
        assertEquals("2026-10-05", page.content[0].recalledFoods!![0].recallDate)
        assertNull(page.content[1].batch)
        assertTrue(page.isLastPage())
    }

    @Test
    fun getRegistryItem_usesItemIdInPath() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id": 5, "product": "Milk", "batch": "M-1"}"""))

        val response = api.getRegistryItem("Bearer abc123", 5)

        assertEquals("/api/v1/registry/5", server.takeRequest().path)
        assertEquals("Milk", response.body()!!.product)
        assertEquals("M-1", response.body()!!.batch)
    }

    @Test
    fun createRegistryItem_sendsJsonBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id": 9, "product": "Milk", "batch": null}"""))

        val response = api.createRegistryItem("Bearer abc123", RegistryItem(product = "Milk", batch = null))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/registry", request.path)
        assertTrue(request.body.readUtf8().contains("\"product\":\"Milk\""))
        assertEquals(9L, response.body()!!.id)
    }

    @Test
    fun deleteRegistryItem_usesDeleteMethod() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        val response = api.deleteRegistryItem("Bearer abc123", 3)

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/registry/3", request.path)
        assertTrue(response.isSuccessful)
    }

    @Test
    fun unauthorized_returns401WithNoBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error": "invalid_token"}"""))

        val response = api.getRegistryItems("Bearer expired", page = 0, size = 20)

        assertEquals(401, response.code())
        assertFalse(response.isSuccessful)
        assertNull(response.body())
    }

    @Test
    fun serverError_returns500WithNoBody() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error": "Internal Server Error"}"""))

        val response = api.getRegistryItem("Bearer abc123", 1)

        assertEquals(500, response.code())
        assertFalse(response.isSuccessful)
        assertNull(response.body())
    }
}
