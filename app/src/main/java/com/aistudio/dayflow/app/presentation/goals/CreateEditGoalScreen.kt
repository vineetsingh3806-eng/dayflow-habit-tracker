package com.aistudio.dayflow.app.presentation.goals

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.dayflow.app.domain.model.Goal
import com.aistudio.dayflow.app.domain.model.Milestone
import com.aistudio.dayflow.app.domain.repository.GoalRepository
import com.aistudio.dayflow.app.domain.usecase.CreateGoalUseCase
import com.aistudio.dayflow.app.domain.usecase.CreateMilestoneUseCase
import com.aistudio.dayflow.app.domain.usecase.UpdateGoalUseCase
import com.aistudio.dayflow.app.presentation.common.DayFlowCard
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditGoalScreen(
    goalId: Long?,
    goalRepository: GoalRepository,
    createGoalUseCase: CreateGoalUseCase,
    updateGoalUseCase: UpdateGoalUseCase,
    createMilestoneUseCase: CreateMilestoneUseCase,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf<LocalDate?>(LocalDate.now().plusMonths(1)) }
    var newMilestoneText by remember { mutableStateOf("") }
    val initialMilestones = remember { mutableStateListOf<String>() }

    var titleError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(goalId) {
        if (goalId != null && goalId > 0) {
            val existing = goalRepository.getGoalByIdOnce(goalId)
            if (existing != null) {
                title = existing.title
                description = existing.description
                targetDate = existing.targetDate
            }
        }
    }

    val datePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selected = LocalDate.of(year, month + 1, dayOfMonth)
            if (selected.isBefore(LocalDate.now())) {
                dateError = "Target date cannot be in the past"
            } else {
                dateError = null
                targetDate = selected
            }
        },
        targetDate?.year ?: LocalDate.now().year,
        (targetDate?.monthValue ?: LocalDate.now().monthValue) - 1,
        targetDate?.dayOfMonth ?: LocalDate.now().dayOfMonth
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (goalId != null && goalId > 0) "Edit Goal" else "New Goal",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title & Description
            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (titleError != null) titleError = null
                        },
                        label = { Text("Goal Title") },
                        placeholder = { Text("e.g. Run 10k, Read 12 Books") },
                        isError = titleError != null,
                        supportingText = { titleError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Why (Optional)") },
                        placeholder = { Text("What outcome will this achieve?") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Target Date
            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "TARGET DEADLINE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { datePicker.show() }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = targetDate?.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault()))
                                    ?: "Select deadline",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        TextButton(onClick = { datePicker.show() }) {
                            Text("Change")
                        }
                    }
                    dateError?.let { err ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Milestones Builder (if creating new goal)
            if (goalId == null || goalId == 0L) {
                DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "MILESTONES (BREAK INTO STEPS)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newMilestoneText,
                                onValueChange = { newMilestoneText = it },
                                placeholder = { Text("Add milestone step...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (newMilestoneText.trim().isNotEmpty()) {
                                        initialMilestones.add(newMilestoneText.trim())
                                        newMilestoneText = ""
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add milestone",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (initialMilestones.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            initialMilestones.forEachIndexed { index, mTitle ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}. ",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = mTitle,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    IconButton(
                                        onClick = { initialMilestones.removeAt(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (title.trim().isEmpty()) {
                        titleError = "Goal title cannot be blank"
                        return@Button
                    }
                    val target = targetDate
                    if (target != null && target.isBefore(LocalDate.now())) {
                        dateError = "Target date cannot be in the past"
                        return@Button
                    }

                    isSaving = true
                    if (newMilestoneText.trim().isNotEmpty()) {
                        initialMilestones.add(newMilestoneText.trim())
                        newMilestoneText = ""
                    }
                    scope.launch {
                        if (goalId != null && goalId > 0) {
                            updateGoalUseCase(
                                Goal(
                                    id = goalId,
                                    title = title.trim(),
                                    description = description.trim(),
                                    targetDate = targetDate
                                )
                            )
                        } else {
                            val newGoalId = createGoalUseCase(
                                Goal(
                                    title = title.trim(),
                                    description = description.trim(),
                                    targetDate = targetDate
                                )
                            ).getOrNull()

                            if (newGoalId != null) {
                                initialMilestones.forEachIndexed { order, mTitle ->
                                    createMilestoneUseCase(
                                        Milestone(
                                            goalId = newGoalId,
                                            title = mTitle,
                                            order = order + 1
                                        )
                                    )
                                }
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
                    text = if (goalId != null && goalId > 0) "Save Changes" else "Save Goal",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
