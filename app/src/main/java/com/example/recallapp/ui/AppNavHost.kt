package com.example.recallapp.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.recallapp.auth.AuthProvider
import com.example.recallapp.auth.TokenStore
import com.example.recallapp.ui.items.MyItemsScreen
import com.example.recallapp.ui.login.LoginRoute
import com.example.recallapp.ui.login.LoginViewModel

/** Route names used by the navigation graph. */
object Routes {
    const val LOGIN = "login"
    const val ITEMS = "items"
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
            MyItemsScreen(
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
                    nav.navigate(Routes.LOGIN) { popUpTo(Routes.ITEMS) { inclusive = true } }
                },
            )
        }
    }
}
