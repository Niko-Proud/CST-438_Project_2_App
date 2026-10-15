package com.example.recallapp.ui.items
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val product: String, val batch: String) : DetailUiState()
    data class Error(val statusCode: Int, val message: String) : DetailUiState()
}

@Composable
fun RegistryDetailScreen(
    uiState: DetailUiState,
    isCreatingNew: Boolean,
    onSaveClick: (product: String, batch: String) -> Unit,
    onDeleteClick: () -> Unit
) {
    var productInput by remember { mutableStateOf("") }
    var batchInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is DetailUiState.Success) {
            productInput = uiState.product
            batchInput = uiState.batch
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        when (uiState) {
            is DetailUiState.Loading -> {
                CircularProgressIndicator()
            }
            is DetailUiState.Error -> {
                Text(
                    text = "Error ${uiState.statusCode}: ${uiState.message}",
                    color = Color.Red,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            is DetailUiState.Success, DetailUiState.Loading -> {
                OutlinedTextField(
                    value = productInput,
                    onValueChange = { productInput = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = batchInput,
                    onValueChange = { batchInput = it },
                    label = { Text("Batch Number (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { onSaveClick(productInput, batchInput) }) {
                        Text(if (isCreatingNew) "Add to Registry" else "Save Changes")
                    }
                    if (!isCreatingNew) {
                        Button(onClick = onDeleteClick, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}