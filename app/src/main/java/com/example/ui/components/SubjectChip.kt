package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Subject

fun parseColorSafe(hex: String, defaultColor: Color = Color(0xFF4F46E5)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        defaultColor
    }
}

fun getSubjectIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "calculate", "math" -> Icons.Default.Calculate
        "science", "flask" -> Icons.Default.Science
        "menu_book", "book", "english" -> Icons.Default.MenuBook
        "translate", "kannada", "language" -> Icons.Default.Translate
        "public", "globe", "social" -> Icons.Default.Public
        "code", "laptop", "cs" -> Icons.Default.Code
        "school" -> Icons.Default.School
        "palette", "art" -> Icons.Default.Palette
        "psychology", "mind" -> Icons.Default.Psychology
        "music_note", "music" -> Icons.Default.MusicNote
        else -> Icons.Default.Book
    }
}

@Composable
fun SubjectBadge(
    subjectName: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    iconName: String = "book"
) {
    val color = parseColorSafe(colorHex)
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = subjectName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun SubjectSelectableChip(
    subject: Subject,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = parseColorSafe(subject.colorHex)
    val backgroundColor = if (isSelected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant
    val textColor = if (isSelected) color else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .testTag("subject_chip_${subject.id}")
            .clip(CircleShape)
            .background(backgroundColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, CircleShape)
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = getSubjectIcon(subject.iconName),
            contentDescription = subject.name,
            tint = if (isSelected) color else textColor.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = subject.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}
