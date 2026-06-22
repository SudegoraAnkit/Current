package com.sudegoratechglobal.current.util

import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class ParsedTask(
    val title: String,
    val scheduledTime: Long,
    val priority: Int = 2,
    val isLocked: Boolean = false,
    val accountabilityContact: String? = null
)

object NlpParser {

    fun parse(input: String): ParsedTask {
        var text = input.trim()
        var priority = 2
        var isLocked = false
        var accountabilityContact: String? = null

        // 1. Parse Priority (e.g. !high, !urgent, !1, !medium, !2, !low, !3)
        val priorityPattern = Pattern.compile("!(high|medium|low|urgent|1|2|3)", Pattern.CASE_INSENSITIVE)
        val priorityMatcher = priorityPattern.matcher(text)
        if (priorityMatcher.find()) {
            val level = priorityMatcher.group(1)?.lowercase(Locale.ROOT)
            priority = when (level) {
                "high", "urgent", "1" -> 1
                "medium", "2" -> 2
                "low", "3" -> 3
                else -> 2
            }
            text = priorityMatcher.replaceAll("").trim()
        }

        // 2. Parse Lock & Accountability Contact (e.g., lock +12345, locked mom)
        // Match "lock" or "locked", optionally followed by a number or word
        val lockPattern = Pattern.compile("\\b(lock|locked)(?:\\s+([+0-9a-zA-Z\\-]+))?\\b", Pattern.CASE_INSENSITIVE)
        val lockMatcher = lockPattern.matcher(text)
        if (lockMatcher.find()) {
            isLocked = true
            accountabilityContact = lockMatcher.group(2)
            text = lockMatcher.replaceAll("").trim()
        }

        val calendar = Calendar.getInstance()
        var dateSpecified = false
        var timeSpecified = false

        // 3. Parse "in X hours/minutes/days" offset
        val relativePattern = Pattern.compile(
            "\\bin\\s+(\\d+)\\s*(hour|hours|hr|hrs|minute|minutes|min|mins|day|days)\\b",
            Pattern.CASE_INSENSITIVE
        )
        val relativeMatcher = relativePattern.matcher(text)
        if (relativeMatcher.find()) {
            val amount = relativeMatcher.group(1)?.toIntOrNull() ?: 0
            val unit = relativeMatcher.group(2)?.lowercase(Locale.ROOT) ?: ""
            when {
                unit.startsWith("min") -> calendar.add(Calendar.MINUTE, amount)
                unit.startsWith("hour") || unit.startsWith("hr") -> calendar.add(Calendar.HOUR_OF_DAY, amount)
                unit.startsWith("day") -> calendar.add(Calendar.DAY_OF_YEAR, amount)
            }
            dateSpecified = true
            timeSpecified = true
            text = relativeMatcher.replaceAll("").trim()
        }

        // 4. Parse Date keywords ("today", "tomorrow")
        if (!dateSpecified) {
            val todayPattern = Pattern.compile("\\btoday\\b", Pattern.CASE_INSENSITIVE)
            val todayMatcher = todayPattern.matcher(text)
            if (todayMatcher.find()) {
                // Keep current day
                dateSpecified = true
                text = todayMatcher.replaceAll("").trim()
            }
        }

        if (!dateSpecified) {
            val tomorrowPattern = Pattern.compile("\\btomorrow\\b", Pattern.CASE_INSENSITIVE)
            val tomorrowMatcher = tomorrowPattern.matcher(text)
            if (tomorrowMatcher.find()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                dateSpecified = true
                text = tomorrowMatcher.replaceAll("").trim()
            }
        }

        // 5. Parse Weekdays ("on Friday", "on mon")
        if (!dateSpecified) {
            val weekdayPattern = Pattern.compile(
                "\\bon\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)\\b",
                Pattern.CASE_INSENSITIVE
            )
            val weekdayMatcher = weekdayPattern.matcher(text)
            if (weekdayMatcher.find()) {
                val dayStr = weekdayMatcher.group(1)?.lowercase(Locale.ROOT) ?: ""
                val targetDayOfWeek = when (dayStr) {
                    "monday", "mon" -> Calendar.MONDAY
                    "tuesday", "tue" -> Calendar.TUESDAY
                    "wednesday", "wed" -> Calendar.WEDNESDAY
                    "thursday", "thu" -> Calendar.THURSDAY
                    "friday", "fri" -> Calendar.FRIDAY
                    "saturday", "sat" -> Calendar.SATURDAY
                    "sunday", "sun" -> Calendar.SUNDAY
                    else -> calendar.get(Calendar.DAY_OF_WEEK)
                }
                val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                var daysDiff = targetDayOfWeek - currentDayOfWeek
                if (daysDiff <= 0) {
                    daysDiff += 7 // Next week
                }
                calendar.add(Calendar.DAY_OF_YEAR, daysDiff)
                dateSpecified = true
                text = weekdayMatcher.replaceAll("").trim()
            }
        }

        // 6. Parse Time (e.g., "at 5pm", "at 17:30", "5pm", "10:00 am")
        // Check for formats like "at 5pm", "at 5", "at 17:30"
        val timePattern1 = Pattern.compile(
            "\\bat\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b",
            Pattern.CASE_INSENSITIVE
        )
        val timeMatcher1 = timePattern1.matcher(text)
        if (timeMatcher1.find()) {
            val hour = timeMatcher1.group(1)?.toIntOrNull() ?: 12
            val min = timeMatcher1.group(2)?.toIntOrNull() ?: 0
            val amPm = timeMatcher1.group(3)?.lowercase(Locale.ROOT)

            setCalendarTime(calendar, hour, min, amPm)
            timeSpecified = true
            text = timeMatcher1.replaceAll("").trim()
        }

        // If not matched by "at", try matching raw times like "5pm" or "10:30am"
        if (!timeSpecified) {
            val timePattern2 = Pattern.compile(
                "\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b",
                Pattern.CASE_INSENSITIVE
            )
            val timeMatcher2 = timePattern2.matcher(text)
            if (timeMatcher2.find()) {
                val hour = timeMatcher2.group(1)?.toIntOrNull() ?: 12
                val min = timeMatcher2.group(2)?.toIntOrNull() ?: 0
                val amPm = timeMatcher2.group(3)?.lowercase(Locale.ROOT)

                setCalendarTime(calendar, hour, min, amPm)
                timeSpecified = true
                text = timeMatcher2.replaceAll("").trim()
            }
        }

        // Fallback defaults
        if (!dateSpecified && !timeSpecified) {
            // Default to tomorrow at 9:00 AM
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
        } else if (dateSpecified && !timeSpecified) {
            // Default to 9:00 AM on the specified day
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
        } else if (!dateSpecified && timeSpecified) {
            // Today at specified time. If time already passed, make it tomorrow
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Trim double spaces
        val finalTitle = text.replace(Regex("\\s+"), " ").trim()
        val title = if (finalTitle.isEmpty()) "Untitled Task" else finalTitle

        return ParsedTask(
            title = title,
            scheduledTime = calendar.timeInMillis,
            priority = priority,
            isLocked = isLocked,
            accountabilityContact = accountabilityContact
        )
    }

    private fun setCalendarTime(calendar: Calendar, hour: Int, min: Int, amPm: String?) {
        if (amPm != null) {
            val h = if (amPm == "pm" && hour < 12) hour + 12 else if (amPm == "am" && hour == 12) 0 else hour
            calendar.set(Calendar.HOUR_OF_DAY, h)
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, hour)
        }
        calendar.set(Calendar.MINUTE, min)
    }
}
