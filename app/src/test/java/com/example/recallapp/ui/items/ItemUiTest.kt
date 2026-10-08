package com.example.recallapp.ui.items

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [ItemUi], the model the home screen uses to show an item. */
class ItemUiTest {

    @Test
    fun itemWithRecallReason_isRecalled() {
        val item = ItemUi(
            id = 1,
            product = "Peanut butter",
            batch = "L2291",
            recallReason = "Possible Salmonella",
            recallDate = "Oct 1, 2026",
        )
        assertTrue(item.isRecalled)
    }

    @Test
    fun itemWithoutRecallReason_isNotRecalled() {
        val item = ItemUi(
            id = 2,
            product = "Oat milk",
            batch = null,
            recallReason = null,
            recallDate = null,
        )
        assertFalse(item.isRecalled)
    }

    @Test
    fun onlyRecalledItemsAreCountedForTheBanner() {
        val items = listOf(
            ItemUi(id = 1, product = "Peanut butter", batch = null, recallReason = "Salmonella", recallDate = null),
            ItemUi(id = 2, product = "Oat milk", batch = null, recallReason = null, recallDate = null),
            ItemUi(id = 3, product = "Spinach", batch = null, recallReason = "E. coli", recallDate = null),
        )
        assertEquals(2, items.count { it.isRecalled })
    }
}