package com.example.recallapp.ui.items
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recallapp.data.BrainwormApiService
import com.example.recallapp.data.RegistryItem
import com.example.recallapp.data.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistryViewModel : ViewModel() {
    private val api: BrainwormApiService = RetrofitClient.instance
    private val mockToken = "Bearer mock_token"
    private val _items = MutableStateFlow<List<ItemUi>>(emptyList())
    val items: StateFlow<List<ItemUi>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    private val _listError = MutableStateFlow<ScreenError?>(null)
    val listError: StateFlow<ScreenError?> = _listError.asStateFlow()

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
                val response = api.getRegistryItems(mockToken, page = currentPage)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _items.value = body.content.map { ItemUi(id = it.id ?: 0, product = it.product, batch = it.batch) }
                        isLastPage = body.last
                    }
                }
                else {
                    _listError.value = ScreenError("Error ${response.code()}", "FAILED TO LOAD LIST", "RETRY") { loadItems() }
                }
            } catch (e: Exception) {
                _listError.value = ScreenError("NETWORK ERROR", e.message ?: "UNKNOWN ERROR", "RETRY") { loadItems() }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreItems() {
        if (_isLoadingMore.value || isLastPage) {
            return
        }
        _isLoadingMore.value = true

        viewModelScope.launch {
            try {
                val nextPage = currentPage + 1
                val response = api.getRegistryItems(mockToken, page = nextPage)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val newItems = body.content.map { ItemUi(id = it.id ?: 0, product = it.product, batch = it.batch) }
                        _items.value += newItems
                        currentPage = nextPage
                        isLastPage = body.last
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
                val response = api.getRegistryItem(mockToken, itemId)
                if (response.isSuccessful) {
                    val item = response.body()
                    if (item != null) { _detailState.value = DetailUiState.Success(item.product, item.batch ?: "") }
                }
                else {
                    _detailState.value = DetailUiState.Error(response.code(), "FAILED TO LOAD ITEM")
                }
            } catch (e: Exception) {
                _detailState.value = DetailUiState.Error(500, e.message ?: "NETWORK ERROR")
            }
        }
    }

    fun saveItem(itemId: Long?, product: String, batch: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _detailState.value = DetailUiState.Loading
            try {
                val item = RegistryItem(product = product, batch = batch.takeIf { it.isNotBlank() })
                val response = if (itemId == null) {
                    api.createRegistryItem(mockToken, item)
                }
                else {
                    api.updateRegistryItem(mockToken, itemId, item)
                }

                if (response.isSuccessful) {
                    loadItems()
                    onComplete()
                }
                else {
                    _detailState.value = DetailUiState.Error(response.code(), "FAILED TO SAVE ITEM")
                }
            } catch (e: Exception) {
                _detailState.value = DetailUiState.Error(500, "NETWORK ERROR")
            }
        }
    }

    fun deleteItem(itemId: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.deleteRegistryItem(mockToken, itemId)
                if (response.isSuccessful) {
                    loadItems()
                    onComplete()
                }
                else {
                    _detailState.value = DetailUiState.Error(response.code(), "FAILED TO DELETE ITEM")
                }
            } catch (e: Exception) {
                _detailState.value = DetailUiState.Error(500, "NETWORK ERROR")
            }
        }
    }
}