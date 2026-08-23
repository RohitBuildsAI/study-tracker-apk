package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InteractiveMonthCalendar(
    selectedDateIso: String,
    onDateSelected: (String) -> Unit,
    studyDatesActivity: Map<String, Int>, // DateIso -> total study minutes
    modifier: Modifier = Modifier
) {
    var calendarYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var calendarMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }

    val monthName = remember(calendarYear, calendarMonth) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, calendarYear)
        cal.set(Calendar.MONTH, calendarMonth)
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    val days = remember(calendarYear, calendarMonth) {
        DateTimeUtils.getMonthDays(calendarYear, calendarMonth)
    }

    val todayIso = remember { DateTimeUtils.getTodayIsoString() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_month_calendar"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (calendarMonth == 0) {
                            calendarMonth = 11
                            calendarYear -= 1
                        } else {
                            calendarMonth -= 1
                        }
                    },
                    modifier = Modifier.testTag("prev_month_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Month"
                    )
                }

                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = {
                        if (calendarMonth == 11) {
                            calendarMonth = 0
                            calendarYear += 1
                        } else {
                            calendarMonth += 1
                        }
                    },
                    modifier = Modifier.testTag("next_month_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Month"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Day of Week Header
            val dayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dayHeaders.forEach { header ->
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid (Rows of 7)
            val chunkedDays = days.chunked(7)
            chunkedDays.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    week.forEach { dayInfo ->
                        if (dayInfo.dayOfMonth == 0) {
                            // Blank padding day
                            Spacer(modifier = Modifier.size(38.dp))
                        } else {
                            val isSelected = dayInfo.isoString == selectedDateIso
                            val isToday = dayInfo.isoString == todayIso
                            val studyMins = studyDatesActivity[dayInfo.isoString] ?: 0
                            val hasActivity = studyMins > 0

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("calendar_day_${dayInfo.isoString}")
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                        color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onDateSelected(dayInfo.isoString)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "${dayInfo.dayOfMonth}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> Color.White
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )

                                    // Activity indicator dot
                                    if (hasActivity) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) Color.White else Color(0xFF10B981)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
