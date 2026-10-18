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
                    _listError.value = ScreenError("Error ${response.code()}", "Failed to load list.", "Retry") { loadItems() }
                }
            } catch (e: Exception) {
                _listError.value = ScreenError("Network Error", e.message ?: "Unknown error", "Retry") { loadItems() }
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


}