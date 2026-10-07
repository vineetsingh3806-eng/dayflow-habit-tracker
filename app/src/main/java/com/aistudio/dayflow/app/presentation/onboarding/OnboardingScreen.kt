package com.aistudio.dayflow.app.presentation.onboarding

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.dayflow.app.domain.model.FrequencyType
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.presentation.common.DayFlowCard
import com.aistudio.dayflow.app.presentation.common.HabitIcons
import com.aistudio.dayflow.app.ui.theme.SoftGreen

data class SuggestedHabitItem(
    val name: String,
    val icon: String,
    val colorHex: String,
    val targetCount: Int = 1,
    val targetUnit: String = "times"
)

data class ImprovementCategory(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val colorHex: String,
    val suggestions: List<SuggestedHabitItem>
)

val improvementCategories = listOf(
    ImprovementCategory(
        id = "study",
        name = "📚 Study",
        description = "Focus on learning and homework",
        icon = Icons.Filled.School,
        colorHex = "#4F46E5",
        suggestions = listOf(
            SuggestedHabitItem("Study 30 minutes", "book", "#4F46E5", 30, "mins"),
            SuggestedHabitItem("Review flashcards", "edit", "#0EA5E9", 1, "times"),
            SuggestedHabitItem("Plan tomorrow's study", "check_circle", "#10B981", 1, "times"),
            SuggestedHabitItem("Deep focus session", "meditation", "#8B5CF6", 45, "mins")
        )
    ),
    ImprovementCategory(
        id = "fitness",
        name = "💪 Fitness",
        description = "Strengthen body and health",
        icon = Icons.Filled.FitnessCenter,
        colorHex = "#10B981",
        suggestions = listOf(
            SuggestedHabitItem("Walk 30 minutes", "walk", "#10B981", 30, "mins"),
            SuggestedHabitItem("Workout", "fitness", "#EF4444", 1, "times"),
            SuggestedHabitItem("Drink enough water", "water", "#0EA5E9", 8, "glasses"),
            SuggestedHabitItem("Sleep on time", "sleep", "#64748B", 1, "times")
        )
    ),
    ImprovementCategory(
        id = "career",
        name = "💼 Career",
        description = "Skills, learning and productivity",
        icon = Icons.Filled.Code,
        colorHex = "#F59E0B",
        suggestions = listOf(
            SuggestedHabitItem("Review priority tasks", "check_circle", "#4F46E5", 1, "times"),
            SuggestedHabitItem("Learn a professional skill", "code", "#F59E0B", 30, "mins"),
            SuggestedHabitItem("Organize workspace", "spa", "#10B981", 1, "times"),
            SuggestedHabitItem("Plan weekly objectives", "book", "#8B5CF6", 1, "times")
        )
    ),
    ImprovementCategory(
        id = "reading",
        name = "📖 Reading",
        description = "Read pages every day",
        icon = Icons.Filled.Book,
        colorHex = "#8B5CF6",
        suggestions = listOf(
            SuggestedHabitItem("Read 15 pages", "book", "#4F46E5", 15, "pages"),
            SuggestedHabitItem("Morning reading", "book", "#0EA5E9", 20, "mins"),
            SuggestedHabitItem("Highlight key ideas", "edit", "#F59E0B", 1, "times"),
            SuggestedHabitItem("Night chapter", "sleep", "#8B5CF6", 1, "chapter")
        )
    ),
    ImprovementCategory(
        id = "personal",
        name = "🌱 Personal",
        description = "Calm, rest and daily balance",
        icon = Icons.Filled.SelfImprovement,
        colorHex = "#0EA5E9",
        suggestions = listOf(
            SuggestedHabitItem("Morning meditation", "meditation", "#10B981", 10, "mins"),
            SuggestedHabitItem("Journal reflection", "edit", "#0EA5E9", 1, "times"),
            SuggestedHabitItem("Healthy meal", "spa", "#F59E0B", 1, "times"),
            SuggestedHabitItem("Take a digital break", "heart", "#EF4444", 30, "mins")
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinish: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<ImprovementCategory?>(null) }
    val selectedHabitIndices = remember { mutableStateListOf<Int>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    if (selectedCategory != null) {
                        IconButton(onClick = {
                            selectedCategory = null
                            selectedHabitIndices.clear()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.completeOnboarding(onFinish) }) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        AnimatedContent(
            targetState = selectedCategory,
            label = "onboarding_flow",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { category ->
            if (category == null) {
                // Screen 1: "What would you like to improve?"
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "What would you like to improve?",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose an area to see tailored daily habit suggestions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(items = improvementCategories, key = { it.id }) { item ->
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(item.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            DayFlowCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    selectedCategory = item
                                    selectedHabitIndices.clear()
                                    // Default: select the first 2 suggestions as a gentle recommendation, user can freely adjust
                                    if (item.suggestions.isNotEmpty()) {
                                        selectedHabitIndices.add(0)
                                        if (item.suggestions.size > 1) {
                                            selectedHabitIndices.add(1)
                                        }
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(catColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { viewModel.completeOnboarding(onFinish) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("I'll set up my own habits later")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // Screen 2: Habit Suggestions
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${category.name} Habits",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pick the habits you want to start with. You can change or remove them anytime.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(
                            items = category.suggestions.indices.toList(),
                            key = { index -> "${category.id}_$index" }
                        ) { index ->
                            val habitSuggestion = category.suggestions[index]
                            val isSelected = selectedHabitIndices.contains(index)
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(habitSuggestion.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            DayFlowCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    if (isSelected) {
                                        selectedHabitIndices.remove(index)
                                    } else {
                                        selectedHabitIndices.add(index)
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(catColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = HabitIcons.getIcon(habitSuggestion.icon),
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = habitSuggestion.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (habitSuggestion.targetCount > 1 || habitSuggestion.targetUnit != "times") {
                                            Text(
                                                text = "${habitSuggestion.targetCount} ${habitSuggestion.targetUnit} daily",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (!selectedHabitIndices.contains(index)) selectedHabitIndices.add(index)
                                            } else {
                                                selectedHabitIndices.remove(index)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val selectedCount = selectedHabitIndices.size
                    val buttonText = if (selectedCount > 0) {
                        "Start with $selectedCount ${if (selectedCount == 1) "Habit" else "Habits"}"
                    } else {
                        "Get Started (No habits)"
                    }

                    Button(
                        onClick = {
                            val habitsToCreate = selectedHabitIndices.map { idx ->
                                val sug = category.suggestions[idx]
                                Habit(
                                    name = sug.name,
                                    icon = sug.icon,
                                    colorHex = sug.colorHex,
                                    frequencyType = FrequencyType.DAILY,
                                    targetCount = sug.targetCount,
                                    targetUnit = sug.targetUnit
                                )
                            }
                            viewModel.completeWithHabits(habitsToCreate, onFinish)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
