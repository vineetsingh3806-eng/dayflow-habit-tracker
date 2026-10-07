package com.aistudio.dayflow.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.dayflow.app.data.local.DayFlowDatabase
import com.aistudio.dayflow.app.data.local.dao.GoalDao
import com.aistudio.dayflow.app.data.local.dao.HabitCompletionDao
import com.aistudio.dayflow.app.data.local.dao.HabitDao
import com.aistudio.dayflow.app.data.local.dao.HabitGoalLinkDao
import com.aistudio.dayflow.app.data.local.dao.MilestoneDao
import com.aistudio.dayflow.app.data.local.entity.GoalEntity
import com.aistudio.dayflow.app.data.local.entity.HabitCompletionEntity
import com.aistudio.dayflow.app.data.local.entity.HabitEntity
import com.aistudio.dayflow.app.data.local.entity.HabitGoalLinkEntity
import com.aistudio.dayflow.app.data.local.entity.MilestoneEntity
import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.FrequencyType
import com.aistudio.dayflow.app.domain.model.GoalStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DayFlowDatabaseTest {

    private lateinit var database: DayFlowDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var habitCompletionDao: HabitCompletionDao
    private lateinit var goalDao: GoalDao
    private lateinit var milestoneDao: MilestoneDao
    private lateinit var habitGoalLinkDao: HabitGoalLinkDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DayFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habitDao = database.habitDao()
        habitCompletionDao = database.habitCompletionDao()
        goalDao = database.goalDao()
        milestoneDao = database.milestoneDao()
        habitGoalLinkDao = database.habitGoalLinkDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testHabitCreationAndRetrieval() = runBlocking {
        val habit = HabitEntity(
            name = "Morning Meditation",
            description = "10 minutes of mindfulness",
            icon = "spa",
            colorHex = "#4F46E5",
            frequencyType = FrequencyType.DAILY,
            selectedDays = emptySet(),
            targetTimesPerWeek = 7,
            targetCount = 10,
            targetUnit = "mins",
            reminderTime = LocalTime.of(7, 30),
            createdAt = Instant.now(),
            isArchived = false
        )

        val id = habitDao.insertHabit(habit)
        assertTrue(id > 0)

        val activeHabits = habitDao.getActiveHabits().first()
        assertEquals(1, activeHabits.size)
        assertEquals("Morning Meditation", activeHabits[0].name)
        assertEquals(FrequencyType.DAILY, activeHabits[0].frequencyType)
        assertEquals(10, activeHabits[0].targetCount)
        assertEquals("mins", activeHabits[0].targetUnit)
    }

    @Test
    fun testHabitArchiveAndRestore() = runBlocking {
        val habit = HabitEntity(
            name = "Read 20 pages",
            description = "Self-improvement books",
            icon = "menu_book",
            colorHex = "#10B981",
            frequencyType = FrequencyType.SELECTED_DAYS,
            selectedDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            targetTimesPerWeek = 3,
            targetCount = 20,
            targetUnit = "pages",
            reminderTime = LocalTime.of(21, 0),
            createdAt = Instant.now(),
            isArchived = false
        )

        val id = habitDao.insertHabit(habit)

        // Verify active
        assertEquals(1, habitDao.getActiveHabits().first().size)
        assertEquals(0, habitDao.getArchivedHabits().first().size)

        // Archive habit
        habitDao.setArchived(id, true)
        assertEquals(0, habitDao.getActiveHabits().first().size)
        assertEquals(1, habitDao.getArchivedHabits().first().size)

        // Restore habit
        habitDao.setArchived(id, false)
        assertEquals(1, habitDao.getActiveHabits().first().size)
        assertEquals(0, habitDao.getArchivedHabits().first().size)
    }

    @Test
    fun testHabitCompletionAndUndo() = runBlocking {
        val habitId = habitDao.insertHabit(
            HabitEntity(
                name = "Drink Water",
                description = "8 glasses",
                icon = "water_drop",
                colorHex = "#0EA5E9",
                frequencyType = FrequencyType.DAILY,
                selectedDays = emptySet(),
                targetTimesPerWeek = 7,
                targetCount = 8,
                targetUnit = "glasses",
                reminderTime = null,
                createdAt = Instant.now(),
                isArchived = false
            )
        )

        val today = LocalDate.of(2026, 10, 2)
        val completion = HabitCompletionEntity(
            habitId = habitId,
            date = today,
            completedAt = Instant.now(),
            status = CompletionStatus.COMPLETED,
            notes = "Done 8 glasses"
        )

        val completionId = habitCompletionDao.insertCompletion(completion)
        assertTrue(completionId > 0)

        // Verify completion exists
        val completions = habitCompletionDao.getCompletionsForDate(today).first()
        assertEquals(1, completions.size)
        assertEquals(habitId, completions[0].habitId)
        assertEquals(CompletionStatus.COMPLETED, completions[0].status)

        // Undo completion
        habitCompletionDao.deleteCompletion(habitId, today)
        val afterUndo = habitCompletionDao.getCompletionsForDate(today).first()
        assertEquals(0, afterUndo.size)
    }

    @Test
    fun testGoalAndMilestonesWithLinking() = runBlocking {
        val habitId = habitDao.insertHabit(
            HabitEntity(
                name = "Workout",
                description = "Gym workout",
                icon = "fitness_center",
                colorHex = "#EF4444",
                frequencyType = FrequencyType.DAILY,
                selectedDays = emptySet(),
                targetTimesPerWeek = 5,
                targetCount = 45,
                targetUnit = "mins",
                reminderTime = null,
                createdAt = Instant.now(),
                isArchived = false
            )
        )

        val goalId = goalDao.insertGoal(
            GoalEntity(
                title = "Run 10K Marathon",
                description = "Preparation for marathon",
                startDate = LocalDate.of(2026, 10, 1),
                targetDate = LocalDate.of(2026, 12, 31),
                status = GoalStatus.ACTIVE,
                progress = 0.0f,
                createdAt = Instant.now()
            )
        )
        assertTrue(goalId > 0)

        val milestone1Id = milestoneDao.insertMilestone(
            MilestoneEntity(
                goalId = goalId,
                title = "Run 3K without stopping",
                order = 1,
                completed = false,
                completedAt = null
            )
        )

        val milestone2Id = milestoneDao.insertMilestone(
            MilestoneEntity(
                goalId = goalId,
                title = "Run 5K under 30 mins",
                order = 2,
                completed = false,
                completedAt = null
            )
        )

        // Link habit to milestone
        habitGoalLinkDao.insertLink(
            HabitGoalLinkEntity(
                habitId = habitId,
                milestoneId = milestone1Id,
                goalId = goalId,
                createdAt = Instant.now()
            )
        )

        val milestones = milestoneDao.getMilestonesForGoal(goalId).first()
        assertEquals(2, milestones.size)
        assertEquals("Run 3K without stopping", milestones[0].title)

        // Mark milestone 1 completed
        milestoneDao.setMilestoneCompleted(milestone1Id, true, Instant.now())
        val updatedMilestone = milestoneDao.getMilestoneById(milestone1Id)
        assertNotNull(updatedMilestone)
        assertTrue(updatedMilestone!!.completed)
        assertNotNull(updatedMilestone.completedAt)

        // Check linked habit
        val linkedHabits = habitGoalLinkDao.getLinksForGoal(goalId).first()
        assertEquals(1, linkedHabits.size)
        assertEquals(habitId, linkedHabits[0].habitId)
    }

    @Test
    fun testCascadeDeletionOfHabitCleansCompletions() = runBlocking {
        val habitId = habitDao.insertHabit(
            HabitEntity(
                name = "Test Habit",
                description = "Temporary",
                icon = "star",
                colorHex = "#4F46E5",
                frequencyType = FrequencyType.DAILY,
                selectedDays = emptySet(),
                targetTimesPerWeek = 7,
                targetCount = 1,
                targetUnit = "times",
                reminderTime = null,
                createdAt = Instant.now(),
                isArchived = false
            )
        )

        val date = LocalDate.of(2026, 10, 2)
        habitCompletionDao.insertCompletion(
            HabitCompletionEntity(
                habitId = habitId,
                date = date,
                completedAt = Instant.now(),
                status = CompletionStatus.COMPLETED,
                notes = ""
            )
        )

        assertEquals(1, habitCompletionDao.getCompletionsForHabit(habitId).first().size)

        // Delete habit
        habitDao.deleteHabitById(habitId)

        // Verify completion is cascaded
        assertEquals(0, habitCompletionDao.getCompletionsForHabit(habitId).first().size)
    }

    @Test
    fun testDatabaseSingletonAndVersionIntegrity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbInstance = DayFlowDatabase.getInstance(context)
        assertNotNull(dbInstance)
        assertNotNull(dbInstance.openHelper)
        assertEquals("dayflow.db", dbInstance.openHelper.databaseName)
        val version = dbInstance.openHelper.readableDatabase.version
        assertEquals(1, version)
    }
}
