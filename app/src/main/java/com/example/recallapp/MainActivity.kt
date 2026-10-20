package com.example.recallapp

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.recallapp.ui.AppNavHost
import com.example.recallapp.ui.admin.AdminConsoleScreen
import com.example.recallapp.ui.items.MyItemsScreen
import com.example.recallapp.ui.login.LoginScreen
import com.example.recallapp.ui.theme.Lab
import com.example.recallapp.ui.theme.RecallTheme
//import com.example.recallapp.ui.AppNavHost
import com.example.recallapp.ui.theme.RecallTheme

/** Single activity that hosts every Compose screen. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //val app = application as RecallApplication
        //setContent { RecallTheme { AppNavHost(auth = app.auth, tokens = app.tokens) } }
        
        val allowDemo =
            (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        setContent {
            RecallTheme {
                AppRoot(allowDemo = allowDemo)
            }
        }
    }
}

@Composable
private fun AppRoot(allowDemo: Boolean) {
    // Demo navigation state only. This is NOT an authenticated session.
    var demoOpen by rememberSaveable { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    if (allowDemo && demoOpen) {
        AppShell(
            isAdmin = true, // Sample role for this local UI demo only.
            onExit = { demoOpen = false },
        )
    } else {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                LoginScreen(
                    loading = false,
                    error = loginError,
                    onSignInClick = {
                        loginError = "Google sign-in is not connected yet."
                    },
                )
            }

            if (allowDemo) {
                TextButton(
                    onClick = {
                        loginError = null
                        demoOpen = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                ) {
                    Text("Explore demo — no login")
                }
            }
        }
        //val app = application as RecallApplication
        //setContent { RecallTheme { AppNavHost(auth = app.auth, tokens = app.tokens) } }
    }
}

@Composable
private fun AppShell(
    isAdmin: Boolean,
    onExit: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf("items") }
    var adminOpen by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    fun showNotConnected(feature: String) {
        Toast.makeText(
            context,
            "$feature is not connected yet.",
            Toast.LENGTH_SHORT,
        ).show()
    }

    // Also handles the Android system Back button.
    BackHandler {
        when {
            adminOpen -> {
                adminOpen = false
                selectedTab = "items"
            }
            selectedTab != "items" -> selectedTab = "items"
            else -> onExit()
        }
    }

    Scaffold(
        containerColor = Lab.Bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!adminOpen) {
                NavigationBar(containerColor = Lab.Surface) {
                    NavigationBarItem(
                        selected = selectedTab == "items",
                        onClick = { selectedTab = "items" },
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                            )
                        },
                        label = { Text("My Items") },
                    )

                    NavigationBarItem(
                        selected = selectedTab == "settings",
                        onClick = { selectedTab = "settings" },
                        icon = {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                            )
                        },
                        label = { Text("Settings") },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            when {
                adminOpen && isAdmin -> {
                    AdminConsoleScreen(
                        onBack = {
                            adminOpen = false
                            selectedTab = "items"
                        },
                        onManageRecalls = {
                            showNotConnected("Recall management")
                        },
                        onManageUsers = {
                            showNotConnected("User management")
                        },
                    )
                }

                selectedTab == "settings" -> {
                    SettingsScreen(
                        isAdmin = isAdmin,
                        onAdmin = { adminOpen = true },
                        onExit = onExit,
                    )
                }

                else -> {
                    MyItemsScreen(
                        items = emptyList(),
                        isAdmin = isAdmin,
                        loading = false,
                        loadingMore = false,
                        error = null,
                        onOpen = { showNotConnected("Item details") },
                        onAdd = { showNotConnected("Adding items") },
                        onAdmin = { adminOpen = true },
                        onLoadMore = {},
                        onSignOut = onExit,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    isAdmin: Boolean,
    onAdmin: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = Lab.TealDark,
        )

        if (isAdmin) {
            Button(
                onClick = onAdmin,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Admin Console")
            }
        }

        //I don't know who is taking care of OAuth2 or whatever but right now i dont wanna touch that shit with a 10 foot pole.
        Text(
            //temporary demo mode for debugging purposes.
            text = "Demo mode — no Google account is signed in.",
            style = MaterialTheme.typography.bodyMedium,
            color = Lab.Muted,
        )

        OutlinedButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Exit demo")
        }
    }
}