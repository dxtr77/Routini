package com.dxtr.routini.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Converters {
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val gson = Gson()

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? {
        return value?.format(timeFormatter)
    }

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? {
        return value?.let {
            return LocalTime.parse(it, timeFormatter)
        }
    }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let {
            return LocalDate.parse(it, dateFormatter)
        }
    }

    @TypeConverter
    fun fromDayOfWeekList(value: List<DayOfWeek>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toDayOfWeekList(value: String?): List<DayOfWeek>? {
        val listType = object : TypeToken<List<DayOfWeek>>() {}.type
        return gson.fromJson(value, listType)
    }
}