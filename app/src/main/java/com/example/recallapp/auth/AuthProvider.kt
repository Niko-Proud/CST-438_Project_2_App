package com.example.recallapp.auth

import android.content.Intent

/** Signs the user in with an identity provider and returns an ID token. */
interface AuthProvider {

    /** True when a client ID has been set in local.properties. */
    val isConfigured: Boolean

    /** Intent that opens the sign in page. */
    fun signInIntent(): Intent

    /** Reads the result of the sign in page and returns the ID token. */
    suspend fun handleResult(data: Intent?): String
}
