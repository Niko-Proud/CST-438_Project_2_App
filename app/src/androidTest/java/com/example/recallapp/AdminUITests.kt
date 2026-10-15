package com.example.recallapp

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.recallapp.ui.admin.AdminConsoleScreen
import com.example.recallapp.ui.items.MyItemsScreen
import com.example.recallapp.ui.theme.RecallTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AdminUiTest {

    @get:Rule
    val compose = createComposeRule()

    private data class FakeUser(
        val id: Long,
        val username: String,
        val isAdmin: Boolean,
    )

    private lateinit var admin: FakeUser
    private lateinit var normie: FakeUser

    @Before
    fun setUp() {
        // Fake users before every test. one is an admin the other is a normie
        admin = FakeUser(
            id = 1L,
            username = "admin_alex",
            isAdmin = true,
        )

        normie = FakeUser(
            id = 2L,
            username = "user_sam",
            isAdmin = false,
        )
    }

    @Test
    fun admin_seesAdminButton_andCanClickIt() {
        var adminClicks = 0

        showMyItems(
            user = admin,
            onAdmin = { adminClicks++ },
        )

        compose.onNodeWithText("Admin")
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        compose.runOnIdle {
            assertEquals(1, adminClicks)
        }
    }

    @Test
    fun normie_doesNotSeeAdminButton() {
        showMyItems(user = normie)

        compose.onNodeWithText("Admin")
            .assertDoesNotExist()
    }

    @Test
    fun adminConsole_displaysBothCards_andCallsCorrectActions() {
        var recallClicks = 0
        var userClicks = 0

        compose.setContent {
            RecallTheme {
                AdminConsoleScreen(
                    onBack = {},
                    onManageRecalls = { recallClicks++ },
                    onManageUsers = { userClicks++ },
                )
            }
        }

        compose.onNodeWithText("Manage recalls")
            .performScrollTo()
            .assertIsDisplayed()

        compose.onNodeWithText("Open recalls")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        compose.runOnIdle {
            assertEquals(1, recallClicks)
            assertEquals(0, userClicks)
        }

        compose.onNodeWithText("Manage users")
            .performScrollTo()
            .assertIsDisplayed()

        compose.onNodeWithText("Open users")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        compose.runOnIdle {
            assertEquals(1, recallClicks)
            assertEquals(1, userClicks)
        }
    }

    private fun showMyItems(
        user: FakeUser,
        onAdmin: () -> Unit = {},
    ) {
        compose.setContent {
            RecallTheme {
                MyItemsScreen(
                    items = emptyList(),
                    isAdmin = user.isAdmin,
                    loading = false,
                    loadingMore = false,
                    error = null,
                    onOpen = {},
                    onAdd = {},
                    onAdmin = onAdmin,
                    onLoadMore = {},
                    onSignOut = {},
                )
            }
        }
    }
}