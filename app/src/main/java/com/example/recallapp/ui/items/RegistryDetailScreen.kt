package com.example.recallapp.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recallapp.ui.theme.Lab
import com.example.recallapp.ui.theme.RecallTheme

sealed class DetailUiState {
    object Loading : DetailUiState()

    /** recallReason and recallDate are null when no recall matches the item. */
    data class Success(
        val product: String,
        val batch: String,
        val recallReason: String? = null,
        val recallDate: String? = null,
    ) : DetailUiState()

    data class Error(val statusCode: Int, val message: String) : DetailUiState()
}

private val BoxShape = RoundedCornerShape(14.dp)
private val SoftBox = Color(0xFFF5F7FA)

/**
 * Detail screen for one registry item: recall status and food info first, with Edit to change it.
 * When isCreatingNew is true it is an empty form for adding a new item. Deleting is done from the list.
 */
@Composable
fun RegistryDetailScreen(
    uiState: DetailUiState,
    isCreatingNew: Boolean,
    onSaveClick: (product: String, batch: String) -> Unit,
    onDeleteClick: () -> Unit,
    onBack: () -> Unit = {},
) {
    var productInput by remember { mutableStateOf("") }
    var batchInput by remember { mutableStateOf("") }
    // New items open as a form. Saved items open as information, and Edit switches to the form.
    var editing by remember(isCreatingNew) { mutableStateOf(isCreatingNew) }

    LaunchedEffect(uiState) {
        if (uiState is DetailUiState.Success) {
            productInput = uiState.product
            batchInput = uiState.batch
        }
    }

    Column(Modifier.fillMaxSize().background(Lab.Surface).systemBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Lab.Ink)
            }
            Text(
                if (isCreatingNew) "Add a food" else "Food details",
                style = MaterialTheme.typography.titleMedium,
                color = Lab.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(48.dp))
        }

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (uiState) {
                is DetailUiState.Loading -> {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Lab.Teal)
                    }
                }
                is DetailUiState.Error -> {
                    Section(title = "SOMETHING WENT WRONG", color = Lab.DangerSoft) {
                        Text(
                            text = if (uiState.statusCode > 0) "Error ${uiState.statusCode}: ${uiState.message}" else uiState.message,
                            color = Lab.Hazard,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                        Text("Go back", color = Lab.Ink, style = MaterialTheme.typography.labelLarge)
                    }
                }
                is DetailUiState.Success -> {
                    if (isCreatingNew) {
                        Text(
                            "Type the food name. Add the batch number if you have it, so the match is more exact.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Lab.Muted,
                        )
                    } else {
                        RecallSection(uiState.recallReason, uiState.recallDate, uiState.batch)
                    }

                    if (editing) {
                        Section(title = "FOOD INFO", color = SoftBox) {
                            FormField("Product Name", productInput) { productInput = it }
                            FormField("Batch Number (Optional)", batchInput) { batchInput = it }
                            Text(
                                "The batch number is usually printed near the date on the package.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Lab.Muted,
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { onSaveClick(productInput.trim(), batchInput.trim()) },
                            enabled = productInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Lab.TealDark, contentColor = Color.White),
                        ) {
                            Text(if (isCreatingNew) "Add to Registry" else "Save", style = MaterialTheme.typography.labelLarge)
                        }
                        if (!isCreatingNew) {
                            TextButton(
                                onClick = {
                                    productInput = uiState.product
                                    batchInput = uiState.batch
                                    editing = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Cancel", color = Lab.Muted, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    } else {
                        Section(title = "FOOD INFO", color = SoftBox) {
                            InfoRow("Product", uiState.product)
                            HorizontalDivider(color = Lab.Line)
                            InfoRow("Batch", uiState.batch.ifBlank { "Not added" })
                        }
                        // Editing is still possible (part of the detail screen), but the screen opens as information first.
                        TextButton(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Edit", color = Lab.Teal, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

/** Recall status in a soft box: green safe, orange check the batch, red recalled. */
@Composable
private fun RecallSection(recallReason: String?, recallDate: String?, batch: String) {
    val status = foodStatus(recallReason, batch)
    val title = when (status) {
        FoodStatus.SAFE -> "No recall found"
        FoodStatus.CAUTION -> "Possible recall"
        FoodStatus.RECALLED -> "Recalled"
    }
    val subtitle = when (status) {
        FoodStatus.SAFE -> "We will tell you if one comes up."
        FoodStatus.CAUTION -> "Check the batch number on your package."
        FoodStatus.RECALLED -> "Do not eat this food."
    }
    Section(title = "RECALL STATUS", color = status.softColor) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(36.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    if (status == FoodStatus.SAFE) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = status.color,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = status.color)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Lab.Muted)
            }
        }
        if (status != FoodStatus.SAFE) {
            HorizontalDivider(color = Lab.Line)
            InfoRow("Reason", recallReason ?: "")
            HorizontalDivider(color = Lab.Line)
            InfoRow("Recall date", recallDate ?: "Unknown")
            HorizontalDivider(color = Lab.Line)
            InfoRow("What to do", if (status == FoodStatus.CAUTION) "Compare the batch" else "Throw it away or return it")
        }
    }
}

/** A soft rounded box with a small title. */
@Composable
private fun Section(title: String, color: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = Lab.Muted, modifier = Modifier.padding(start = 4.dp))
        Column(
            Modifier.fillMaxWidth().background(color, BoxShape).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** One "label  value" line inside a section. */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Lab.Muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = Lab.Ink, textAlign = TextAlign.End)
    }
}

@Composable
private fun FormField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Lab.Surface,
            unfocusedContainerColor = Lab.Surface,
            focusedBorderColor = Lab.Teal,
            unfocusedBorderColor = Lab.Line,
            focusedLabelColor = Lab.Teal,
            cursorColor = Lab.Teal,
        ),
    )
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun RecalledItemPreview() {
    RecallTheme {
        RegistryDetailScreen(
            uiState = DetailUiState.Success("Frozen dumplings", "24215", "Listeria", "2026-10-05"),
            isCreatingNew = false,
            onSaveClick = { _, _ -> },
            onDeleteClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SafeItemPreview() {
    RecallTheme {
        RegistryDetailScreen(
            uiState = DetailUiState.Success("Rolled oats", ""),
            isCreatingNew = false,
            onSaveClick = { _, _ -> },
            onDeleteClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun CautionItemPreview() {
    RecallTheme {
        RegistryDetailScreen(
            uiState = DetailUiState.Success("Peanut butter", "", "Salmonella", "2026-10-08"),
            isCreatingNew = false,
            onSaveClick = { _, _ -> },
            onDeleteClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun NewItemPreview() {
    RecallTheme {
        RegistryDetailScreen(
            uiState = DetailUiState.Success("", ""),
            isCreatingNew = true,
            onSaveClick = { _, _ -> },
            onDeleteClick = {},
        )
    }
}
