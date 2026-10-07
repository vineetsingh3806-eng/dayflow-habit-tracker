package com.aistudio.dayflow.app.presentation.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.dayflow.app.domain.model.FrequencyType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.model.HabitCompletion
import com.aistudio.dayflow.app.domain.usecase.ArchiveHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.DeleteHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.GetHabitByIdUseCase
import com.aistudio.dayflow.app.domain.usecase.GetHabitCompletionsUseCase
import com.aistudio.dayflow.app.domain.usecase.RestoreHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.StreakCalculator
import com.aistudio.dayflow.app.domain.usecase.ToggleHabitCompletionUseCase
import com.aistudio.dayflow.app.presentation.common.DayFlowCard
import com.aistudio.dayflow.app.presentation.common.HabitIcons
import com.aistudio.dayflow.app.ui.theme.SoftGreen
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: Long,
    getHabitByIdUseCase: GetHabitByIdUseCase,
    getHabitCompletionsUseCase: GetHabitCompletionsUseCase,
    toggleHabitCompletionUseCase: ToggleHabitCompletionUseCase,
    archiveHabitUseCase: ArchiveHabitUseCase,
    restoreHabitUseCase: RestoreHabitUseCase,
    deleteHabitUseCase: DeleteHabitUseCase,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val habit by getHabitByIdUseCase(habitId).collectAsStateWithLifecycle(initialValue = null)
    val completions by getHabitCompletionsUseCase(habitId).collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val isCompletedToday = completions.any { it.date == today && it.status == CompletionStatus.COMPLETED }

    val streakInfo = habit?.let {
        StreakCalculator.calculateStreak(it, completions, today)
    }

    if (habit == null) {
        Scaffold { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...")
            }
        }
        return
    }

    val currentHabit = habit ?: return
    val habitColor = try {
        Color(android.graphics.Color.parseColor(currentHabit.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(habitId) }) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit habit")
                    }
                    if (currentHabit.isArchived) {
                        IconButton(onClick = {
                            scope.launch {
                                restoreHabitUseCase(habitId)
                                onNavigateBack()
                            }
                        }) {
                            Icon(imageVector = Icons.Filled.Unarchive, contentDescription = "Restore habit")
                        }
                    } else {
                        IconButton(onClick = { showArchiveConfirm = true }) {
                            Icon(imageVector = Icons.Filled.Archive, contentDescription = "Archive habit")
                        }
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete habit",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Icon + Name + Streak
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(habitColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = HabitIcons.getIcon(currentHabit.icon),
                        contentDescription = null,
                        tint = habitColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = currentHabit.name,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF7ED))
                                .border(1.dp, Color(0xFFFED7AA), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFC2410C),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "🔥 ${streakInfo?.currentStreak ?: 0} day streak",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFC2410C)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Best: ${streakInfo?.bestStreak ?: 0} days",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isCompletedToday && (streakInfo?.currentStreak ?: 0) > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🎉 Streak extended today!",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFB45309)
                        )
                    }
                    if (currentHabit.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentHabit.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Today's Action Button
            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Today:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                toggleHabitCompletionUseCase(
                                    habitId = habitId,
                                    date = today,
                                    isCurrentlyCompleted = isCompletedToday
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompletedToday) SoftGreen else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCompletedToday) "✓ Completed" else "○ Mark Complete",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Schedule & Reminder Details
            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val frequencyLabel = when (currentHabit.frequencyType) {
                        FrequencyType.DAILY -> "Every day"
                        FrequencyType.SELECTED_DAYS -> {
                            if (currentHabit.selectedDays.size == 1) {
                                "Every " + currentHabit.selectedDays.first().getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                            } else {
                                val days = currentHabit.selectedDays.map { it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } }.joinToString(", ")
                                "Every $days"
                            }
                        }
                        FrequencyType.WEEKDAYS -> "Weekdays"
                        FrequencyType.TIMES_PER_WEEK -> "${currentHabit.targetTimesPerWeek} times/week"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Frequency:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(frequencyLabel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    val reminder = currentHabit.reminderTime
                    if (reminder != null) {
                        val formattedReminder = reminder.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
                        val nextReminderLabel = when (currentHabit.frequencyType) {
                            FrequencyType.DAILY -> "Every day, $formattedReminder"
                            FrequencyType.SELECTED_DAYS -> {
                                if (currentHabit.selectedDays.size == 1) {
                                    val dayName = currentHabit.selectedDays.first().getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                                    "$dayName, $formattedReminder"
                                } else {
                                    "Scheduled days, $formattedReminder"
                                }
                            }
                            FrequencyType.WEEKDAYS -> "Weekdays, $formattedReminder"
                            FrequencyType.TIMES_PER_WEEK -> "Target days, $formattedReminder"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Next reminder:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                nextReminderLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (currentHabit.targetCount > 1 || currentHabit.targetUnit != "times") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Daily target:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${currentHabit.targetCount} ${currentHabit.targetUnit}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // 30-Day Calendar History
            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "PAST 30 DAYS HISTORY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val completedDates = completions
                        .filter { it.status == CompletionStatus.COMPLETED }
                        .map { it.date }
                        .toSet()

                    // Last 28 days as 4 weeks of 7 days
                    val daysList = (0..27).map { today.minusDays(27L - it) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        daysList.chunked(7).forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                week.forEach { date ->
                                    val isCompleted = completedDates.contains(date)
                                    val isScheduled = StreakCalculator.isHabitScheduledOnDate(currentHabit, date)
                                    val isCurrentDay = date == today

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isCompleted -> SoftGreen
                                                    isScheduled && date.isBefore(today) -> MaterialTheme.colorScheme.surfaceVariant
                                                    else -> MaterialTheme.colorScheme.surface
                                                }
                                            )
                                            .border(
                                                width = if (isCurrentDay) 2.dp else 1.dp,
                                                color = if (isCurrentDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCompleted) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "${date.dayOfMonth}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = if (isScheduled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Archive Confirmation Dialog
    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text("Archive Habit?") },
            text = { Text("This will hide this habit.") },
            confirmButton = {
                Button(onClick = {
                    showArchiveConfirm = false
                    com.aistudio.dayflow.app.notifications.HabitReminderScheduler.cancelReminder(context, habitId, reason = "habit_archived_by_user")
                    scope.launch {
                        archiveHabitUseCase(habitId)
                        onNavigateBack()
                    }
                }) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Habit?") },
            text = { Text("This will remove this habit.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        com.aistudio.dayflow.app.notifications.HabitReminderScheduler.cancelReminder(context, habitId, reason = "habit_deleted_by_user")
                        scope.launch {
                            deleteHabitUseCase(habitId)
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
