package com.aistudio.dayflow.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aistudio.dayflow.app.data.local.converter.Converters
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

@Database(
    entities = [
        HabitEntity::class,
        HabitCompletionEntity::class,
        GoalEntity::class,
        MilestoneEntity::class,
        HabitGoalLinkEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DayFlowDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun goalDao(): GoalDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun habitGoalLinkDao(): HabitGoalLinkDao

    companion object {
        @Volatile
        private var INSTANCE: DayFlowDatabase? = null

        fun getInstance(context: Context): DayFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DayFlowDatabase::class.java,
                    "dayflow.db"
                )
                    // In production, fallback only on downgrade to avoid silent data loss during future version upgrades.
                    // Future schema upgrades (v2+) must provide explicit Migration implementations.
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
