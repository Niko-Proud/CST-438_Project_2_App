package com.example.recallapp.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recallapp.ui.theme.Lab
import com.example.recallapp.ui.theme.RecallTheme

/**
 * One row on the My Items screen.
 *
 * @param recallReason what the recall is about (for example "Listeria"), or null if the item is not recalled
 * @param recallDate when the recall started, already formatted for display
 */
data class ItemUi(
    val id: Long,
    val product: String,
    val batch: String?,
    val recallReason: String? = null,
    val recallDate: String? = null,
) {
    /** True when the item has a recall. */
    val isRecalled: Boolean get() = recallReason != null
}

/** An error to show on screen with one button (for example "Try again"). */
data class ScreenError(
    val title: String,
    val message: String,
    val actionLabel: String,
    val onAction: () -> Unit,
)

/**
 * Home screen: the foods the user is tracking. UI only, all data comes in through parameters.
 *
 * @param items the rows to show
 * @param isAdmin shows the Admin button
 * @param loading shows a spinner instead of the list
 * @param loadingMore shows a spinner under the list while the next page loads
 * @param error shown instead of the list when not null
 */
@Composable
fun MyItemsScreen(
    items: List<ItemUi>,
    isAdmin: Boolean,
    loading: Boolean,
    loadingMore: Boolean,
    error: ScreenError?,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onAdmin: () -> Unit,
    onLoadMore: () -> Unit,
    onSignOut: () -> Unit,
) {
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd) { if (nearEnd) onLoadMore() }

    Scaffold(
        containerColor = Lab.Bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.navigationBarsPadding(),
                containerColor = Lab.AccentDark,
                contentColor = Color.White,
                shape = MaterialTheme.shapes.medium,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add item", style = MaterialTheme.typography.labelLarge) },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 110.dp),
        ) {
            item { Header(isAdmin, items.size, onAdmin, onSignOut) }

            val recalled = items.firstOrNull { it.isRecalled }
            if (recalled != null && error == null) item { RecallBanner(recalled) { onOpen(recalled.id) } }

            item {
                Text(
                    "MY ITEMS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Lab.TealDark,
                    modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 10.dp),
                )
            }

            when {
                loading -> item { Spinner() }
                error != null -> item { ErrorCard(error) }
                items.isEmpty() -> item { EmptyCard() }
                else -> items(items, key = { it.id }) { item -> ItemCard(item) { onOpen(item.id) } }
            }

            if (loadingMore) item { Spinner() }
        }
    }
}

@Composable
private fun Header(isAdmin: Boolean, count: Int, onAdmin: () -> Unit, onSignOut: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().background(Lab.Teal).statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(Lab.Accent))
            Spacer(Modifier.width(10.dp))
            Text("BRAINWORM", style = MaterialTheme.typography.headlineSmall, color = Color.White, modifier = Modifier.weight(1f))
            if (isAdmin) {
                TextButton(onClick = onAdmin) { Text("Admin", color = Lab.Mint, style = MaterialTheme.typography.labelLarge) }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = Color.White) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Sign out") }, onClick = { menuOpen = false; onSignOut() })
                }
            }
        }
        Text("Is your food\nsafe today?", style = MaterialTheme.typography.displaySmall, color = Color.White)
        Text(
            if (count == 1) "You are tracking 1 food." else "You are tracking $count foods.",
            style = MaterialTheme.typography.bodyMedium,
            color = Lab.Mint,
        )
    }
}

@Composable
private fun RecallBanner(item: ItemUi, onOpen: () -> Unit) {
    Column(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).fillMaxWidth()
            .background(Lab.AccentDark, RoundedCornerShape(6.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text("RECALL ALERT", style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
        Text("${item.product} recalled".uppercase(), style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text(
            listOfNotNull(item.recallReason, item.recallDate?.let { "Recalled $it" }).joinToString(". ") + ".",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
        Button(
            onClick = onOpen,
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Lab.Ink),
        ) { Text("See item", style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun ItemCard(item: ItemUi, onClick: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()
            .background(Lab.Surface, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusBadge(item.isRecalled)
            Text(item.product, style = MaterialTheme.typography.titleMedium, color = Lab.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(item.batch?.let { "Batch $it" } ?: "No batch", style = MaterialTheme.typography.bodySmall, color = Lab.Muted)
            item.recallReason?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Lab.Hazard) }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Lab.Chevron)
    }
}

@Composable
private fun StatusBadge(recalled: Boolean) {
    Text(
        text = if (recalled) "RECALLED" else "OK",
        style = MaterialTheme.typography.labelSmall,
        color = if (recalled) Color.White else Lab.Ink,
        modifier = Modifier
            .background(if (recalled) Lab.Recalled else Lab.Line, RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun EmptyCard() {
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().background(Lab.Surface, RoundedCornerShape(4.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("No items yet", style = MaterialTheme.typography.titleMedium, color = Lab.Ink)
        Text("Tap Add item to start tracking a food.", style = MaterialTheme.typography.bodyMedium, color = Lab.Muted)
    }
}

@Composable
private fun ErrorCard(error: ScreenError) {
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().background(Lab.Surface, RoundedCornerShape(4.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(error.title.uppercase(), style = MaterialTheme.typography.labelMedium, color = Lab.Hazard)
        Text(error.message, style = MaterialTheme.typography.bodyLarge, color = Lab.Ink)
        Button(
            onClick = error.onAction,
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = Lab.Teal, contentColor = Color.White),
        ) { Text(error.actionLabel, style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun Spinner() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Lab.Teal)
    }
}

private val previewItems = listOf(
    ItemUi(1, "Frozen chicken dumplings", "24215", recallReason = "Listeria", recallDate = "Oct 5, 2026"),
    ItemUi(2, "Rolled oats", null),
    ItemUi(3, "Peanut butter, creamy", "PB-0926"),
)

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun MyItemsPreview() {
    RecallTheme {
        MyItemsScreen(previewItems, isAdmin = true, loading = false, loadingMore = false, error = null, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun MyItemsErrorPreview() {
    RecallTheme {
        MyItemsScreen(
            emptyList(), isAdmin = false, loading = false, loadingMore = false,
            error = ScreenError("Server error", "The server had a problem. Try again in a moment.", "Try again") {},
            {}, {}, {}, {}, {},
        )
    }
}