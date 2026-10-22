package com.example.recallapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.recallapp.auth.AuthProvider
import com.example.recallapp.auth.TokenStore
import com.example.recallapp.ui.admin.AdminConsoleScreen
import com.example.recallapp.ui.admin.ManageUsersRoute
import com.example.recallapp.ui.items.MyItemsScreen
import com.example.recallapp.ui.items.RegistryDetailScreen
import com.example.recallapp.ui.items.RegistryViewModel
import com.example.recallapp.ui.items.DetailUiState
import com.example.recallapp.ui.login.LoginRoute
import com.example.recallapp.ui.login.LoginViewModel
import com.example.recallapp.ui.admin.ManageRecallsRoute

/** Keeps route names consistent when registering and opening screens. */
object Routes {
    const val LOGIN = "login"
    const val ITEMS = "items"
    const val SETTINGS = "settings"
    const val ADMIN = "admin"
    const val MANAGE_RECALLS = "manage_recalls"
    const val MANAGE_USERS = "manage_users"
    const val DETAIL = "detail/{itemId}"
    fun detail(itemId: Long) = "detail/$itemId"
}

/**
 * Registers the app's screens and connects their navigation callbacks.
 *
 * Admin tools open through Settings, not through separate bottom tabs.
 * Admin-role enforcement is not implemented in this local-development flow.
 */
@Composable
fun AppNavHost(auth: AuthProvider, tokens: TokenStore) {
    val nav = rememberNavController()
    val context = LocalContext.current

    // Choose the initial destination once.
    // Login and logout callbacks handle subsequent navigation.
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
                            // Settings remains selected while its console
                            // or user-management screen is open.
                            selected = currentRoute == route ||
                                    (
                                            route == Routes.SETTINGS &&
                                                    currentRoute in listOf(
                                                Routes.ADMIN,
                                                Routes.MANAGE_USERS,
                                                Routes.MANAGE_RECALLS
                                            )
                                            ),
                            onClick = {
                                nav.navigate(route) {
                                    // Keep My Items as home and avoid
                                    // accumulating repeated tab destinations.
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
            // Preserves the existing Google sign-in flow.
            composable(Routes.LOGIN) {
                val vm: LoginViewModel = viewModel(
                    factory = LoginViewModel.factory(auth, tokens),
                )

                LoginRoute(
                    vm = vm,
                    onSignedIn = {
                        nav.navigate(Routes.ITEMS) {
                            // Back should not reopen login after success.
                            popUpTo(Routes.LOGIN) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }

            // Displays the home screen after successful login.
//             composable(Routes.ITEMS) {
                //MyItemsScreen(
                    // Registry data and item actions are not connected yet.
//                     items = emptyList(),

//                     // Hide the header shortcut; the console opens from Settings.
//                     // This UI parameter does not change a database role.
//                     isAdmin = false,

//                     loading = false,
//                     loadingMore = false,
//                     error = null,
//                     onOpen = {},
//                     onAdd = {},
//                     onAdmin = {},
//                     onLoadMore = {},
//                     onSignOut = {
//                         tokens.clear()

//                         nav.navigate(Routes.LOGIN) {
//                             // Remove the signed-in screens from history.
//                             popUpTo(Routes.ITEMS) {
//                                 inclusive = true
//                             }
//                             launchSingleTop = true
//                         }
//                     },
//                 )
//             }

            // Settings provides the entry point to the admin console.
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

                    // Temporarily visible to all signed-in users locally.
                    // This will use the real admin flag after account linking.
                    Button(
                        onClick = {
                            nav.navigate(Routes.ADMIN) {
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text("Admin Console")
                    }
                }
            }

            // Displays the existing console with its two action cards.
            composable(Routes.ADMIN) {
                AdminConsoleScreen(
                    onBack = {
                        // Matches the console's "Back to My Items" button.
                        nav.navigate(Routes.ITEMS) {
                            popUpTo(Routes.ITEMS) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onManageUsers = {
                        nav.navigate(Routes.MANAGE_USERS) {
                            launchSingleTop = true
                        }
                    },
                    onManageRecalls = {
//                        // Give feedback until recall management is implemented.
//                        Toast.makeText(
//                            context,
//                            "Recall management is not connected yet.",
//                            Toast.LENGTH_SHORT,
//                        ).show()
                        nav.navigate(Routes.MANAGE_RECALLS) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            // Loads real users through the route's ViewModel and API client.
            composable(Routes.MANAGE_USERS) {
                ManageUsersRoute(
                    // Returns to the console underneath this destination.
                    onBack = { nav.popBackStack() },
                )
            }
            // Opens the read-only recall list; Back returns to the admin console.
            composable(Routes.MANAGE_RECALLS) {
                ManageRecallsRoute(
                    onBack = { nav.popBackStack() },
                )
            }
        // The list stays empty until the registry API is connected in the next issue.
        // Note: Lines 239 to 262 are written by Alex.
        composable(Routes.ITEMS) {
            val vm: RegistryViewModel = viewModel(factory = RegistryViewModel.factory(tokens))
            val items by vm.items.collectAsState()
            val isLoading by vm.isLoading.collectAsState()
            val isLoadingMore by vm.isLoadingMore.collectAsState()
            val error by vm.listError.collectAsState()

            MyItemsScreen(
                items = items,
                isAdmin = false,
                loading = isLoading,
                loadingMore = isLoadingMore,
                error = error,
                onOpen = { itemId -> nav.navigate(Routes.detail(itemId)) },
                onAdd = { nav.navigate(Routes.detail(-1L)) },
                onAdmin = {},
                onLoadMore = { vm.loadMoreItems() },
                onSignOut = {
                    tokens.clear()
                    nav.navigate(Routes.LOGIN) { popUpTo(Routes.ITEMS) { inclusive = true } }
                },
            )
        }

        composable(Routes.DETAIL) { backStackEntry ->
            val itemIdStr = backStackEntry.arguments?.getString("itemId")
            val itemId = itemIdStr?.toLongOrNull() ?: -1L
            val isCreatingNew = itemId == -1L
            val vm: RegistryViewModel = viewModel(factory = RegistryViewModel.factory(tokens))
            val uiState by vm.detailState.collectAsState()

            LaunchedEffect(itemId) {
                if (!isCreatingNew) {
                    vm.loadSingleItem(itemId)
                } else {
                    vm.loadSingleItem(-1L)
                }
            }

            RegistryDetailScreen(
                uiState = if (isCreatingNew && uiState is DetailUiState.Loading) DetailUiState.Success(
                    "",
                    ""
                ) else uiState,
                isCreatingNew = isCreatingNew,
                onSaveClick = { product, batch ->
                    val idToSave = if (isCreatingNew) null else itemId
                    vm.saveItem(idToSave, product, batch) {
                        nav.popBackStack()
                    }
                },
                onDeleteClick = {
                    if (!isCreatingNew) {
                        vm.deleteItem(itemId) {
                            nav.popBackStack()
                        }
                    }
                }
            )
        }
    }
}