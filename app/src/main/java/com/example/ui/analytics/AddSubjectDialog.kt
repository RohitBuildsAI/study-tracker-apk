package com.example.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.getSubjectIcon
import com.example.ui.components.parseColorSafe

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String, iconName: String, targetHoursPerWeek: Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#3B82F6") }
    var selectedIconName by remember { mutableStateOf("book") }
    var targetHours by remember { mutableFloatStateOf(5f) }
    var nameError by remember { mutableStateOf(false) }

    val colorPalette = listOf(
        "#3B82F6", "#10B981", "#8B5CF6", "#F59E0B",
        "#EF4444", "#06B6D4", "#EC4899", "#6366F1",
        "#14B8A6", "#F97316"
    )

    val iconOptions = listOf(
        "calculate" to "Math",
        "science" to "Science",
        "menu_book" to "Book",
        "translate" to "Language",
        "public" to "Social",
        "code" to "Tech",
        "school" to "General",
        "palette" to "Art",
        "psychology" to "Logic"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_subject_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add New Subject",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_add_subject_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("Subject Name *") },
                    placeholder = { Text("e.g. Physics, History, AI") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Subject name is required", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Color Picker Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Subject Color",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colorPalette.take(5).forEach { hex ->
                            val color = parseColorSafe(hex)
                            val isSelected = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colorPalette.drop(5).take(5).forEach { hex ->
                            val color = parseColorSafe(hex)
                            val isSelected = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }
                }

                // Icon Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Icon",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        iconOptions.take(5).forEach { (iconKey, label) ->
                            val isSelected = selectedIconName == iconKey
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) parseColorSafe(selectedColorHex).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) parseColorSafe(selectedColorHex) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .size(42.dp)
                                    .clickable { selectedIconName = iconKey }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = getSubjectIcon(iconKey),
                                        contentDescription = label,
                                        tint = if (isSelected) parseColorSafe(selectedColorHex) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Weekly Target Hours
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Weekly Target",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${targetHours.toInt()} Hours / week",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = targetHours,
                        onValueChange = { targetHours = it },
                        valueRange = 1f..25f,
                        steps = 23,
                        modifier = Modifier.testTag("weekly_target_slider")
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                                return@Button
                            }
                            onSave(name.trim(), selectedColorHex, selectedIconName, targetHours)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_subject_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Subject")
                    }
                }
            }
        }
    }
}
