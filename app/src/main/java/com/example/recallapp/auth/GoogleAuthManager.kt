package com.example.recallapp.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.recallapp.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google sign in with the Authorization Code flow and PKCE.
 * AppAuth creates the PKCE verifier and challenge and opens the sign in page in a Custom Tab.
 * The result is the Google ID token, which the API checks as a Resource Server.
 */
class GoogleAuthManager(context: Context) : AuthProvider {

    private val service = AuthorizationService(context)

    private val config = AuthorizationServiceConfiguration(
        Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),
        Uri.parse("https://oauth2.googleapis.com/token"),
    )

    override val isConfigured: Boolean = BuildConfig.GOOGLE_CLIENT_ID.isNotBlank()
//fuck
    override fun signInIntent(): Intent {
        val request = AuthorizationRequest.Builder(
            config,
            BuildConfig.GOOGLE_CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse("${BuildConfig.GOOGLE_REDIRECT_SCHEME}:/oauth2redirect"),
        ).setScopes("openid", "email", "profile").build()
        return service.getAuthorizationRequestIntent(request)
    }

    override suspend fun handleResult(data: Intent?): String {
        if (data == null) throw IllegalStateException("Sign in was cancelled.")
        AuthorizationException.fromIntent(data)?.let {
            throw IllegalStateException(it.errorDescription ?: "Sign in failed.")
        }
        val response = AuthorizationResponse.fromIntent(data)
            ?: throw IllegalStateException("Sign in was cancelled.")

        return suspendCancellableCoroutine { cont ->
            service.performTokenRequest(response.createTokenExchangeRequest()) { tokens, ex ->
                val idToken = tokens?.idToken
                when {
                    idToken != null -> cont.resume(idToken)
                    ex != null -> cont.resumeWithException(
                        IllegalStateException(ex.errorDescription ?: "Sign in failed."),
                    )
                    else -> cont.resumeWithException(IllegalStateException("Google did not return an ID token."))
                }
            }
        }
    }
}
