package com.aistudio.dayflow.app.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.dayflow.app.domain.model.Achievement
import com.aistudio.dayflow.app.presentation.common.AchievementCard
import com.aistudio.dayflow.app.presentation.common.CalendarHeatmapView
import com.aistudio.dayflow.app.presentation.common.DayFlowCard
import com.aistudio.dayflow.app.presentation.common.EmptyStateView
import com.aistudio.dayflow.app.presentation.common.HabitIcons
import com.aistudio.dayflow.app.presentation.common.WeeklyActivityView
import com.aistudio.dayflow.app.ui.theme.SoftGreen

enum class AchievementFilter {
    ALL,
    UNLOCKED,
    IN_PROGRESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var achievementFilter by remember { mutableStateOf(AchievementFilter.ALL) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Progress & Insights",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
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
        ) {
            // Tab Selector: Progress vs Achievements
            PrimaryTabRow(
                selectedTabIndex = if (state.selectedTab == StatisticsTab.PROGRESS) 0 else 1,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Tab(
                    selected = state.selectedTab == StatisticsTab.PROGRESS,
                    onClick = { viewModel.selectTab(StatisticsTab.PROGRESS) },
                    text = {
                        Text(
                            text = "📊 Progress",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                )
                Tab(
                    selected = state.selectedTab == StatisticsTab.ACHIEVEMENTS,
                    onClick = { viewModel.selectTab(StatisticsTab.ACHIEVEMENTS) },
                    text = {
                        Text(
                            text = "🏆 Achievements (${state.unlockedCount}/${state.totalAchievementsCount})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (state.selectedTab) {
                StatisticsTab.PROGRESS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Today's Progress Card
                        item(key = "today_progress_card") {
                            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "TODAY'S COMPLETION",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (state.todayScheduledCount > 0) {
                                                "${state.todayCompletedCount} of ${state.todayScheduledCount} done"
                                            } else {
                                                "No habits scheduled today"
                                            },
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${state.todayCompletionRate}%",
                                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = { (state.todayCompletionRate / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }
                        }

                        // 2. Weekly Progress Card with Day-by-Day Activity Indicators
                        item(key = "weekly_progress_card") {
                            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "WEEKLY PROGRESS",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${state.weeklyCompletionRate}%",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SoftGreen
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    WeeklyActivityView(
                                        days = state.weeklyDayItems,
                                        summaryText = state.weeklySummaryText
                                    )
                                }
                            }
                        }

                        // 3. Monthly Summary Card
                        item(key = "monthly_summary_card") {
                            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "MONTHLY SUMMARY (30 DAYS)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${state.monthlyCompletionRate}%",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${state.monthlyCompletedCount} completions logged in the last 30 days.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = { (state.monthlyCompletionRate / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = SoftGreen,
                                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }
                        }

                        // 4. Quick Stats Row: Current Streak, Best Streak, Total Completed
                        item(key = "quick_metrics_row") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Current Streak Card
                                DayFlowCard(modifier = Modifier.weight(1f)) {
                                    Column {
                                        Text(
                                            text = "🔥 ${state.currentStreak}",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFC2410C)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Current streak",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Best Streak Card
                                DayFlowCard(modifier = Modifier.weight(1f)) {
                                    Column {
                                        Text(
                                            text = "🏆 ${state.bestStreak}",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFD97706)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Best streak",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Total Completed Card
                                DayFlowCard(modifier = Modifier.weight(1f)) {
                                    Column {
                                        Text(
                                            text = "✓ ${state.totalCompletions}",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = SoftGreen
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Completed",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // 5. Encouraging Trend / Insight Banner
                        val insightText = when {
                            state.weeklyCompletionRate >= 70 -> "Great momentum! You're above 70% this week."
                            state.currentStreak >= 3 -> "🔥 ${state.currentStreak} days strong! Keep showing up."
                            state.trendSummary.isNotBlank() -> state.trendSummary
                            else -> "Every day is another chance to build consistency."
                        }

                        item(key = "insight_banner") {
                            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = insightText,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (state.bestDay != null) {
                                            Text(
                                                text = "Strongest day: ${state.bestDay}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 6. Activity Calendar Heatmap
                        item(key = "calendar_heatmap") {
                            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                                CalendarHeatmapView(days = state.heatmapDays)
                            }
                        }

                        // 7. Per-Habit Consistency
                        if (state.habitConsistencies.isNotEmpty()) {
                            item(key = "habit_consistency_header") {
                                Text(
                                    text = "Habit Consistency Breakdown",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            items(
                                items = state.habitConsistencies,
                                key = { "cons_${it.habit.id}" }
                            ) { item ->
                                DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = HabitIcons.getIcon(item.habit.icon),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = item.habit.name,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Text(
                                                text = "${(item.consistencyRate * 100).toInt()}%",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        LinearProgressIndicator(
                                            progress = { item.consistencyRate },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = SoftGreen,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            strokeCap = StrokeCap.Round
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "🔥 ${item.currentStreak} day streak",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = Color(0xFFEA580C)
                                            )
                                            Text(
                                                text = "${item.totalCompletions} total completions",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                StatisticsTab.ACHIEVEMENTS -> {
                    val filteredAchievements = when (achievementFilter) {
                        AchievementFilter.ALL -> state.achievements
                        AchievementFilter.UNLOCKED -> state.achievements.filter { it.isUnlocked }
                        AchievementFilter.IN_PROGRESS -> state.achievements.filter { !it.isUnlocked }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Achievement Header Progress Card
                        item(key = "achievements_overview") {
                            DayFlowCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "🏆 MILESTONES & ACHIEVEMENTS",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${state.unlockedCount} of ${state.totalAchievementsCount} Unlocked",
                                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        val unlockedPercentage = if (state.totalAchievementsCount > 0) {
                                            ((state.unlockedCount.toFloat() / state.totalAchievementsCount.toFloat()) * 100).toInt()
                                        } else 0

                                        Text(
                                            text = "$unlockedPercentage%",
                                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFD97706)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    val progressFraction = if (state.totalAchievementsCount > 0) {
                                        (state.unlockedCount.toFloat() / state.totalAchievementsCount.toFloat())
                                    } else 0f

                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = Color(0xFFF59E0B),
                                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        strokeCap = StrokeCap.Round
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Achievements unlock automatically based on your local habit consistency.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Filter Chips
                        item(key = "filter_chips") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = achievementFilter == AchievementFilter.ALL,
                                    onClick = { achievementFilter = AchievementFilter.ALL },
                                    label = { Text("All (${state.totalAchievementsCount})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                FilterChip(
                                    selected = achievementFilter == AchievementFilter.UNLOCKED,
                                    onClick = { achievementFilter = AchievementFilter.UNLOCKED },
                                    label = { Text("Unlocked (${state.unlockedCount})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                FilterChip(
                                    selected = achievementFilter == AchievementFilter.IN_PROGRESS,
                                    onClick = { achievementFilter = AchievementFilter.IN_PROGRESS },
                                    label = { Text("In Progress (${state.totalAchievementsCount - state.unlockedCount})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        // List of Achievements
                        if (filteredAchievements.isEmpty()) {
                            item(key = "empty_achievements") {
                                EmptyStateView(
                                    icon = Icons.Filled.EmojiEvents,
                                    title = "No achievements here yet",
                                    description = "Complete habits to unlock milestones."
                                )
                            }
                        } else {
                            items(
                                items = filteredAchievements,
                                key = { it.id }
                            ) { achievement ->
                                AchievementCard(achievement = achievement)
                            }
                        }
                    }
                }
            }
        }
    }
}
