package com.example.recallapp.data

/**
 * Holds one user received from the API.
 * Property names match the JSON: id, username, and admin.
 */
data class UserDto(
    val id: Long,
    val username: String,
    val admin: Boolean,
)
/**
 * Sends only the role field that the PATCH endpoint should update.
 * The property name matches the backend JSON.
 */
data class UpdateUserRoleRequest(
    val admin: Boolean,
)
/**
 * Holds one page from GET /api/v1/users.
 * The pagination fields let us determine whether more users can be loaded.
 */
data class UserPageDto(
    val content: List<UserDto>,
    val number: Int,
    val last: Boolean,
    val totalElements: Long,
)