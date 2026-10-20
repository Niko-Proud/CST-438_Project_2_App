package com.example.recallapp.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recallapp.data.BrainwormApiService
import com.example.recallapp.data.RecallDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Holds recall data, pagination, and request status for the screen. */
data class ManageRecallsUiState(
    val recalls: List<RecallDto> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val nextPage: Int = 0,
    val hasMore: Boolean = false,
)

/**
 * Reads recall pages from the API.
 * Loading one page at a time avoids downloading the entire recall database.
 */
class ManageRecallsViewModel(
    private val api: BrainwormApiService,
) : ViewModel() {

    private val _state = MutableStateFlow(ManageRecallsUiState())
    val state: StateFlow<ManageRecallsUiState> = _state.asStateFlow()

    private var requestJob: Job? = null

    init {
        refresh()
    }

    /** Starts again at page zero; replaces the list only after success. */
    fun refresh() {
        fetchPage(pageNumber = 0, replace = true)
    }

    /** Requests the next page only when the server says more pages exist. */
    fun loadMore() {
        val current = _state.value
        if (!current.hasMore) return

        fetchPage(
            pageNumber = current.nextPage,
            replace = false,
        )
    }

    /** Retries the failed initial request or the failed next-page request. */
    fun retry() {
        val current = _state.value

        if (current.nextPage == 0) {
            refresh()
        } else {
            loadMore()
        }
    }

    /**
     * Performs one request and preserves existing records on failure.
     * A single active job prevents overlapping pagination requests.
     */
    private fun fetchPage(pageNumber: Int, replace: Boolean) {
        if (requestJob?.isActive == true) return

        _state.update {
            it.copy(
                loading = replace,
                loadingMore = !replace,
                error = null,
            )
        }

        requestJob = viewModelScope.launch {
            try {
                val response = api.getRecalls(
                    page = pageNumber,
                    size = 20,
                )

                check(response.isSuccessful) {
                    "Could not load recalls (HTTP ${response.code()})."
                }

                val page = response.body()
                    ?: error("The server returned no recall data.")

                check(page.number == pageNumber) {
                    "The server returned an unexpected page."
                }

                check(page.last || page.content.isNotEmpty()) {
                    "The server returned an empty unfinished page."
                }

                _state.update { current ->
                    val combined = if (replace) {
                        page.content
                    } else {
                        current.recalls + page.content
                    }

                    current.copy(
                        recalls = combined.distinctBy { it.id },
                        nextPage = page.number + 1,
                        hasMore = !page.last,
                        error = null,
                    )
                }
            } catch (cancelled: CancellationException) {
                // Cancellation is lifecycle behavior, not a request error.
                throw cancelled
            } catch (exception: Exception) {
                _state.update {
                    it.copy(
                        error = exception.message
                            ?: "Could not load recalls.",
                        // Remember which page failed so Retry repeats it.
                        nextPage = pageNumber,
                    )
                }
            } finally {
                _state.update {
                    it.copy(
                        loading = false,
                        loadingMore = false,
                    )
                }
            }
        }
    }
}