package com.example.recallapp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recallapp.ui.theme.Lab
import com.example.recallapp.ui.theme.RecallTheme

/**
 * Admin landing screen.
 *
 * Navigation is handled by the caller through the button callbacks.
 * This screen does not fetch data or grant admin permissions.
 */
@Composable
fun AdminConsoleScreen(
    onBack: () -> Unit,
    onManageRecalls: () -> Unit,
    onManageUsers: () -> Unit,
) {
    //ui "syntactical sugar"
    Scaffold(
        containerColor = Lab.Bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AdminHeader(onBack = onBack)
            }

            item {
                Text(
                    text = "ADMIN TOOLS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Lab.TealDark,
                    modifier = Modifier.padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 6.dp,
                    ),
                )
            }

            /**This is for recalls and user management. below(aka in the preview
             * function at the bottom of the file) there's a space to put the
             * button's actions.
             */
            item {
                AdminActionCard(
                    title = "Manage recalls",
                    description = "View recall notices and add new food recalls.",
                    actionLabel = "Open recalls",
                    onClick = onManageRecalls,
                )
            }

            //This is the Manage Users card. these are just the UI bits though
            item {
                AdminActionCard(
                    title = "Manage users",
                    description = "View registered users and manage admin access.",
                    actionLabel = "Open users",
                    onClick = onManageUsers,
                )
            }
        }
    }
}

@Composable
private fun AdminHeader(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Lab.Teal)
            .statusBarsPadding()
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 28.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = "Back to My Items",
                style = MaterialTheme.typography.labelLarge,
                color = Lab.Mint,
            )
        }

        Text(
            text = "BRAINWORM",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )

        Text(
            text = "Admin Console",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
        )

        Text(
            text = "Manage food recalls and user access.",
            style = MaterialTheme.typography.bodyMedium,
            color = Lab.Mint,
        )
    }
}

/**This is the definition of the action card. Its purpose is to easily
 * change the descriptions and button action while the general template
 * of the card itself stays the same. It was previously called on.
 */
@Composable
private fun AdminActionCard(
    title: String,
    description: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(
                color = Lab.Surface,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Lab.Ink,
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Lab.Muted,
        )

        Button(
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(
                containerColor = Lab.Teal,
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun AdminConsolePreview() {
    RecallTheme {
        AdminConsoleScreen(
            //Right now the on manage recalls and on manage users buttons do nothing.
            onBack = {},
            onManageRecalls = {},
            onManageUsers = {},
        )
    }
}