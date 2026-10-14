package com.example.recallapp.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [TokenStore], which other screens use to send the token to the API. */
class TokenStoreTest {

    @Test
    fun newStore_isSignedOut() {
        val tokens = TokenStore()

        assertFalse(tokens.isSignedIn)
        assertNull(tokens.bearer())
    }

    @Test
    fun save_makesBearerHeaderValue() {
        val tokens = TokenStore()

        tokens.save("abc123")

        assertTrue(tokens.isSignedIn)
        assertEquals("Bearer abc123", tokens.bearer())
    }

    @Test
    fun clear_signsOut() {
        val tokens = TokenStore()
        tokens.save("abc123")

        tokens.clear()

        assertFalse(tokens.isSignedIn)
        assertNull(tokens.idToken)
    }
}
