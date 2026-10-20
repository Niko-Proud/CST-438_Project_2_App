package com.example.recallapp.ui.items

import com.example.recallapp.auth.TokenStore
import com.example.recallapp.data.BrainwormApiService
import com.example.recallapp.data.FakeApiService
import com.example.recallapp.data.RegistryItem
import com.example.recallapp.data.SpringPage
import com.example.recallapp.ui.login.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/** API that answers every request with the same error code. */
class ErrorApi(private val code: Int) : BrainwormApiService {
    private fun <T> error(): Response<T> = Response.error(code, "".toResponseBody(null))
    override suspend fun deleteMyAccount(token: String): Response<Unit> = error()
    override suspend fun getRegistryItems(token: String, page: Int, size: Int): Response<SpringPage<RegistryItem>> = error()
    override suspend fun getRegistryItem(token: String, itemId: Long): Response<RegistryItem> = error()
    override suspend fun createRegistryItem(token: String, item: RegistryItem): Response<RegistryItem> = error()
    override suspend fun updateRegistryItem(token: String, itemId: Long, item: RegistryItem): Response<RegistryItem> = error()
    override suspend fun deleteRegistryItem(token: String, itemId: Long): Response<Unit> = error()
}

/** Tests the home list and detail logic with the fake API, so no server is needed. */
class RegistryViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val tokens = TokenStore().apply { save("test-token") }

    // Created on first use, after the rule has set up the Main dispatcher.
    private val vm by lazy { RegistryViewModel(tokens, FakeApiService()) }

    @Test
    fun firstLoad_showsItemsWithRecallStatus() {
        val items = vm.items.value
        assertEquals(4, items.size)
        assertEquals(FoodStatus.RECALLED, items[0].status)
        assertEquals("Listeria", items[0].recallReason)
        assertEquals(FoodStatus.SAFE, items[1].status)
        // Recalled but no batch number, so the user should check the package.
        assertEquals(FoodStatus.CAUTION, items[3].status)
        assertNull(vm.listError.value)
    }

    @Test
    fun loadSingleItem_showsRecallInfo() {
        vm.loadSingleItem(1)

        val state = vm.detailState.value as DetailUiState.Success
        assertEquals("Frozen dumplings", state.product)
        assertEquals("Listeria", state.recallReason)
        assertEquals("2026-10-05", state.recallDate)
    }

    @Test
    fun loadSingleItem_missingItemShowsError() {
        vm.loadSingleItem(999)

        val state = vm.detailState.value as DetailUiState.Error
        assertEquals(404, state.statusCode)
    }

    @Test
    fun addItem_appearsInHomeList() {
        var done = false
        vm.saveItem(null, "Milk", "") { done = true }

        assertTrue(done)
        assertEquals(5, vm.items.value.size)
        assertEquals("Milk", vm.items.value.last().product)
        assertNull(vm.items.value.last().batch)
    }

    @Test
    fun deleteAccount_signsOutAndClearsList() {
        var done = false
        vm.deleteAccount { done = true }

        assertTrue(done)
        assertFalse(tokens.isSignedIn)
        assertEquals(0, vm.items.value.size)
    }

    @Test
    fun deleteItem_removesItFromHomeList() {
        vm.deleteItem(2) {}

        assertEquals(listOf(1L, 3L, 4L), vm.items.value.map { it.id })
    }

    @Test
    fun list401_signsOutAndClearsToken() {
        val errorVm = RegistryViewModel(tokens, ErrorApi(401))

        assertTrue(errorVm.signedOut.value)
        assertFalse(tokens.isSignedIn)
        assertNull(errorVm.listError.value)
    }

    @Test
    fun list403_showsNoAccess() {
        val errorVm = RegistryViewModel(tokens, ErrorApi(403))

        assertEquals("No access", errorVm.listError.value?.title)
        assertFalse(errorVm.signedOut.value)
    }

    @Test
    fun list500_showsServerProblemWithTryAgain() {
        val errorVm = RegistryViewModel(tokens, ErrorApi(500))

        val error = errorVm.listError.value
        assertEquals("Server problem", error?.title)
        assertEquals("Try again", error?.actionLabel)
    }

    @Test
    fun detail404_showsNotFoundMessage() {
        val errorVm = RegistryViewModel(tokens, ErrorApi(404))
        errorVm.loadSingleItem(1)

        val state = errorVm.detailState.value as DetailUiState.Error
        assertEquals(404, state.statusCode)
        assertEquals(messageForCode(404), state.message)
    }
}
