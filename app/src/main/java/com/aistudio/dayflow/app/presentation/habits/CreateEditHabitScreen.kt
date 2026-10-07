package com.aistudio.dayflow.app.presentation.habits

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aistudio.dayflow.app.domain.model.FrequencyType
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.usecase.CreateHabitUseCase
import com.aistudio.dayflow.app.domain.usecase.GetHabitByIdUseCase
import com.aistudio.dayflow.app.domain.usecase.UpdateHabitUseCase
import com.aistudio.dayflow.app.notifications.HabitReminderScheduler
import com.aistudio.dayflow.app.presentation.common.DayFlowCard
import com.aistudio.dayflow.app.presentation.common.DayFlowColorPalette
import com.aistudio.dayflow.app.presentation.common.HabitIcons
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEditHabitScreen(
    habitId: Long?,
    getHabitByIdUseCase: GetHabitByIdUseCase,
    createHabitUseCase: CreateHabitUseCase,
    updateHabitUseCase: UpdateHabitUseCase,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("check_circle") }
    var selectedColor by remember { mutableStateOf(DayFlowColorPalette.first()) }
    var frequencyType by remember { mutableStateOf(FrequencyType.DAILY) }
    var selectedDays by remember { mutableStateOf(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)) }
    var targetCountText by remember { mutableStateOf("1") }
    var targetUnit by remember { mutableStateOf("times") }
    var hasReminder by remember { mutableStateOf(false) }
    var reminderHour by remember { mutableIntStateOf(9) }
    var reminderMinute by remember { mutableIntStateOf(0) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Load existing habit if editing
    LaunchedEffect(habitId) {
        if (habitId != null && habitId > 0) {
            val existing = getHabitByIdUseCase.getOnce(habitId)
            if (existing != null) {
                name = existing.name
                description = existing.description
                selectedIcon = existing.icon
                selectedColor = existing.colorHex
                frequencyType = existing.frequencyType
                selectedDays = existing.selectedDays
                targetCountText = existing.targetCount.toString()
                targetUnit = existing.targetUnit
                if (existing.reminderTime != null) {
                    hasReminder = true
                    reminderHour = existing.reminderTime.hour
                    reminderMinute = existing.reminderTime.minute
                }
            }
        }
    }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            reminderHour = hourOfDay
            reminderMinute = minute
        },
        reminderHour,
        reminderMinute,
        false
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* system notification permission result */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (habitId != null && habitId > 0) "Edit Habit" else "New Habit",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Habit Name & Description
            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (nameError != null) nameError = null
                        },
                        label = { Text("Habit name") },
                        placeholder = { Text("e.g. Read for 20 mins, Drink water") },
                        isError = nameError != null,
                        supportingText = { nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Notes or Motivation (Optional)") },
                        placeholder = { Text("Why this habit matters to your goals") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Icon Picker
            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CHOOSE ICON",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HabitIcons.iconMap.forEach { (iconKey, iconVector) ->
                            val isSelected = selectedIcon == iconKey
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedIcon = iconKey },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = iconKey,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Color Picker
            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "THEME COLOR",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DayFlowColorPalette.forEach { hex ->
                            val isSelected = selectedColor == hex
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected color",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Repeat / Frequency
            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "How often?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            FrequencyType.DAILY to "Every day",
                            FrequencyType.SELECTED_DAYS to "Selected days"
                        ).forEach { (type, label) ->
                            FilterChip(
                                selected = frequencyType == type,
                                onClick = { frequencyType = type },
                                label = { Text(label, fontWeight = FontWeight.Medium) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    if (frequencyType == FrequencyType.SELECTED_DAYS) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Days:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val daysOfWeekList = listOf(
                            DayOfWeek.MONDAY to "Mon",
                            DayOfWeek.TUESDAY to "Tue",
                            DayOfWeek.WEDNESDAY to "Wed",
                            DayOfWeek.THURSDAY to "Thu",
                            DayOfWeek.FRIDAY to "Fri",
                            DayOfWeek.SATURDAY to "Sat",
                            DayOfWeek.SUNDAY to "Sun"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            daysOfWeekList.forEach { (dow, abbrev) ->
                                val isSelected = selectedDays.contains(dow)
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedDays = if (isSelected) {
                                                if (selectedDays.size > 1) selectedDays - dow else selectedDays
                                            } else {
                                                selectedDays + dow
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = abbrev,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Target (Optional)
            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "DAILY TARGET (OPTIONAL)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = targetCountText,
                            onValueChange = { targetCountText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Count") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = targetUnit,
                            onValueChange = { targetUnit = it },
                            label = { Text("Unit") },
                            placeholder = { Text("mins, glasses, pages") },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Reminder (Local Notification)
            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Reminder",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Gentle local notification",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = hasReminder,
                            onCheckedChange = { checked ->
                                hasReminder = checked
                                if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            }
                        )
                    }

                    if (hasReminder) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val formattedTime = LocalTime.of(reminderHour, reminderMinute)
                            .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { timePickerDialog.show() }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Time",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Explicit recurring reminder explanation badge
                        Spacer(modifier = Modifier.height(12.dp))
                        val recurringRepeatText = when (frequencyType) {
                            FrequencyType.DAILY -> "Every day at $formattedTime"
                            FrequencyType.SELECTED_DAYS -> {
                                if (selectedDays.isEmpty()) {
                                    "Select at least one day"
                                } else if (selectedDays.size == 7) {
                                    "Every day at $formattedTime"
                                } else if (selectedDays.size == 1) {
                                    val singleDay = selectedDays.first().getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                                    "Every $singleDay at $formattedTime"
                                } else {
                                    val daysOfWeekOrder = listOf(
                                        DayOfWeek.MONDAY to "Mon",
                                        DayOfWeek.TUESDAY to "Tue",
                                        DayOfWeek.WEDNESDAY to "Wed",
                                        DayOfWeek.THURSDAY to "Thu",
                                        DayOfWeek.FRIDAY to "Fri",
                                        DayOfWeek.SATURDAY to "Sat",
                                        DayOfWeek.SUNDAY to "Sun"
                                    )
                                    val labels = daysOfWeekOrder
                                        .filter { selectedDays.contains(it.first) }
                                        .joinToString(", ") { it.second }
                                    "Every $labels at $formattedTime"
                                }
                            }
                            FrequencyType.WEEKDAYS -> "Every weekday at $formattedTime"
                            FrequencyType.TIMES_PER_WEEK -> "Every scheduled day at $formattedTime"
                        }

                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = recurringRepeatText,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Repeats every week automatically.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        nameError = "Habit name cannot be blank"
                        return@Button
                    }

                    isSaving = true
                    val targetCount = (targetCountText.toIntOrNull() ?: 1).coerceAtLeast(1)
                    val reminderTime = if (hasReminder) LocalTime.of(reminderHour, reminderMinute) else null

                    val habitToSave = Habit(
                        id = habitId ?: 0L,
                        name = name.trim(),
                        description = description.trim(),
                        icon = selectedIcon,
                        colorHex = selectedColor,
                        frequencyType = frequencyType,
                        selectedDays = if (frequencyType == FrequencyType.SELECTED_DAYS) selectedDays else emptySet(),
                        targetCount = targetCount,
                        targetUnit = targetUnit.ifBlank { "times" },
                        reminderTime = reminderTime
                    )

                    scope.launch {
                        if (habitId != null && habitId > 0) {
                            updateHabitUseCase(habitToSave)
                            if (reminderTime != null) {
                                HabitReminderScheduler.scheduleReminder(
                                    context = context,
                                    habitId = habitId,
                                    habitName = habitToSave.name,
                                    frequencyType = habitToSave.frequencyType,
                                    selectedDays = habitToSave.selectedDays,
                                    reminderTime = reminderTime,
                                    targetDesc = "${habitToSave.targetCount} ${habitToSave.targetUnit}"
                                )
                            } else {
                                HabitReminderScheduler.cancelReminder(context, habitId, reason = "reminder_removed_by_user")
                            }
                        } else {
                            val newId = createHabitUseCase(habitToSave).getOrNull()
                            if (newId != null && reminderTime != null) {
                                HabitReminderScheduler.scheduleReminder(
                                    context = context,
                                    habitId = newId,
                                    habitName = habitToSave.name,
                                    frequencyType = habitToSave.frequencyType,
                                    selectedDays = habitToSave.selectedDays,
                                    reminderTime = reminderTime,
                                    targetDesc = "${habitToSave.targetCount} ${habitToSave.targetUnit}"
                                )
                            }
                        }
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !isSaving
            ) {
                Text(
                    text = if (habitId != null && habitId > 0) "Save Changes" else "Save Habit",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
