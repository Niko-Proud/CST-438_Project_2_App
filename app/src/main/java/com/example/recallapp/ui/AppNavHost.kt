package com.example.recallapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.recallapp.auth.AuthProvider
import com.example.recallapp.auth.TokenStore
import com.example.recallapp.ui.items.MyItemsScreen
import com.example.recallapp.ui.login.LoginRoute
import com.example.recallapp.ui.login.LoginViewModel

/** Keeps route names consistent when registering and opening screens. */
object Routes {
    const val LOGIN = "login"
    const val ITEMS = "items"
    const val SETTINGS = "settings"
}

/**
 * Registers the app's screens and handles navigation between them.
 * Bottom navigation is hidden during login.
 */
@Composable
fun AppNavHost(auth: AuthProvider, tokens: TokenStore) {
    val nav = rememberNavController()

    // Select the initial screen once. Login/logout callbacks handle
    // navigation afterward without changing the graph's start destination.
    val start = remember {
        if (tokens.isSignedIn) Routes.ITEMS else Routes.LOGIN
    }

    val entry by nav.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route ?: start

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute != Routes.LOGIN) {
                NavigationBar {
                    listOf(
                        Routes.ITEMS to "My Items",
                        Routes.SETTINGS to "Settings",
                    ).forEach { (route, label) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                nav.navigate(route) {
                                    // Keep My Items as home without stacking
                                    // repeated copies of the tab screens.
                                    popUpTo(Routes.ITEMS) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            },
                            icon = {
                                Text(
                                    if (route == Routes.ITEMS) "⌂" else "⚙",
                                )
                            },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            // Registers login as a destination in this navigation graph.
            composable(Routes.LOGIN) {
                val vm: LoginViewModel = viewModel(
                    factory = LoginViewModel.factory(auth, tokens),
                )

                LoginRoute(
                    vm = vm,
                    onSignedIn = {
                        nav.navigate(Routes.ITEMS) {
                            // Prevent Back from reopening login after success.
                            popUpTo(Routes.LOGIN) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }

            // Registers the home screen used after successful login.
            composable(Routes.ITEMS) {
                MyItemsScreen(
                    // Registry data and item actions are not connected yet.
                    items = emptyList(),
                    isAdmin = false,
                    loading = false,
                    loadingMore = false,
                    error = null,
                    onOpen = {},
                    onAdd = {},
                    onAdmin = {},
                    onLoadMore = {},
                    onSignOut = {
                        tokens.clear()

                        nav.navigate(Routes.LOGIN) {
                            // Remove home from history when signing out.
                            popUpTo(Routes.ITEMS) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }

            // Registers Settings beside Login and Items, inside the same graph.
            composable(Routes.SETTINGS) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(20.dp),
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                }
            }
        }
    }
}