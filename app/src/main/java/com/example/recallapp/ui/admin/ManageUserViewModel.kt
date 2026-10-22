package com.example.recallapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recallapp.auth.TokenStore
import com.example.recallapp.data.BrainwormApiService
import com.example.recallapp.data.UpdateUserRoleRequest
import com.example.recallapp.data.UserDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds everything the Manage Users screen needs to display.
 *
 * error represents a list-loading failure.
 * actionError represents a failed role change or deletion.
 */
data class ManageUsersUiState(
    val users: List<UserDto> = emptyList(),
    val query: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val busyUserIds: Set<Long> = emptySet(),
    val actionError: String? = null,
) {
    /** Searches all loaded users without changing the original list. */
    val visibleUsers: List<UserDto>
        get() {
            val search = query.trim()

            return users.filter {
                it.username.contains(search, ignoreCase = true)
            }
        }
}

/**
 * Owns user-list state and coordinates authenticated API requests.
 *
 * Receives the same TokenStore used by login.
 * The backend determines whether the caller has admin permission.
 */
class ManageUsersViewModel(
    private val api: BrainwormApiService,
    private val tokens: TokenStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ManageUsersUiState())
    val state: StateFlow<ManageUsersUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadUsers()
    }

    /** Gets the saved credentials for a protected API request. */
    private fun authorizationHeader(): String {
        return tokens.bearer()
            ?: throw IllegalStateException(
                "You are signed out. Please sign in again."
            )
    }

    /** Updates the username filter without requesting data again. */
    fun updateQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    /** Clears an action error after the screen dismisses its message. */
    fun clearActionError() {
        _state.update { it.copy(actionError = null) }
    }

    /**
     * Loads every page before publishing a complete list.
     * Also serves as Retry.
     *
     * Loading and mutations do not overlap, preventing an older list
     * response from overwriting a successful role change or deletion.
     */
    fun loadUsers() {
        if (loadJob?.isActive == true) return
        if (_state.value.busyUserIds.isNotEmpty()) return

        // Set immediately so actions cannot start before the coroutine runs.
        _state.update {
            it.copy(
                loading = true,
                error = null,
                actionError = null,
            )
        }

        loadJob = viewModelScope.launch {
            try {
                val loadedUsers = mutableListOf<UserDto>()
                var requestedPage = 0

                while (true) {
                    val response = api.getUsers(
                        token = authorizationHeader(),
                        page = requestedPage,
                        size = 20,
                    )

                    if (!response.isSuccessful) {
                        throw IllegalStateException(
                            "Could not load users (HTTP ${response.code()})."
                        )
                    }

                    val page = response.body()
                        ?: throw IllegalStateException(
                            "The server returned no user data."
                        )

                    check(page.number == requestedPage) {
                        "The server returned an unexpected page."
                    }

                    loadedUsers.addAll(page.content)

                    if (page.last) break

                    check(page.content.isNotEmpty()) {
                        "The server returned an empty unfinished page."
                    }

                    requestedPage++
                }

                _state.update {
                    it.copy(
                        users = loadedUsers.distinctBy { user -> user.id },
                        error = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                // Cancellation must not appear as a network failure.
                throw cancelled
            } catch (error: Exception) {
                // Keep any previously loaded users and the search text.
                _state.update {
                    it.copy(
                        error = error.message ?: "Could not load users.",
                    )
                }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    /**
     * Promotes or demotes a user after the screen obtains confirmation.
     * Uses the server's returned record instead of assuming success.
     */
    fun changeRole(userId: Long, makeAdmin: Boolean) {
        if (!beginAction(userId)) return

        viewModelScope.launch {
            try {
                val response = api.updateUserRole(
                    token = authorizationHeader(),
                    userId = userId,
                    request = UpdateUserRoleRequest(admin = makeAdmin),
                )

                if (!response.isSuccessful) {
                    throw IllegalStateException(
                        "Could not change role (HTTP ${response.code()})."
                    )
                }

                val updatedUser = response.body()
                    ?: throw IllegalStateException(
                        "The server returned no updated user. Reload to verify."
                    )

                check(updatedUser.id == userId) {
                    "The server returned a different user. Reload to verify."
                }

                _state.update { current ->
                    current.copy(
                        users = current.users.map { user ->
                            if (user.id == userId) updatedUser else user
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        actionError = error.message
                            ?: "Could not change the user's role.",
                    )
                }
            } finally {
                finishAction(userId)
            }
        }
    }

    /**
     * Deletes a user after confirmation.
     * Removes the displayed record only after a successful API response.
     */
    fun deleteUser(userId: Long) {
        if (!beginAction(userId)) return

        viewModelScope.launch {
            try {
                val response = api.deleteUser(
                    token = authorizationHeader(),
                    userId = userId,
                )

                if (!response.isSuccessful) {
                    throw IllegalStateException(
                        "Could not delete user (HTTP ${response.code()})."
                    )
                }

                _state.update { current ->
                    current.copy(
                        users = current.users.filterNot {
                            it.id == userId
                        },
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        actionError = error.message
                            ?: "Could not delete the user.",
                    )
                }
            } finally {
                finishAction(userId)
            }
        }
    }

    /**
     * Reserves a user for one operation at a time.
     * Called synchronously from UI callbacks on the main thread.
     */
    private fun beginAction(userId: Long): Boolean {
        val current = _state.value

        if (current.loading) return false
        if (userId in current.busyUserIds) return false
        if (current.users.none { it.id == userId }) return false

        _state.update {
            it.copy(
                busyUserIds = it.busyUserIds + userId,
                actionError = null,
            )
        }

        return true
    }

    /** Re-enables a user's controls after success, failure, or cancellation. */
    private fun finishAction(userId: Long) {
        _state.update {
            it.copy(
                busyUserIds = it.busyUserIds - userId,
            )
        }
    }
}