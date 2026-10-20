package com.example.recallapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.recallapp.ui.theme.Lab

/**
 * Menu button with "Log out" and "Delete account".
 * Users see it on the home screen, admins see it on the admin page.
 */
@Composable
fun AccountMenu(onSignOut: () -> Unit, onDeleteAccount: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Lab.Ink)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Log out") },
                onClick = { menuOpen = false; onSignOut() },
            )
            DropdownMenuItem(
                text = { Text("Delete account", color = Lab.Hazard) },
                onClick = { menuOpen = false; confirmDelete = true },
            )
        }
    }

    // Deleting the account removes all the user's data, so ask once before doing it.
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete your account?") },
            text = { Text("All your foods will be deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDeleteAccount() }) {
                    Text("Delete", color = Lab.Hazard)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel", color = Lab.Ink)
                }
            },
        )
    }
}
