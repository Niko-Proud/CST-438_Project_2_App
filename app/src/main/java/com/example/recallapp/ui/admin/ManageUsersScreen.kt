package com.example.recallapp.ui.admin

import androidx.compose.runtime.Composable

//this is a fake user for testing purposes. will eventually be purged.
data class AdminUserUi(
    val id: Long,
    val username: String,
    val isAdmin: Boolean,
)

@Composable
fun ManageUsersScreen(
    users: List<AdminUserUi>,
    currentUserId: Long,
    onBack: () -> Unit,
    onChangeRole: (Long, Boolean) -> Unit,
    onDeleteUser: (Long) -> Unit,
) {
    // Search, user cards, and confirmation dialogs go here.
}

