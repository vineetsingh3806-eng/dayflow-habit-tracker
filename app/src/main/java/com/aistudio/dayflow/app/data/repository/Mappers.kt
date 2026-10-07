package com.aistudio.dayflow.app.data.repository

import com.aistudio.dayflow.app.data.local.entity.GoalEntity
import com.aistudio.dayflow.app.data.local.entity.HabitCompletionEntity
import com.aistudio.dayflow.app.data.local.entity.HabitEntity
import com.aistudio.dayflow.app.data.local.entity.MilestoneEntity
import com.aistudio.dayflow.app.domain.model.Goal
import com.aistudio.dayflow.app.domain.model.Habit
import com.aistudio.dayflow.app.domain.model.HabitCompletion
import com.aistudio.dayflow.app.domain.model.Milestone

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    name = name,
    description = description,
    icon = icon,
    colorHex = colorHex,
    frequencyType = frequencyType,
    selectedDays = selectedDays,
    targetTimesPerWeek = targetTimesPerWeek,
    targetCount = targetCount,
    targetUnit = targetUnit,
    reminderTime = reminderTime,
    createdAt = createdAt,
    isArchived = isArchived
)

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    name = name,
    description = description,
    icon = icon,
    colorHex = colorHex,
    frequencyType = frequencyType,
    selectedDays = selectedDays,
    targetTimesPerWeek = targetTimesPerWeek,
    targetCount = targetCount,
    targetUnit = targetUnit,
    reminderTime = reminderTime,
    createdAt = createdAt,
    isArchived = isArchived
)

fun HabitCompletionEntity.toDomain(): HabitCompletion = HabitCompletion(
    id = id,
    habitId = habitId,
    date = date,
    completedAt = completedAt,
    status = status,
    notes = notes
)

fun HabitCompletion.toEntity(): HabitCompletionEntity = HabitCompletionEntity(
    id = id,
    habitId = habitId,
    date = date,
    completedAt = completedAt,
    status = status,
    notes = notes
)

fun GoalEntity.toDomain(): Goal = Goal(
    id = id,
    title = title,
    description = description,
    startDate = startDate,
    targetDate = targetDate,
    status = status,
    progress = progress,
    createdAt = createdAt
)

fun Goal.toEntity(): GoalEntity = GoalEntity(
    id = id,
    title = title,
    description = description,
    startDate = startDate,
    targetDate = targetDate,
    status = status,
    progress = progress,
    createdAt = createdAt
)

fun MilestoneEntity.toDomain(): Milestone = Milestone(
    id = id,
    goalId = goalId,
    title = title,
    order = order,
    completed = completed,
    completedAt = completedAt
)

fun Milestone.toEntity(): MilestoneEntity = MilestoneEntity(
    id = id,
    goalId = goalId,
    title = title,
    order = order,
    completed = completed,
    completedAt = completedAt
)
