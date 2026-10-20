package com.example.recallapp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recallapp.data.RetrofitClient
import com.example.recallapp.data.UserDto
import com.example.recallapp.ui.theme.Lab

/**
 * Connects the screen to the real API through its ViewModel.
 * Keeps networking and state ownership outside the layout.
 */
@Composable
fun ManageUsersRoute(onBack: () -> Unit) {
    val vm: ManageUsersViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ManageUsersViewModel(RetrofitClient.instance)
            }
        },
    )

    val state by vm.state.collectAsStateWithLifecycle()

    ManageUsersScreen(
        state = state,
        onBack = onBack,
        onQueryChange = vm::updateQuery,
        onRetry = vm::loadUsers,
    )
}

/**
 * Displays the supplied user state and reports interactions through callbacks.
 * This function does not make network requests or modify user records.
 */
@Composable
fun ManageUsersScreen(
    state: ManageUsersUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
) {
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
                    text = "Find users and view their roles.",
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

                        Text(
                            text = state.error,
                            color = Lab.Ink,
                        )

                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
            }

            else -> {
                // Compute the filtered list once for this rendered state.
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
                        UserCard(user)
                    }
                }
            }
        }
    }
}

/** Displays a user's identity and current role without exposing edit actions. */
@Composable
private fun UserCard(user: UserDto) {
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
                .background(Lab.Mint, RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}