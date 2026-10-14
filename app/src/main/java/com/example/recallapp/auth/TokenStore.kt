package com.example.recallapp.auth

/** Holds the Google ID token of the signed in user while the app is running. */
class TokenStore {

    /** The current ID token, or null when signed out. */
    var idToken: String? = null
        private set

    /** True when a token is saved. */
    val isSignedIn: Boolean
        get() = idToken != null

    /** Saves the token after a successful sign in. */
    fun save(token: String) {
        idToken = token
    }

    /** Removes the token when the user signs out. */
    fun clear() {
        idToken = null
    }

    /** Value for the Authorization header, or null when signed out. */
    fun bearer(): String? = idToken?.let { "Bearer $it" }
}
