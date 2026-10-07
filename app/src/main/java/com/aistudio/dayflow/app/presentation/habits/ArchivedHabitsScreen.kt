package com.aistudio.dayflow.app.presentation.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.dayflow.app.domain.usecase.GetArchivedHabitsUseCase
import com.aistudio.dayflow.app.domain.usecase.RestoreHabitUseCase
import com.aistudio.dayflow.app.presentation.common.DayFlowCard
import com.aistudio.dayflow.app.presentation.common.EmptyStateView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedHabitsScreen(
    getArchivedHabitsUseCase: GetArchivedHabitsUseCase,
    restoreHabitUseCase: RestoreHabitUseCase,
    onNavigateBack: () -> Unit
) {
    val habits by getArchivedHabitsUseCase().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Archived Habits",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (habits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    icon = Icons.Filled.Archive,
                    title = "No archived habits",
                    description = "Archived habits preserve their completion history and can be restored here."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(habits, key = { it.id }) { habit ->
                    DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(onClick = {
                                scope.launch {
                                    restoreHabitUseCase(habit.id)
                                    val reminderTime = habit.reminderTime
                                    if (reminderTime != null) {
                                        com.aistudio.dayflow.app.notifications.HabitReminderScheduler.scheduleReminder(
                                            context = context,
                                            habitId = habit.id,
                                            habitName = habit.name,
                                            frequencyType = habit.frequencyType,
                                            selectedDays = habit.selectedDays,
                                            reminderTime = reminderTime,
                                            targetDesc = "${habit.targetCount} ${habit.targetUnit}"
                                        )
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Unarchive,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text("Restore")
                            }
                        }
                    }
                }
            }
        }
    }
}
