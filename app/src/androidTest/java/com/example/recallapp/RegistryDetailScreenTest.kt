package com.example.recallapp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.recallapp.ui.items.DetailUiState
import com.example.recallapp.ui.items.RegistryDetailScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class RegistryDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun successState_displayPopulatedFieldsAndAllowsSave() {
        var capturedProduct = ""
        var capturedBatch = ""

        composeTestRule.setContent {
            RegistryDetailScreen(
                uiState = DetailUiState.Success(product = "Organic Spinach", batch = "LOT-123"),
                isCreatingNew = false,
                onSaveClick = { product, batch ->
                    capturedProduct = product
                    capturedBatch = batch
                }, onDeleteClick = {}
            )
        }

        composeTestRule.onNodeWithText("Organic Spinach").assertIsDisplayed()
        composeTestRule.onNodeWithText("LOT-123").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Changes").performClick()

        assertEquals("Organic Spinach", capturedProduct)
        assertEquals("LOT-123", capturedBatch)
    }

    @Test
    fun errorState_displaysErrorMessage() {
        composeTestRule.setContent {
            RegistryDetailScreen(
                uiState = DetailUiState.Error(403, "You do not own this item"),
                isCreatingNew = false,
                onSaveClick = { _, _ -> },
                onDeleteClick = {}
            )
        }
        composeTestRule.onNodeWithText("Error 403: You do not own this item").assertIsDisplayed()
    }
}