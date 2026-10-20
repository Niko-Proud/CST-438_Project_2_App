package com.example.recallapp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.recallapp.data.RecallDto
import com.example.recallapp.data.RetrofitClient
import com.example.recallapp.ui.theme.Lab
import com.example.recallapp.ui.theme.RecallTheme

/** Connects the read-only screen to its API-backed ViewModel. */
@Composable
fun ManageRecallsRoute(onBack: () -> Unit) {
    val vm: ManageRecallsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ManageRecallsViewModel(RetrofitClient.instance)
            }
        },
    )

    val state by vm.state.collectAsStateWithLifecycle()

    ManageRecallsScreen(
        state = state,
        onBack = onBack,
        onRefresh = vm::refresh,
        onRetry = vm::retry,
        onLoadMore = vm::loadMore,
    )
}

/**
 * Displays recalls without changing database records.
 * Receives callbacks so previews and UI tests do not need networking.
 */
@Composable
fun ManageRecallsScreen(
    state: ManageRecallsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val busy = state.loading || state.loadingMore

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Lab.Bg)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Lab.Teal)
                    .statusBarsPadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(onClick = onBack) {
                    Text("Back to Admin Console", color = Lab.Mint)
                }

                Text(
                    text = "Manage Recalls",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )

                Text(
                    text = "View recall records available in the database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Lab.Mint,
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = "${state.recalls.size} loaded",
                    color = Lab.TealDark,
                    style = MaterialTheme.typography.labelMedium,
                )

                TextButton(
                    onClick = onRefresh,
                    enabled = !busy,
                ) {
                    Text("Refresh")
                }
            }
        }

        if (state.loading) {
            item {
                RecallLoadingIndicator()
            }
        } else {
            // Preserve any previously loaded records when a request fails.
            items(
                items = state.recalls,
                key = { it.id },
            ) { recall ->
                RecallCard(recall)
            }

            if (state.recalls.isEmpty() && state.error == null) {
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .background(
                                Lab.Surface,
                                RoundedCornerShape(4.dp),
                            )
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "No recalls available yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Lab.Ink,
                        )
                        Text(
                            text = "Records will appear here after they are " +
                                    "added to the database. Tap Refresh to check again.",
                            color = Lab.Muted,
                        )
                    }
                }
            }
        }

        state.error?.let { message ->
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .background(Lab.Surface, RoundedCornerShape(4.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Couldn't load recalls",
                        style = MaterialTheme.typography.titleMedium,
                        color = Lab.Hazard,
                    )
                    Text(message, color = Lab.Ink)

                    Button(
                        onClick = onRetry,
                        enabled = !busy,
                    ) {
                        Text("Retry")
                    }
                }
            }
        }

        if (state.loadingMore) {
            item {
                RecallLoadingIndicator()
            }
        } else if (
            state.hasMore &&
            !state.loading &&
            state.error == null
        ) {
            item {
                OutlinedButton(
                    onClick = onLoadMore,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                ) {
                    Text("Load more")
                }
            }
        }
    }
}

/** Displays a recall and handles missing fields without showing "null". */
@Composable
private fun RecallCard(recall: RecallDto) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(Lab.Surface, RoundedCornerShape(4.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = recall.product
                ?.takeIf { it.isNotBlank() }
                ?: "Unnamed product",
            style = MaterialTheme.typography.titleMedium,
            color = Lab.Ink,
        )

        Text(
            text = "Recall ID: ${recall.id}",
            style = MaterialTheme.typography.bodySmall,
            color = Lab.Muted,
        )

        RecallField("Contaminant", recall.contaminant)
        RecallField("Recall date", recall.recallDate)
        RecallField("Manufacture date", recall.manuDate)
        RecallField("Batch", recall.batch)
    }
}

/** Formats optional values consistently across recall cards. */
@Composable
private fun RecallField(label: String, value: String?) {
    val displayedValue = value
        ?.takeIf { it.isNotBlank() }
        ?: "Not provided"

    Text(
        text = "$label: $displayedValue",
        style = MaterialTheme.typography.bodyMedium,
        color = Lab.Ink,
    )
}

/** Reuses the same loading indicator for initial and subsequent requests. */
@Composable
private fun RecallLoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        CircularProgressIndicator(color = Lab.Teal)
    }
}

/** Shows the layout with a clearly fictional record and no network requests. */
@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ManageRecallsPreview() {
    RecallTheme {
        ManageRecallsScreen(
            state = ManageRecallsUiState(
                recalls = listOf(
                    RecallDto(
                        id = 1L,
                        product = "Sample product — preview only",
                        contaminant = "Example contaminant",
                        recallDate = "2026-10-01",
                        manuDate = null,
                        batch = "SAMPLE-001",
                    ),
                ),
            ),
            onBack = {},
            onRefresh = {},
            onRetry = {},
            onLoadMore = {},
        )
    }
}