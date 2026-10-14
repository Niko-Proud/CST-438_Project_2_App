package com.example.recallapp.ui.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Connects the login screen UI to [LoginViewModel] and the Google sign in page. */
@Composable
fun LoginRoute(vm: LoginViewModel, onSignedIn: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        vm.onAuthResult(result.data)
    }

    LaunchedEffect(state.signedIn) {
        if (state.signedIn) onSignedIn()
    }

    LoginScreen(
        loading = state.loading,
        error = state.error,
        onSignInClick = { vm.startSignIn()?.let { launcher.launch(it) } },
    )
}
