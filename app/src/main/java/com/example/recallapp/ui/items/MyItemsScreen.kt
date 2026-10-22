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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recallapp.ui.AccountMenu
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

    /** Safe, caution or recalled, used for the colors. */
    val status: FoodStatus get() = foodStatus(recallReason, batch)
}

/** How safe a food is. Green is safe, orange means check the batch, red means recalled. */
enum class FoodStatus(val label: String, val color: Color, val softColor: Color) {
    SAFE("SAFE", Lab.Safe, Lab.SafeSoft),
    CAUTION("CHECK", Lab.Caution, Lab.CautionSoft),
    RECALLED("RECALLED", Lab.Recalled, Lab.DangerSoft),
}

/**
 * A recall with a batch number on the item is a real match (red).
 * A recall on an item without a batch number may not be the same lot, so the user should check (orange).
 */
fun foodStatus(recallReason: String?, batch: String?): FoodStatus = when {
    recallReason == null -> FoodStatus.SAFE
    batch.isNullOrBlank() -> FoodStatus.CAUTION
    else -> FoodStatus.RECALLED
}

/** An error to show on screen with one button (for example "Try again"). */
data class ScreenError(
    val title: String,
    val message: String,
    val actionLabel: String,
    val onAction: () -> Unit,
)

/** Which items the list shows. */
private enum class ItemFilter(val label: String) { ALL("All"), RECALLED("Recalled"), SAFE("Safe") }

private val CardShape = RoundedCornerShape(14.dp)

/**
 * Home screen: the foods the user is tracking, with a recall alert on top.
 * UI only, all data comes in through parameters. The filter tabs work on the items given.
 *
 * @param items the rows to show
 * @param isAdmin shows the Admin button next to the menu
 * @param loading shows a spinner instead of the list
 * @param loadingMore shows a spinner under the list while the next page loads
 * @param error shown instead of the list when not null
 * @param onDelete called with the item id when its trash button is tapped
 * @param onDeleteAccount called after the user confirms "Delete account" in the menu
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
    onDelete: (Long) -> Unit = {},
    onDeleteAccount: () -> Unit = {},
) {

    var filter by rememberSaveable { mutableStateOf(ItemFilter.ALL) }

    val recalled = items.filter { it.isRecalled }
    val shown = items.filter { item ->
        when (filter) {
            ItemFilter.ALL -> true
            ItemFilter.RECALLED -> item.isRecalled
            ItemFilter.SAFE -> !item.isRecalled
        }
    }

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
            FloatingActionButton(
                onClick = onAdd,
                shape = CircleShape,
                containerColor = Lab.TealDark,
                contentColor = Color.White,
                modifier = Modifier.navigationBarsPadding(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add food", modifier = Modifier.size(28.dp))
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item { TopBar(isAdmin = isAdmin, onAdmin = onAdmin, onSignOut = onSignOut, onDeleteAccount = onDeleteAccount) }
            item { Title(count = items.size) }
            if (!loading && error == null) item { HeroCard(recalled, items.size, onOpen) }
            item { FilterTabs(filter, items.size, recalled.size) { filter = it } }

            when {
                loading -> item { Spinner() }
                error != null -> item { ErrorCard(error) }
                items.isEmpty() -> item { EmptyCard("No foods yet", "Tap + to add the first food you want us to watch.") }
                shown.isEmpty() -> item { EmptyCard("No matches", "Try a different filter.") }
                else -> items(shown, key = { it.id }) { item -> ItemCard(item, onClick = { onOpen(item.id) }, onDelete = { onDelete(item.id) }) }
            }

            if (loadingMore) item { Spinner() }
        }
    }
}

@Composable
private fun TopBar(isAdmin: Boolean, onAdmin: () -> Unit, onSignOut: () -> Unit, onDeleteAccount: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 6.dp).height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).background(Lab.Accent, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(8.dp))
        Text(
            "BRAINWORM",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = Lab.TealDark,
            modifier = Modifier.weight(1f),
        )
        // Everyone gets the same menu. Admins also get the Admin button next to it.
        if (isAdmin) {
            TextButton(onClick = onAdmin) {
                Text("Admin", style = MaterialTheme.typography.labelLarge, color = Lab.Teal)
            }
        }
        AccountMenu(onSignOut = onSignOut, onDeleteAccount = onDeleteAccount)
    }
}

@Composable
private fun Title(count: Int) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 16.dp)) {
        Text("Is your food\nsafe today?", style = MaterialTheme.typography.displaySmall, color = Lab.Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            when (count) {
                0 -> "Add the foods you buy and we will watch the FDA for you."
                1 -> "You are tracking 1 food."
                else -> "You are tracking $count foods."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Lab.Muted,
        )
    }
}

@Composable
private fun HeroCard(recalled: List<ItemUi>, total: Int, onOpen: (Long) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val first = recalled.firstOrNull { it.status == FoodStatus.RECALLED } ?: recalled.firstOrNull()

    if (first == null) {
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).fillMaxWidth()
                .shadow(6.dp, shape).background(Lab.Surface, shape).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(44.dp).background(Lab.SafeSoft, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Lab.Safe)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("All clear", style = MaterialTheme.typography.titleMedium, color = Lab.Ink)
                Text(
                    if (total == 0) "Nothing to check yet." else "None of your foods are recalled right now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Lab.Muted,
                )
            }
        }
    } else {
        AlertHero(first, recalled.size, onOpen)
    }
}

@Composable
private fun AlertHero(first: ItemUi, count: Int, onOpen: (Long) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).fillMaxWidth()
            .shadow(10.dp, shape).background(first.status.color, shape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text(
                (if (first.status == FoodStatus.CAUTION) "POSSIBLE RECALL" else "RECALL ALERT") +
                    (if (count == 1) "" else " · $count FOODS"),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
        Text(
            first.product,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(recallDetail(first), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
        Button(
            onClick = { onOpen(first.id) },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Lab.Ink),
        ) {
            Text("See what to do", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun FilterTabs(selected: ItemFilter, total: Int, recalledCount: Int, onSelect: (ItemFilter) -> Unit) {
    Column(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("MY FOODS", style = MaterialTheme.typography.labelMedium, color = Lab.TealDark)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ItemFilter.entries.forEach { option ->
                val count = when (option) {
                    ItemFilter.ALL -> total
                    ItemFilter.RECALLED -> recalledCount
                    ItemFilter.SAFE -> total - recalledCount
                }
                val on = option == selected
                Text(
                    "${option.label} ($count)",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    color = if (on) Color.White else Lab.InkSoft,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (on) Lab.TealDark else Lab.Surface)
                        .clickable { onSelect(option) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ItemCard(item: ItemUi, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth()
            .shadow(2.dp, CardShape).clip(CardShape).background(Lab.Surface)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StatusIcon(item.status)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                item.product,
                style = MaterialTheme.typography.titleMedium,
                color = Lab.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.batch?.let { "Batch $it" } ?: "No batch number",
                style = MaterialTheme.typography.bodySmall,
                color = Lab.Muted,
            )
        }
        StatusBadge(item.status)
        DeleteButton(item.product, onDelete)
    }
}

@Composable
private fun DeleteButton(product: String, onDelete: () -> Unit) {
    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
        Icon(Icons.Filled.Delete, contentDescription = "Delete $product", tint = Lab.Muted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun StatusIcon(status: FoodStatus) {
    Box(
        Modifier.size(40.dp).background(status.softColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (status == FoodStatus.SAFE) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = status.color,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun StatusBadge(status: FoodStatus) {
    Text(
        text = status.label,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = Modifier
            .background(status.color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun EmptyCard(title: String, message: String) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth()
            .background(Lab.Surface, CardShape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Lab.Ink)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Lab.Muted)
    }
}

@Composable
private fun ErrorCard(error: ScreenError) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 5.dp).fillMaxWidth()
            .background(Lab.Surface, CardShape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(error.title.uppercase(), style = MaterialTheme.typography.labelMedium, color = Lab.Hazard)
        Text(error.message, style = MaterialTheme.typography.bodyLarge, color = Lab.Ink)
        Button(
            onClick = error.onAction,
            shape = RoundedCornerShape(10.dp),
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

private fun recallDetail(item: ItemUi): String =
    listOfNotNull(item.recallReason, item.recallDate?.let { "Recalled $it" }).joinToString(" · ")

private val previewItems = listOf(
    ItemUi(1, "Frozen chicken dumplings", "24215", recallReason = "Possible Listeria", recallDate = "Oct 5, 2026"),
    ItemUi(2, "Rolled oats", null),
    ItemUi(3, "Peanut butter, creamy", null, recallReason = "Possible Salmonella", recallDate = "Oct 8, 2026"),
    ItemUi(4, "Raw cashews, 16 oz", "RC-118", recallReason = "Possible Salmonella", recallDate = "Oct 3, 2026"),
)

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun MyItemsPreview() {
    RecallTheme {
        MyItemsScreen(previewItems, isAdmin = true, loading = false, loadingMore = false, error = null, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun MyItemsAllClearPreview() {
    RecallTheme {
        MyItemsScreen(
            previewItems.filter { !it.isRecalled }, isAdmin = false, loading = false, loadingMore = false, error = null,
            {}, {}, {}, {}, {},
        )
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
