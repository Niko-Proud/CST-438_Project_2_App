package com.example.recallapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.recallapp.BuildConfig
import com.example.recallapp.auth.AuthProvider
import com.example.recallapp.auth.TokenStore
import com.example.recallapp.data.FakeApiService
import com.example.recallapp.ui.admin.AdminConsoleScreen
import com.example.recallapp.ui.admin.ManageRecallsRoute
import com.example.recallapp.ui.admin.ManageUsersRoute
import com.example.recallapp.ui.items.DetailUiState
import com.example.recallapp.ui.items.MyItemsScreen
import com.example.recallapp.ui.items.RegistryDetailScreen
import com.example.recallapp.ui.items.RegistryViewModel
import com.example.recallapp.ui.login.LoginRoute
import com.example.recallapp.ui.login.LoginViewModel

/** Route names used by the navigation graph. */
object Routes {
    const val LOGIN = "login"
    const val ITEMS = "items"
    const val ADMIN = "admin"
    const val MANAGE_USERS = "manage_users"
    const val MANAGE_RECALLS = "manage_recalls"
    const val DETAIL = "detail/{itemId}"
    fun detail(itemId: Long) = "detail/$itemId"
}

/** The screens of the app and how the user moves between them. */
@Composable
fun AppNavHost(auth: AuthProvider, tokens: TokenStore) {
    val nav = rememberNavController()
    // Skip login is only a debug convenience; keeping this flag separate means a real
    // Google sign-in still uses the live API instead of accidentally using fake data.
    var demoMode by remember { mutableStateOf(false) }
    val start = if (tokens.isSignedIn) Routes.ITEMS else Routes.LOGIN

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.LOGIN) {
            val vm: LoginViewModel = viewModel(factory = LoginViewModel.factory(auth, tokens))
            Box(Modifier.fillMaxSize()) {
                LoginRoute(
                    vm = vm,
                    onSignedIn = {
                        demoMode = false
                        nav.navigate(Routes.ITEMS) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    },
                )
                // This keeps local UI work possible when Google OAuth is not configured.
                if (BuildConfig.DEBUG) {
                    TextButton(
                        onClick = {
                            demoMode = true
                            nav.navigate(Routes.ITEMS) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),
                    ) { Text("Skip login") }
                }
            }
        }

        composable(Routes.ITEMS) {
            val vm: RegistryViewModel = viewModel(
                factory = RegistryViewModel.factory(
                    tokens = tokens,
                    demoApi = if (demoMode) FakeApiService() else null,
                ),
            )
            val items by vm.items.collectAsState()
            val isLoading by vm.isLoading.collectAsState()
            val isLoadingMore by vm.isLoadingMore.collectAsState()
            val error by vm.listError.collectAsState()
            val signedOut by vm.signedOut.collectAsState()

            // The server said the sign-in expired (401), so go back to the login screen.
            LaunchedEffect(signedOut) {
                if (signedOut) nav.navigate(Routes.LOGIN) { popUpTo(Routes.ITEMS) { inclusive = true } }
            }

            MyItemsScreen(
                items = items,
                // Admin access remains visible in debug builds while the backend role check is finalized.
                isAdmin = BuildConfig.DEBUG,
                loading = isLoading,
                loadingMore = isLoadingMore,
                error = error,
                onOpen = { itemId -> nav.navigate(Routes.detail(itemId)) },
                onAdd = { nav.navigate(Routes.detail(-1L)) },
                onAdmin = { nav.navigate(Routes.ADMIN) },
                onLoadMore = { vm.loadMoreItems() },
                onDelete = { itemId -> vm.deleteItem(itemId) {} },
                onDeleteAccount = {
                    vm.deleteAccount {
                        nav.navigate(Routes.LOGIN) { popUpTo(Routes.ITEMS) { inclusive = true } }
                    }
                },
                onSignOut = {
                    tokens.clear()
                    nav.navigate(Routes.LOGIN) { popUpTo(Routes.ITEMS) { inclusive = true } }
                },
            )
        }

        // Uses the same ViewModel as the home screen, so the list refreshes after a save or delete.
        composable(Routes.DETAIL) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")?.toLongOrNull() ?: -1L
            val isCreatingNew = itemId == -1L
            val homeEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.ITEMS) }
            val vm: RegistryViewModel = viewModel(
                viewModelStoreOwner = homeEntry,
                factory = RegistryViewModel.factory(
                    tokens = tokens,
                    demoApi = if (demoMode) FakeApiService() else null,
                ),
            )
            val uiState by vm.detailState.collectAsState()
            val signedOut by vm.signedOut.collectAsState()

            LaunchedEffect(signedOut) {
                if (signedOut) nav.navigate(Routes.LOGIN) { popUpTo(Routes.ITEMS) { inclusive = true } }
            }

            LaunchedEffect(itemId) {
                if (!isCreatingNew) vm.loadSingleItem(itemId)
            }

            RegistryDetailScreen(
                uiState = if (isCreatingNew && uiState !is DetailUiState.Error) DetailUiState.Success("", "") else uiState,
                isCreatingNew = isCreatingNew,
                onSaveClick = { product, batch ->
                    vm.saveItem(if (isCreatingNew) null else itemId, product, batch) { nav.popBackStack() }
                },
                onDeleteClick = {
                    if (!isCreatingNew) vm.deleteItem(itemId) { nav.popBackStack() }
                },
                onBack = { nav.popBackStack() },
            )
        }

        composable(Routes.ADMIN) {
            AdminConsoleScreen(
                onBack = { nav.popBackStack() },
                onManageRecalls = { nav.navigate(Routes.MANAGE_RECALLS) { launchSingleTop = true } },
                onManageUsers = { nav.navigate(Routes.MANAGE_USERS) { launchSingleTop = true } },
            )
        }

        // Victoria's admin screens. Back returns to the admin console.
        composable(Routes.MANAGE_USERS) {
            ManageUsersRoute(onBack = { nav.popBackStack() })
        }

        composable(Routes.MANAGE_RECALLS) {
            ManageRecallsRoute(onBack = { nav.popBackStack() })
        }
    }
}
