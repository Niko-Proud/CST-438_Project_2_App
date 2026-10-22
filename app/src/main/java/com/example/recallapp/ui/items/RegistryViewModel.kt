package com.example.recallapp.ui.items
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recallapp.BuildConfig
import com.example.recallapp.auth.TokenStore
import com.example.recallapp.data.BrainwormApiService
import com.example.recallapp.data.FakeApiService
import com.example.recallapp.data.RegistryItem
import com.example.recallapp.data.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A short, readable message for an HTTP error code from the API. */
fun messageForCode(code: Int): String = when (code) {
    401 -> "Your sign-in has expired. Please sign in again."
    403 -> "You don't have permission to do this."
    404 -> "This food was not found. It may have been deleted."
    in 500..599 -> "Something went wrong on our server. Try again in a moment."
    else -> "Something went wrong (code $code). Try again."
}

/** Shown when the phone cannot reach the server at all. */
const val NETWORK_MESSAGE = "Could not reach the server. Check your internet and try again."

/** Turns an API item into a home screen row, using the first matched recall if there is one. */
fun RegistryItem.toItemUi(): ItemUi {
    val recall = recalledFoods?.firstOrNull()
    return ItemUi(
        id = id ?: 0,
        product = product,
        batch = batch,
        recallReason = recall?.let { it.contaminant ?: "Recalled" },
        recallDate = recall?.recallDate,
    )
}

class RegistryViewModel(
    private val tokens: TokenStore,
    private val api: BrainwormApiService = RetrofitClient.instance,
) : ViewModel() {
    private val bearerToken: String
        get() = tokens.bearer() ?: ""

    private val _items = MutableStateFlow<List<ItemUi>>(emptyList())
    val items: StateFlow<List<ItemUi>> = _items.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    private val _listError = MutableStateFlow<ScreenError?>(null)
    val listError: StateFlow<ScreenError?> = _listError.asStateFlow()

    private val _signedOut = MutableStateFlow(false)
    /** Becomes true when the server says the token is no longer valid (401). The screen then goes to login. */
    val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()

    private var currentPage = 0
    private var isLastPage = false
    private val _detailState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val detailState: StateFlow<DetailUiState> = _detailState.asStateFlow()

    init { loadItems() }

    fun loadItems() {
        if (_isLoading.value) {
            return
        }
        _isLoading.value = true
        _listError.value = null
        currentPage = 0
        isLastPage = false

        viewModelScope.launch {
            try {
                val response = api.getRegistryItems(bearerToken, page = currentPage)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _items.value = body.content.map { it.toItemUi() }
                        isLastPage = body.isLastPage()
                    }
                }
                else {
                    handleListError(response.code())
                }
            } catch (e: Exception) {
                _listError.value = ScreenError("No connection", NETWORK_MESSAGE, "Try again") { loadItems() }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Deletes the user's account on the server, then signs them out. onComplete runs only on success. */
    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.deleteMyAccount(bearerToken)
                if (response.isSuccessful) {
                    tokens.clear()
                    _items.value = emptyList()
                    onComplete()
                } else {
                    handleListError(response.code())
                }
            } catch (e: Exception) {
                _listError.value = ScreenError("No connection", NETWORK_MESSAGE, "OK") { loadItems() }
            }
        }
    }

    fun loadMoreItems() {
        // Wait for the first page, and stop if it failed.
        if (_isLoadingMore.value || _isLoading.value || _listError.value != null || isLastPage) {
            return
        }
        _isLoadingMore.value = true

        viewModelScope.launch {
            try {
                val nextPage = currentPage + 1
                val response = api.getRegistryItems(bearerToken, page = nextPage)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val newItems = body.content.map { it.toItemUi() }
                        _items.value += newItems
                        currentPage = nextPage
                        isLastPage = body.isLastPage()
                    }
                }
            } catch (e: Exception) {} finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun loadSingleItem(itemId: Long) {
        _detailState.value = DetailUiState.Loading
        viewModelScope.launch {
            try {
                val response = api.getRegistryItem(bearerToken, itemId)
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item != null) {
                        val row = item.toItemUi()
                        _detailState.value = DetailUiState.Success(
                            product = item.product,
                            batch = item.batch ?: "",
                            recallReason = row.recallReason,
                            recallDate = row.recallDate,
                        )
                    }
                }
                else {
                    handleDetailError(response.code())
                }
            } catch (e: Exception) {
                _detailState.value = DetailUiState.Error(0, NETWORK_MESSAGE)
            }
        }
    }

    fun saveItem(itemId: Long?, product: String, batch: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _detailState.value = DetailUiState.Loading
            try {
                val item = RegistryItem(product = product, batch = batch.takeIf { it.isNotBlank() })
                val response = if (itemId == null) {
                    api.createRegistryItem(bearerToken, item)
                }
                else {
                    api.updateRegistryItem(bearerToken, itemId, item)
                }

                if (response.isSuccessful) {
                    loadItems()
                    onComplete()
                }
                else {
                    handleDetailError(response.code())
                }
            } catch (e: Exception) {
                _detailState.value = DetailUiState.Error(0, NETWORK_MESSAGE)
            }
        }
    }

    fun deleteItem(itemId: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.deleteRegistryItem(bearerToken, itemId)
                if (response.isSuccessful) {
                    loadItems()
                    onComplete()
                }
                else {
                    handleListError(response.code())
                }
            } catch (e: Exception) {
                _listError.value = ScreenError("No connection", NETWORK_MESSAGE, "Try again") { loadItems() }
            }
        }
    }

    /** 401 signs the user out. Other codes show a message on the home screen with a Try again button. */
    private fun handleListError(code: Int) {
        if (code == 401) {
            signOut()
            return
        }
        val title = when (code) {
            403 -> "No access"
            404 -> "Not found"
            in 500..599 -> "Server problem"
            else -> "Error $code"
        }
        _listError.value = ScreenError(title, messageForCode(code), "Try again") { loadItems() }
    }

    /** 401 signs the user out. Other codes show the message on the detail screen. */
    private fun handleDetailError(code: Int) {
        if (code == 401) {
            signOut()
            return
        }
        _detailState.value = DetailUiState.Error(code, messageForCode(code))
    }

    private fun signOut() {
        tokens.clear()
        _signedOut.value = true
    }

    companion object {
        fun factory(tokens: TokenStore): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val api = if (BuildConfig.USE_FAKE_API) FakeApiService() else RetrofitClient.instance
                RegistryViewModel(tokens, api)
            }
        }
    }
}
