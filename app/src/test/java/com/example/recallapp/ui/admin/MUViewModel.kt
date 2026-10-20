package com.example.recallapp.ui.admin

import com.example.recallapp.data.BrainwormApiService
import com.example.recallapp.data.RegistryItem
import com.example.recallapp.data.UserDto
import com.example.recallapp.data.UserPageDto
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import com.example.recallapp.data.UpdateUserRoleRequest
import com.example.recallapp.data.RecallDto
import com.example.recallapp.data.RecallPageDto

/**
 * Tests user loading and searching without Android UI or a running API.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManageUsersViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var api: FakeUsersApi

    /** Replaces Android's Main dispatcher with a controllable test dispatcher. */
    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        api = FakeUsersApi()
    }

    /** Restores the dispatcher so other tests are not affected. */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Confirms that users from later pages are included. */
    @Test
    fun loadsEveryPage() = runTest(dispatcher) {
        val vm = ManageUsersViewModel(api)

        advanceUntilIdle()

        assertEquals(listOf(0, 1), api.requestedPages)
        assertEquals(
            listOf("admin_Kiya", "normie"),
            vm.state.value.users.map { it.username },
        )
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.error)
    }

    /** Search is case-insensitive and includes users from the second page. */
    @Test
    fun searchesAcrossLoadedPages() = runTest(dispatcher) {
        val vm = ManageUsersViewModel(api)
        advanceUntilIdle()

        vm.updateQuery("  NORM  ")

        assertEquals(
            listOf("normie"),
            vm.state.value.visibleUsers.map { it.username },
        )

        vm.updateQuery("no-such-user")
        assertTrue(vm.state.value.visibleUsers.isEmpty())

        vm.updateQuery("")
        assertEquals(2, vm.state.value.visibleUsers.size)

        // Searching did not trigger additional HTTP requests.
        assertEquals(listOf(0, 1), api.requestedPages)
    }

    /** A failed later page must not be presented as a complete user list. */
    @Test
    fun failedLoadShowsErrorAndCanRetry() = runTest(dispatcher) {
        api.failOnPage = 1
        val vm = ManageUsersViewModel(api)

        advanceUntilIdle()

        assertEquals("Connection failed.", vm.state.value.error)
        assertFalse(vm.state.value.loading)
        assertTrue(vm.state.value.users.isEmpty())

        api.failOnPage = null
        vm.loadUsers()
        advanceUntilIdle()

        assertNull(vm.state.value.error)
        assertEquals(2, vm.state.value.users.size)
    }

    /** An empty database is a successful result, not an error. */
    @Test
    fun acceptsAnEmptyUserList() = runTest(dispatcher) {
        api.pages = listOf(
            UserPageDto(
                content = emptyList(),
                number = 0,
                last = true,
                totalElements = 0,
            ),
        )

        val vm = ManageUsersViewModel(api)
        advanceUntilIdle()

        assertTrue(vm.state.value.users.isEmpty())
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.error)
    }
}

/**
 * Supplies predictable user pages and records which pages were requested.
 * Registry methods deliberately fail if accidentally called by these tests.
 */
private class FakeUsersApi : BrainwormApiService {

    val requestedPages = mutableListOf<Int>()
    var failOnPage: Int? = null

    var pages = listOf(
        UserPageDto(
            content = listOf(UserDto(1L, "admin_Kiya", true)),
            number = 0,
            last = false,
            totalElements = 2,
        ),
        UserPageDto(
            content = listOf(UserDto(2L, "normie", false)),
            number = 1,
            last = true,
            totalElements = 2,
        ),
    )

    /** Returns one configured page or simulates a connection failure. */
    override suspend fun getUsers(
        page: Int,
        size: Int,
    ): Response<UserPageDto> {
        requestedPages.add(page)

        if (page == failOnPage) {
            throw IOException("Connection failed.")
        }

        return Response.success(pages[page])
    }

    override suspend fun getRegistryItem(
        token: String,
        itemId: Long,
    ): Response<RegistryItem> = error("Not used in user tests.")

    override suspend fun createRegistryItem(
        token: String,
        item: RegistryItem,
    ): Response<RegistryItem> = error("Not used in user tests.")

    override suspend fun updateRegistryItem(
        token: String,
        itemId: Long,
        item: RegistryItem,
    ): Response<RegistryItem> = error("Not used in user tests.")

    override suspend fun deleteRegistryItem(
        token: String,
        itemId: Long,
    ): Response<Unit> = error("Not used in user tests.")

    /**
     * Satisfies the API interface.
     * Existing loading/search tests should never call this operation.
     */
    override suspend fun updateUserRole(
        userId: Long,
        request: UpdateUserRoleRequest,
    ): Response<UserDto> = error("Not used in loading tests.")

    /**
     * Satisfies the API interface.
     * Existing loading/search tests should never call this operation.
     */
    override suspend fun deleteUser(
        userId: Long,
    ): Response<Unit> = error("Not used in loading tests.")

    /** Recall loading is not exercised by these user tests. */
    override suspend fun getRecalls(
        page: Int,
        size: Int,
        product: String?,
    ): Response<RecallPageDto> = error("Not used in user tests.")
}