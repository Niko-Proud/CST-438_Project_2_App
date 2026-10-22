package com.example.recallapp.ui.items

//import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.recallapp.ui.theme.RecallTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Tests admin-button visibility and interaction without a real API. */
class MyItemsAdminAccessTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Normies must not see an entry point to the admin console. */
    @Test
    fun normieDoesNotSeeAdminButton() {
        showScreen(isAdmin = false)

        composeRule.onNodeWithText("Admin").assertDoesNotExist()
    }

    /** Admins see the button, and tapping it invokes navigation once. */
    @Test
    fun adminCanOpenConsole() {
        var adminClicks = 0

        showScreen(
            isAdmin = true,
            onAdmin = { adminClicks++ },
        )

        composeRule.onNodeWithText("Admin")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, adminClicks)
        }
    }

    /** Controls stay hidden while the caller has no confirmed admin role. */
    @Test
    fun unconfirmedRoleDoesNotShowAdminButton() {
        showScreen(
            isAdmin = false,
            loading = true,
        )

        composeRule.onNodeWithText("Admin").assertDoesNotExist()
    }

    /** A profile failure displays its message without granting admin access. */
    @Test
    fun profileFailureShowsErrorAndHidesAdminButton() {
        var dismissClicks = 0

        showScreen(
            isAdmin = false,
            error = ScreenError(
                title = "Account unavailable",
                message = "Could not connect to load your role.",
                actionLabel = "Dismiss",
                onAction = { dismissClicks++ },
            ),
        )

        composeRule.onNodeWithText("Admin").assertDoesNotExist()

        composeRule.onNodeWithText("Could not connect to load your role.")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Dismiss").performClick()

        composeRule.runOnIdle {
            assertEquals(1, dismissClicks)
        }
    }

    /** Supplies only UI state, keeping tests independent of login and Docker. */
    private fun showScreen(
        isAdmin: Boolean,
        loading: Boolean = false,
        error: ScreenError? = null,
        onAdmin: () -> Unit = {},
    ) {
        composeRule.setContent {
            RecallTheme {
                MyItemsScreen(
                    items = emptyList(),
                    isAdmin = isAdmin,
                    loading = loading,
                    loadingMore = false,
                    error = error,
                    onOpen = {},
                    onAdd = {},
                    onAdmin = onAdmin,
                    onLoadMore = {},
                    onSignOut = {},
                    onDelete = {},
                    onDeleteAccount = {},
                )
            }
        }
    }
}