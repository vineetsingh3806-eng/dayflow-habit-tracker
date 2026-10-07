package com.aistudio.dayflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.FrequencyType
import com.aistudio.dayflow.app.domain.model.GoalStatus
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["isArchived"]),
        Index(value = ["createdAt"])
    ]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val icon: String,
    val colorHex: String,
    val frequencyType: FrequencyType,
    val selectedDays: Set<DayOfWeek>,
    val targetTimesPerWeek: Int,
    val targetCount: Int,
    val targetUnit: String,
    val reminderTime: LocalTime?,
    val createdAt: Instant,
    val isArchived: Boolean
)

@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId", "date"], unique = true),
        Index(value = ["date"]),
        Index(value = ["habitId"])
    ]
)
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val date: LocalDate,
    val completedAt: Instant,
    val status: CompletionStatus,
    val notes: String
)

@Entity(
    tableName = "goals",
    indices = [
        Index(value = ["status"]),
        Index(value = ["targetDate"])
    ]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val startDate: LocalDate,
    val targetDate: LocalDate?,
    val status: GoalStatus,
    val progress: Float,
    val createdAt: Instant
)

@Entity(
    tableName = "milestones",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["order"])
    ]
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long,
    val title: String,
    val order: Int,
    val completed: Boolean,
    val completedAt: Instant?
)

@Entity(
    tableName = "habit_goal_links",
    primaryKeys = ["habitId", "milestoneId"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MilestoneEntity::class,
            parentColumns = ["id"],
            childColumns = ["milestoneId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["milestoneId"]),
        Index(value = ["goalId"])
    ]
)
data class HabitGoalLinkEntity(
    val habitId: Long,
    val milestoneId: Long,
    val goalId: Long,
    val createdAt: Instant
)
