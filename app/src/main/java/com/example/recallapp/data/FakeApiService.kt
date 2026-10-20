package com.example.recallapp.data

import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/**
 * Keeps items in memory instead of calling the server.
 * Used when USE_FAKE_API=true is in local.properties, so we can test the screens without the API.
 */
class FakeApiService : BrainwormApiService {
    private val items = mutableListOf(
        RegistryItem(1, "Frozen dumplings", "24215", listOf(RecalledFood(contaminant = "Listeria", recallDate = "2026-10-05"))),
        RegistryItem(2, "Rolled oats", null),
        RegistryItem(3, "Baby spinach", "B-7731"),
        RegistryItem(4, "Peanut butter", null, listOf(RecalledFood(contaminant = "Salmonella", recallDate = "2026-10-08"))),
    )
    private var nextId = 5L

    override suspend fun deleteMyAccount(token: String): Response<Unit> {
        items.clear()
        return Response.success(Unit)
    }

    override suspend fun getRegistryItems(token: String, page: Int, size: Int): Response<SpringPage<RegistryItem>> =
        Response.success(SpringPage(items.toList(), last = true))

    override suspend fun getRegistryItem(token: String, itemId: Long): Response<RegistryItem> {
        val item = items.find { it.id == itemId } ?: return Response.error(404, "".toResponseBody(null))
        return Response.success(item)
    }

    override suspend fun createRegistryItem(token: String, item: RegistryItem): Response<RegistryItem> {
        val saved = item.copy(id = nextId++)
        items += saved
        return Response.success(saved)
    }

    override suspend fun updateRegistryItem(token: String, itemId: Long, item: RegistryItem): Response<RegistryItem> {
        val index = items.indexOfFirst { it.id == itemId }
        if (index == -1) return Response.error(404, "".toResponseBody(null))
        items[index] = item.copy(id = itemId, recalledFoods = items[index].recalledFoods)
        return Response.success(items[index])
    }

    override suspend fun deleteRegistryItem(token: String, itemId: Long): Response<Unit> {
        items.removeAll { it.id == itemId }
        return Response.success(Unit)
    }

    // Admin calls are not part of the fake data yet, so they return empty results.
    override suspend fun getUsers(page: Int, size: Int): Response<UserPageDto> =
        Response.success(UserPageDto(emptyList(), number = 0, last = true, totalElements = 0))

    override suspend fun updateUserRole(userId: Long, request: UpdateUserRoleRequest): Response<UserDto> =
        Response.error(404, "".toResponseBody(null))

    override suspend fun deleteUser(userId: Long): Response<Unit> = Response.success(Unit)

    override suspend fun getRecalls(page: Int, size: Int, product: String?): Response<RecallPageDto> =
        Response.success(RecallPageDto(emptyList(), number = 0, last = true, totalElements = 0))
}
