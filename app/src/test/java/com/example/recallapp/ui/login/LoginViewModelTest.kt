package com.example.recallapp.ui.login

import android.content.Intent
import com.example.recallapp.auth.AuthProvider
import com.example.recallapp.auth.TokenStore
import com.example.recallapp.data.BrainwormApiService
import com.example.recallapp.data.FakeApiService
import com.example.recallapp.data.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import retrofit2.Response

/** Swaps Dispatchers.Main for a test dispatcher so viewModelScope runs in unit tests. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(UnconfinedTestDispatcher())
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Fake sign in that returns a fixed token, or fails when the token is null. No browser is opened. */
class FakeAuthProvider(
    override val isConfigured: Boolean = true,
    private val token: String? = "test-id-token",
) : AuthProvider {
    override fun signInIntent(): Intent = throw UnsupportedOperationException("Not used in unit tests")
    override suspend fun handleResult(data: Intent?): String =
        token ?: throw IllegalStateException("Sign in was cancelled.")
}

private class LoginApi : BrainwormApiService by FakeApiService() {
    override suspend fun connectGoogleAccount(token: String): Response<UserDto> =
        Response.success(UserDto(1L, "test-user", false))
}

/** Unit tests for the login screen state and token handling. */
class LoginViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val tokens = TokenStore()

    @Test
    fun startSignIn_whenClientIdMissing_showsErrorAndDoesNotOpenGoogle() {
        val vm = LoginViewModel(FakeAuthProvider(isConfigured = false), tokens, LoginApi())

        val intent = vm.startSignIn()

        assertNull(intent)
        assertEquals(LoginViewModel.NOT_CONFIGURED, vm.state.value.error)
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun successfulSignIn_savesTokenAndMarksSignedIn() {
        val vm = LoginViewModel(FakeAuthProvider(token = "abc123"), tokens, LoginApi())

        vm.onAuthResult(null)

        assertTrue(vm.state.value.signedIn)
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.error)
        assertEquals("abc123", tokens.idToken)
    }

    @Test
    fun cancelledSignIn_showsErrorAndStaysSignedOut() {
        val vm = LoginViewModel(FakeAuthProvider(token = null), tokens, LoginApi())

        vm.onAuthResult(null)

        assertFalse(vm.state.value.signedIn)
        assertEquals("Sign in was cancelled.", vm.state.value.error)
        assertNull(tokens.idToken)
    }

    @Test
    fun savedToken_startsSignedIn() {
        tokens.save("existing-token")

        val vm = LoginViewModel(FakeAuthProvider(), tokens, LoginApi())

        assertTrue(vm.state.value.signedIn)
    }
}
