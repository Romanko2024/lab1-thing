package com.example.lab1_thing.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lab1_thing.model.Artist
import com.example.lab1_thing.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDialog(
    initialTrack: Track? = null,
    artists: List<Artist>,
    onDismiss: () -> Unit,
    onSave: (String, Int, Artist?) -> Unit
) {
    var title by remember { mutableStateOf(initialTrack?.title ?: "") }
    var duration by remember { mutableStateOf(initialTrack?.durationSec?.toString() ?: "") }
    var selectedArtist by remember { mutableStateOf(initialTrack?.artist) }

    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTrack == null) "Додати трек" else "Редагувати трек") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Назва") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter { c -> c.isDigit() } },
                    label = { Text("Тривалість (сек)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                // Вибір артиста
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedArtist?.name ?: "Без артиста",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Артист") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Без артиста") },
                            onClick = {
                                selectedArtist = null
                                expanded = false
                            }
                        )
                        artists.forEach { artist ->
                            DropdownMenuItem(
                                text = { Text(artist.name) },
                                onClick = {
                                    selectedArtist = artist
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val durationInt = duration.toIntOrNull() ?: 0
                onSave(title, durationInt, selectedArtist)
            }) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    )
}