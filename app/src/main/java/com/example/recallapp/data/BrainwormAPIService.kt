package com.example.recallapp.data
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Paging info in the "page" object of a Spring PagedModel response. */
data class PageMeta(
    val size: Int = 0,
    val number: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0
)

/**
 * One page of results. Works with both Spring page formats:
 * the plain Page ("last" at the top) and PagedModel ("page" object).
 */
data class SpringPage<T>(
    val content: List<T> = emptyList(),
    val last: Boolean? = null,
    val page: PageMeta? = null
) {
    /** True when there are no more pages to load. */
    fun isLastPage(): Boolean {
        return last ?: page?.let { it.number + 1 >= it.totalPages } ?: true
    }
}

/** An FDA recall matched to one of the user's registry items. */
data class RecalledFood(
    val id: Long? = null,
    val product: String? = null,
    val contaminant: String? = null,
    val recallDate: String? = null,
    val batch: String? = null
)

/** A food the user is tracking. recalledFoods is filled in by the server. */
data class RegistryItem(
    val id: Long? = null,
    val product: String,
    val batch: String?,
    val recalledFoods: List<RecalledFood>? = null
)

interface BrainwormApiService {
    /** Creates or retrieves the database account for a verified Google token. */
    @POST("users/me")
    suspend fun connectGoogleAccount(
        @Header("Authorization") token: String,
    ): Response<UserDto>

    /** Deletes the signed in user's own account and all their data. */
    @DELETE("users/me")
    // Every user-owned request carries the Google token explicitly so Retrofit cannot
    // accidentally send a request that the API will reject as anonymous.
    suspend fun deleteMyAccount(
        @Header("Authorization") token: String
    ): Response<Unit>

    @GET("registry")
    suspend fun getRegistryItems(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<SpringPage<RegistryItem>>

    @GET("registry/{itemId}")
    suspend fun getRegistryItem(
        @Header("Authorization") token: String,
        @Path("itemId") itemId: Long
    ): Response<RegistryItem>

    @POST("registry")
    suspend fun createRegistryItem(
        @Header("Authorization") token: String,
        @Body item: RegistryItem
    ): Response<RegistryItem>

    @PUT("registry/{itemId}")
    suspend fun updateRegistryItem(
        @Header("Authorization") token: String,
        @Path("itemId") itemId: Long,
        @Body item: RegistryItem
    ): Response<RegistryItem>

    @DELETE("registry/{itemId}")
    suspend fun deleteRegistryItem(
        @Header("Authorization") token: String,
        @Path("itemId") itemId: Long
    ): Response<Unit>

    /**
     * Fetches one page of users.
     * Page numbering starts at 0.
     */
    @GET("users")
    suspend fun getUsers(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): Response<UserPageDto>

    /**
     * Changes a user's role and returns the updated database record.
     * admin=true promotes; admin=false makes the user a normie.
     */
    @PATCH("users/{userId}")
    suspend fun updateUserRole(
        @Path("userId") userId: Long,
        @Body request: UpdateUserRoleRequest,
    ): Response<UserDto>

    /**
     * Deletes a user. The current backend returns HTTP 204 with no body.
     */
    @DELETE("users/{userId}")
    suspend fun deleteUser(
        @Path("userId") userId: Long,
    ): Response<Unit>

    /**
     * Reads one page of recalls.
     * A null product omits the filter and returns all recalls for that page.
     */
    @GET("recalls")
    suspend fun getRecalls(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("product") product: String? = null,
    ): Response<RecallPageDto>
}
