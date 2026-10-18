package com.example.recallapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.recallapp.auth.AuthProvider
import com.example.recallapp.auth.TokenStore
import com.example.recallapp.ui.items.MyItemsScreen
import com.example.recallapp.ui.items.RegistryDetailScreen
import com.example.recallapp.ui.items.RegistryViewModel
import com.example.recallapp.ui.items.DetailUiState
import com.example.recallapp.ui.login.LoginRoute
import com.example.recallapp.ui.login.LoginViewModel

/** Route names used by the navigation graph. */
object Routes {
    const val LOGIN = "login"
    const val ITEMS = "items"
    const val DETAIL = "detail/{itemId}"
    fun detail(itemId: Long) = "detail/$itemId"
}

/** The screens of the app and how the user moves between them. */
@Composable
fun AppNavHost(auth: AuthProvider, tokens: TokenStore) {
    val nav = rememberNavController()
    val start = if (tokens.isSignedIn) Routes.ITEMS else Routes.LOGIN

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.LOGIN) {
            val vm: LoginViewModel = viewModel(factory = LoginViewModel.factory(auth, tokens))
            LoginRoute(
                vm = vm,
                onSignedIn = {
                    nav.navigate(Routes.ITEMS) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
            )
        }

        // The list stays empty until the registry API is connected in the next issue.
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
