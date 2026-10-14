package com.example.recallapp

import android.app.Application
import com.example.recallapp.auth.GoogleAuthManager
import com.example.recallapp.auth.TokenStore

/** Creates the objects that live as long as the app: Google sign in and the saved token. */
class RecallApplication : Application() {

    /** Google sign in. */
    lateinit var auth: GoogleAuthManager
        private set

    /** Saved ID token, shared by every screen. */
    val tokens = TokenStore()

    override fun onCreate() {
        super.onCreate()
        auth = GoogleAuthManager(this)
    }
}
