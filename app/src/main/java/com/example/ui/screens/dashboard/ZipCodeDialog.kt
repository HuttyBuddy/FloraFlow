package com.example.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ZipCodeDialog(
    initialZip: String,
    onDismiss: () -> Unit,
    onUpdate: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .padding(16.dp)
                .testTag("change_zip_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Update Location Zip Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                var zipInput by remember { mutableStateOf(initialZip) }

                OutlinedTextField(
                    value = zipInput,
                    onValueChange = { zipInput = it },
                    label = { Text("Zip Code") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("zip_code_input"),
                    placeholder = { Text("e.g. 90210") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (zipInput.isNotBlank()) {
                                onUpdate(zipInput.trim())
                            }
                        },
                        enabled = zipInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_zip_code")
                    ) {
                        Text("Update")
                    }
                }
            }
        }
    }
}
