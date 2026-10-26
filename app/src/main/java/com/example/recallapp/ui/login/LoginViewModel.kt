package com.example.recallapp.ui.login

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recallapp.auth.AuthProvider
import com.example.recallapp.auth.TokenStore
import com.example.recallapp.data.BrainwormApiService
import com.example.recallapp.data.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the login screen shows. */
data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
)

/** Runs Google sign in, links the account on the API, and then saves the ID token. */
class LoginViewModel(
    private val auth: AuthProvider,
    private val tokens: TokenStore,
    private val api: BrainwormApiService,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState(signedIn = tokens.isSignedIn))

    /** Current screen state. */
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    /** Returns the intent that opens Google sign in, or null when sign in is not set up yet. */
    fun startSignIn(): Intent? {
        if (!auth.isConfigured) {
            _state.update { it.copy(error = NOT_CONFIGURED) }
            return null
        }
        _state.update { it.copy(loading = true, error = null) }
        return auth.signInIntent()
    }

    /** Handles the result that comes back from the Google sign in page. */
    fun onAuthResult(data: Intent?) {
        viewModelScope.launch {
            try {
                val idToken = auth.handleResult(data)
                // The backend creates or finds its user record before we enter the app.
                // Saving the token only after this succeeds prevents a half-finished login.
                val response = api.connectGoogleAccount("Bearer $idToken")

                check(response.isSuccessful) {
                    "Could not connect your account (HTTP ${response.code()}). Please try again."
                }
                checkNotNull(response.body()) {
                    "The API returned no account information. Please try again."
                }

                tokens.save(idToken)
                _state.update { it.copy(loading = false, signedIn = true) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Sign in failed. Please try again.") }
            }
        }
    }

    companion object {
        /** Error shown when GOOGLE_CLIENT_ID is missing from local.properties. */
        const val NOT_CONFIGURED = "Google sign in is not set up yet. Add GOOGLE_CLIENT_ID to local.properties."

        /** Factory that passes the sign in provider and token store to the ViewModel. */
        fun factory(
            auth: AuthProvider,
            tokens: TokenStore,
            api: BrainwormApiService = RetrofitClient.instance,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(auth, tokens, api) }
        }
    }
}
