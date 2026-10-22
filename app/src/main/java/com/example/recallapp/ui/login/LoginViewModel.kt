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
import kotlinx.coroutines.CancellationException
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

/** Completes Google sign-in and links the account before opening the app. */
class LoginViewModel(
    private val auth: AuthProvider,
    private val tokens: TokenStore,
    private val api: BrainwormApiService,
) : ViewModel() {

    private val _state = MutableStateFlow(
        LoginUiState(signedIn = tokens.isSignedIn)
    )

    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    /** Opens Google sign-in and reports configuration or launch errors. */
    fun startSignIn(): Intent? {
        if (_state.value.loading) return null

        if (!auth.isConfigured) {
            _state.update { it.copy(error = NOT_CONFIGURED) }
            return null
        }

        _state.update {
            it.copy(loading = true, error = null, signedIn = false)
        }

        return try {
            auth.signInIntent()
        } catch (error: Exception) {
            _state.update {
                it.copy(
                    loading = false,
                    error = error.message ?: "Could not open Google sign-in.",
                )
            }
            null
        }
    }

    /**
     * Sends Google's ID token to the API to find or create the account.
     * Saves the session only after the backend confirms success.
     */
    fun onAuthResult(data: Intent?) {
        viewModelScope.launch {
            _state.update {
                it.copy(loading = true, error = null, signedIn = false)
            }

            try {
                val idToken = auth.handleResult(data)

                val response = api.connectGoogleAccount(
                    token = "Bearer $idToken",
                )

                if (!response.isSuccessful) {
                    val message = when (response.code()) {
                        401 -> "The API could not verify your Google sign-in."
                        403 -> "The API refused access to your account."
                        else ->
                            "Could not connect your account " +
                                    "(HTTP ${response.code()}). Please try again."
                    }

                    throw IllegalStateException(message)
                }

                checkNotNull(response.body()) {
                    "The API returned no account information. Please try again."
                }

                tokens.save(idToken)

                _state.update {
                    it.copy(
                        loading = false,
                        error = null,
                        signedIn = true,
                    )
                }
            } catch (cancelled: CancellationException) {
                // Cancellation is not a failed Google or network request.
                throw cancelled
            } catch (error: Exception) {
                tokens.clear()

                _state.update {
                    it.copy(
                        loading = false,
                        signedIn = false,
                        error = error.message
                            ?: "Sign in failed. Please try again.",
                    )
                }
            }
        }
    }

    companion object {
        const val NOT_CONFIGURED =
            "Google sign in is not set up yet. " +
                    "Add GOOGLE_CLIENT_ID to local.properties."

        /** Supplies real dependencies; tests can inject a fake API directly. */
        fun factory(
            auth: AuthProvider,
            tokens: TokenStore,
            api: BrainwormApiService = RetrofitClient.instance,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LoginViewModel(auth, tokens, api)
            }
        }
    }
}