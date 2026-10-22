package com.example.recallapp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recallapp.data.RetrofitClient
import com.example.recallapp.data.UserDto
import com.example.recallapp.ui.theme.Lab
import com.example.recallapp.auth.TokenStore

/**
 * Connects the screen to the API using the existing login session.
 * The ViewModel handles requests and updates the displayed user list.
 */
@Composable
fun ManageUsersRoute(
    tokens: TokenStore,
    onBack: () -> Unit,
) {
    val vm: ManageUsersViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ManageUsersViewModel(
                    api = RetrofitClient.instance,
                    tokens = tokens,
                )
            }
        },
    )

    val state by vm.state.collectAsStateWithLifecycle()

    ManageUsersScreen(
        state = state,
        onBack = onBack,
        onQueryChange = vm::updateQuery,
        onRetry = vm::loadUsers,
        onChangeRole = vm::changeRole,
        onDeleteUser = vm::deleteUser,
        onDismissActionError = vm::clearActionError,
    )
}
/**
 * Displays users and confirms potentially destructive actions.
 * Opening a dialog makes no API request; confirming invokes the callback.
 */
@Composable
fun ManageUsersScreen(
    state: ManageUsersUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onChangeRole: (Long, Boolean) -> Unit,
    onDeleteUser: (Long) -> Unit,
    onDismissActionError: () -> Unit,
) {
    var roleTarget by remember { mutableStateOf<UserDto?>(null) }
    var deleteTarget by remember { mutableStateOf<UserDto?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Lab.Bg)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Lab.Teal)
                    .statusBarsPadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onBack) {
                    Text("Back", color = Lab.Mint)
                }

                Text(
                    text = "Manage Users",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )

                Text(
                    text = "Find users and manage their roles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Lab.Mint,
                )
            }
        }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text("Search usernames") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        when {
            state.loading -> {
                item {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(color = Lab.Teal)
                        Text("Loading users…", color = Lab.Muted)
                    }
                }
            }

            state.error != null -> {
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .background(Lab.Surface, RoundedCornerShape(4.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Couldn't load users",
                            style = MaterialTheme.typography.titleMedium,
                            color = Lab.Hazard,
                        )

                        Text(state.error, color = Lab.Ink)

                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
            }

            else -> {
                val visibleUsers = state.visibleUsers

                item {
                    Text(
                        text = "${visibleUsers.size} of ${state.users.size} users",
                        style = MaterialTheme.typography.labelMedium,
                        color = Lab.TealDark,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }

                if (visibleUsers.isEmpty()) {
                    item {
                        Text(
                            text = if (state.users.isEmpty()) {
                                "No users in the database yet."
                            } else {
                                "No usernames match your search."
                            },
                            color = Lab.Muted,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                } else {
                    items(
                        items = visibleUsers,
                        key = { user -> user.id },
                    ) { user ->
                        UserCard(
                            user = user,
                            busy = user.id in state.busyUserIds,
                            onRoleClick = { roleTarget = user },
                            onDeleteClick = { deleteTarget = user },
                        )
                    }
                }
            }
        }
    }

    // Keep the intended change fixed to what the confirmation describes.
    roleTarget?.let { user ->
        val makeAdmin = !user.admin

        AlertDialog(
            onDismissRequest = { roleTarget = null },
            title = {
                Text(if (makeAdmin) "Make admin?" else "Make normie?")
            },
            text = {
                val roleName = if (makeAdmin) "an admin" else "a normie"
                Text("Change ${user.username} to $roleName?")
            },
            confirmButton = {
                TextButton(
                    enabled = !state.loading &&
                            user.id !in state.busyUserIds &&
                            state.users.any { it.id == user.id },
                    onClick = {
                        roleTarget = null
                        onChangeRole(user.id, makeAdmin)
                    },
                    modifier = Modifier.testTag("confirm-role"),
                ) {
                    Text("Confirm role change")
                }
            },
            dismissButton = {
                TextButton(onClick = { roleTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Deletion only happens after the user confirms this dialog.
    deleteTarget?.let { user ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${user.username}?") },
            text = {
                Text(
                    "This permanently deletes the user and their registry " +
                            "items. Associated recall records may also be deleted " +
                            "under the current backend mappings.",
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !state.loading &&
                            user.id !in state.busyUserIds &&
                            state.users.any { it.id == user.id },
                    onClick = {
                        deleteTarget = null
                        onDeleteUser(user.id)
                    },
                    modifier = Modifier.testTag("confirm-delete"),
                ) {
                    Text("Delete permanently", color = Lab.Hazard)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // A dialog makes request failures visible even when the list is scrolled.
    if (roleTarget == null && deleteTarget == null) {
        state.actionError?.let { message ->
            AlertDialog(
                onDismissRequest = onDismissActionError,
                title = { Text("Action failed") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = onDismissActionError) {
                        Text("Dismiss")
                    }
                },
            )
        }
    }
}

/**
 * Displays a user and opens confirmation dialogs through callbacks.
 * Both buttons are disabled while a request for this user is running.
 */
@Composable
private fun UserCard(
    user: UserDto,
    busy: Boolean,
    onRoleClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(Lab.Surface, RoundedCornerShape(4.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = user.username,
            style = MaterialTheme.typography.titleMedium,
            color = Lab.Ink,
        )

        Text(
            text = "User ID: ${user.id}",
            style = MaterialTheme.typography.bodySmall,
            color = Lab.Muted,
        )

        Text(
            text = if (user.admin) "Admin" else "Normie",
            style = MaterialTheme.typography.labelMedium,
            color = Lab.TealDark,
            modifier = Modifier
                .testTag("role-${user.id}")
                .background(Lab.Mint, RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )

        Button(
            onClick = onRoleClick,
            enabled = !busy,
            colors = ButtonDefaults.buttonColors(
                containerColor = Lab.Teal,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("change-role-${user.id}"),
        ) {
            Text(if (user.admin) "Make normie" else "Make admin")
        }

        OutlinedButton(
            onClick = onDeleteClick,
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("delete-user-${user.id}"),
        ) {
            Text("Delete user")
        }

        if (busy) {
            Text(
                text = "Saving…",
                style = MaterialTheme.typography.bodySmall,
                color = Lab.Muted,
            )
        }
    }
}