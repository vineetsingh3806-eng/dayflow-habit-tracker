package com.aistudio.dayflow.app.data.local.converter

import androidx.room.TypeConverter
import com.aistudio.dayflow.app.domain.model.CompletionStatus
import com.aistudio.dayflow.app.domain.model.FrequencyType
import com.aistudio.dayflow.app.domain.model.GoalStatus
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Converters {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.format(dateFormatter)

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it, dateFormatter) }

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.format(timeFormatter)

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it, timeFormatter) }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromFrequencyType(type: FrequencyType?): String? = type?.name

    @TypeConverter
    fun toFrequencyType(value: String?): FrequencyType? = value?.let { FrequencyType.valueOf(it) }

    @TypeConverter
    fun fromCompletionStatus(status: CompletionStatus?): String? = status?.name

    @TypeConverter
    fun toCompletionStatus(value: String?): CompletionStatus? = value?.let { CompletionStatus.valueOf(it) }

    @TypeConverter
    fun fromGoalStatus(status: GoalStatus?): String? = status?.name

    @TypeConverter
    fun toGoalStatus(value: String?): GoalStatus? = value?.let { GoalStatus.valueOf(it) }

    @TypeConverter
    fun fromDaysOfWeek(days: Set<DayOfWeek>?): String? {
        return days?.joinToString(separator = ",") { it.name }
    }

    @TypeConverter
    fun toDaysOfWeek(value: String?): Set<DayOfWeek>? {
        if (value.isNullOrEmpty()) return emptySet()
        return value.split(",")
            .mapNotNull {
                try {
                    DayOfWeek.valueOf(it.trim())
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            .toSet()
    }
}
