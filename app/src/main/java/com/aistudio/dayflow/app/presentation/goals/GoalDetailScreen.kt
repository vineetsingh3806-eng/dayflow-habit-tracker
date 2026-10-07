package com.aistudio.dayflow.app.presentation.goals

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.model.Milestone
import com.aistudio.dayflow.app.domain.repository.GoalRepository
import com.aistudio.dayflow.app.domain.repository.HabitRepository
import com.aistudio.dayflow.app.domain.usecase.ArchiveGoalUseCase
import com.aistudio.dayflow.app.domain.usecase.CreateMilestoneUseCase
import com.aistudio.dayflow.app.domain.usecase.DeleteGoalUseCase
import com.aistudio.dayflow.app.domain.usecase.DeleteMilestoneUseCase
import com.aistudio.dayflow.app.domain.usecase.GetGoalWithDetailsUseCase
import com.aistudio.dayflow.app.domain.usecase.GoalProgressCalculator
import com.aistudio.dayflow.app.domain.usecase.LinkHabitToMilestoneUseCase
import com.aistudio.dayflow.app.domain.usecase.ToggleMilestoneCompletionUseCase
import com.aistudio.dayflow.app.domain.usecase.UnlinkHabitFromMilestoneUseCase
import com.aistudio.dayflow.app.presentation.common.DayFlowCard
import com.aistudio.dayflow.app.presentation.common.HabitIcons
import com.aistudio.dayflow.app.ui.theme.SoftGreen
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: Long,
    getGoalWithDetailsUseCase: GetGoalWithDetailsUseCase,
    toggleMilestoneCompletionUseCase: ToggleMilestoneCompletionUseCase,
    createMilestoneUseCase: CreateMilestoneUseCase,
    deleteMilestoneUseCase: DeleteMilestoneUseCase,
    linkHabitToMilestoneUseCase: LinkHabitToMilestoneUseCase,
    unlinkHabitFromMilestoneUseCase: UnlinkHabitFromMilestoneUseCase,
    deleteGoalUseCase: DeleteGoalUseCase,
    archiveGoalUseCase: ArchiveGoalUseCase,
    goalRepository: GoalRepository,
    habitRepository: HabitRepository,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val goalWithDetails by getGoalWithDetailsUseCase(goalId).collectAsStateWithLifecycle(initialValue = null)
    val allActiveHabits by habitRepository.getActiveHabits().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var showAddMilestoneDialog by remember { mutableStateOf(false) }
    var newMilestoneText by remember { mutableStateOf("") }
    var showLinkHabitDialog by remember { mutableStateOf(false) }

    if (goalWithDetails == null) {
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

    val details = goalWithDetails ?: return
    val goal = details.goal
    val milestones = details.milestones
    val progress = GoalProgressCalculator.calculateProgress(details)
    val deadlineDescription = GoalProgressCalculator.getDeadlineDescription(goal, LocalDate.now())

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(goalId) }) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit goal")
                    }
                    IconButton(onClick = { showArchiveConfirm = true }) {
                        Icon(imageVector = Icons.Filled.Archive, contentDescription = "Archive goal")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete goal",
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
            // Goal Title & Target Date
            Column {
                Text(
                    text = "GOAL",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "🎯 ${goal.title}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (goal.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = goal.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress Card
            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OVERALL PROGRESS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${(progress * 100).toInt()}% complete",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = deadlineDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Milestones Section
            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MILESTONES",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Steps to achieve your goal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showAddMilestoneDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add milestone",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (milestones.isEmpty()) {
                        Text(
                            text = "No milestones yet. Break this goal into steps to track progress.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        milestones.forEach { milestone ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = milestone.completed,
                                    onCheckedChange = {
                                        val willBeCompleted = !milestone.completed
                                        scope.launch {
                                            toggleMilestoneCompletionUseCase(milestone.id, milestone.completed)
                                            // Recalculate progress
                                            val updatedCount = milestones.count { if (it.id == milestone.id) willBeCompleted else it.completed }
                                            val newProg = (updatedCount.toFloat() / milestones.size.toFloat()).coerceIn(0f, 1f)
                                            goalRepository.updateGoalProgress(goalId, newProg)

                                            if (willBeCompleted) {
                                                snackbarHostState.showSnackbar("Great job! 🎉")
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = milestone.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = if (milestone.completed) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (milestone.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            deleteMilestoneUseCase(milestone.id)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete milestone",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Linked Habits Section
            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LINKED HABITS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Daily habits contributing to this goal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showLinkHabitDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Link,
                                contentDescription = "Link habit",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val linkedHabits = allActiveHabits.filter { details.linkedHabitIds.contains(it.id) }
                    if (linkedHabits.isEmpty()) {
                        Text(
                            text = "Link daily habits to this goal to build daily momentum toward your milestone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        linkedHabits.forEach { habit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val habitColor = try {
                                    Color(android.graphics.Color.parseColor(habit.colorHex))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(habitColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = HabitIcons.getIcon(habit.icon),
                                        contentDescription = null,
                                        tint = habitColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = habit.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            unlinkHabitFromMilestoneUseCase(habit.id, goalId)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LinkOff,
                                        contentDescription = "Unlink habit",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add Milestone Dialog
    if (showAddMilestoneDialog) {
        AlertDialog(
            onDismissRequest = { showAddMilestoneDialog = false },
            title = { Text("Add Milestone") },
            text = {
                OutlinedTextField(
                    value = newMilestoneText,
                    onValueChange = { newMilestoneText = it },
                    label = { Text("Milestone title") },
                    placeholder = { Text("e.g. Complete chapter 1-5") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMilestoneText.trim().isNotEmpty()) {
                            scope.launch {
                                createMilestoneUseCase(
                                    Milestone(
                                        goalId = goalId,
                                        title = newMilestoneText.trim(),
                                        order = milestones.size + 1
                                    )
                                )
                                newMilestoneText = ""
                                showAddMilestoneDialog = false
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMilestoneDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Link Habit Dialog
    if (showLinkHabitDialog) {
        val unlinkedHabits = allActiveHabits.filter { !details.linkedHabitIds.contains(it.id) }
        AlertDialog(
            onDismissRequest = { showLinkHabitDialog = false },
            title = { Text("Link a Habit") },
            text = {
                if (unlinkedHabits.isEmpty()) {
                    Text("All active habits are already linked or no habits exist.")
                } else {
                    Column {
                        unlinkedHabits.forEach { habit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            val targetMilestoneId = milestones.firstOrNull()?.id ?: 0L
                                            linkHabitToMilestoneUseCase(habit.id, targetMilestoneId, goalId)
                                            showLinkHabitDialog = false
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = HabitIcons.getIcon(habit.icon),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = habit.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLinkHabitDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Archive Confirmation Dialog
    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text("Archive Goal?") },
            text = { Text("This will hide this goal.") },
            confirmButton = {
                Button(onClick = {
                    showArchiveConfirm = false
                    scope.launch {
                        archiveGoalUseCase(goalId)
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
            title = { Text("Delete Goal?") },
            text = { Text("This will remove this goal.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            deleteGoalUseCase(goalId)
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
