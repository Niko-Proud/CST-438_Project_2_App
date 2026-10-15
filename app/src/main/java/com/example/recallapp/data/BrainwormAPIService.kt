package com.example.recallapp.data
import retrofit2.Response
import retrofit2.http.*

data class RegistryItem(
    val id: Long? = null,
    val product: String,
    val batch: String?
)

interface BrainwormApiService {
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
}