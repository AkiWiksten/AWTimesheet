package com.akiwiksten.awtimesheet.core

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Core / shared formatting and defaults
const val DATE = "date"
const val DATE_FORMAT = "yyyy-MM-dd"
const val TIME_FORMAT = "HH:mm"
const val ZERO_TIME = "00:00"
const val DEFAULT_DAILY_WORK_TIME = "07:30"
const val MINUTES_IN_HOUR = 60
const val LOADING_INDICATOR_DELAY_MS = 1_000L
const val APP_NAME_QUALIFIER = "app_name"
const val LABEL_FONT_SIZE_SCALE = 1.08f

// Default work types
val DEFAULT_WORK_TYPES = listOf(
    R.string.other,
    R.string.work_type_paid_vacation,
    R.string.work_type_unpaid_vacation,
    R.string.work_type_sick_leave,
    R.string.work_type_parental_leave,
    R.string.work_type_other_leave,
    R.string.work_type_flex_day
)

// Core / UI design constants
val DEFAULT_ELEVATION = 8.dp
val SCREEN_PAGE_PADDING = 16.dp
val SCREEN_SECTION_SPACING_LARGE = 20.dp
val SCREEN_PAGE_PADDING_LARGE = 24.dp
val SCREEN_EMPTY_STATE_PADDING = 32.dp
val FIELD_CORNER_RADIUS = 12.dp
val HEADER_CONTENT_PADDING = 12.dp
val HEADER_CONTENT_SPACING = 6.dp
val FORM_GROUP_PADDING = 12.dp
val FORM_GROUP_SPACING = 12.dp
val FORM_SECTION_SPACING = 16.dp
val FORM_INLINE_SPACING = 8.dp
val SCREEN_CONTENT_SPACING = 24.dp
val FORM_MAX_WIDTH = 600.dp
val ACTION_BUTTON_FONT_SIZE = 18.sp

// Domain / project details keys
const val START_TIME = "start_time"
const val END_TIME = "end_time"
const val WORK_TIME_BY_DATE = "work_time_today"
const val WORK_TIME_BY_DATE_ESTIMATE = "work_time_today_estimate"
const val INITIAL_FLEX_TIME_TOTAL = "initial_flex_time_total"
const val CALCULATED_FLEX_TIME_TOTAL = "calculated_flex_time_total"
const val DAILY_WORK_TIME_ESTIMATE = "daily_work_time_estimate"
const val LUNCH_START = "lunch_start"
const val LUNCH_END = "lunch_end"
const val DAILY_LUNCH_TIME_ESTIMATE = "daily_lunch_time_estimate"
const val BREAK_START = "break_start"
const val BREAK_END = "break_end"
const val LUNCH_TIME_ESTIMATE = "lunch_time_estimate"

// App / navigation routes
const val INTRO_SCREEN = "intro_screen"
const val CALENDAR_SCREEN = "calendar_screen"
const val PROJECT_DETAILS_SCREEN = "project_details_screen"
const val PROJECTS_SCREEN = "projects_screen"
const val SINGLE_PROJECT_SCREEN = "single_project_screen"
const val SETTINGS_SCREEN = "settings_screen"

// Domain / project entity keys
const val PROJECT_NAME = "project_name"
const val PROJECT_TIME = "project_time"
const val KILOMETRES = "kilometres"
const val ALLOWANCE = "allowance"
const val WORK_TYPE = "workType"

// Domain / settings keys
const val NAME = "name"
const val EMPLOYER = "employer"

// Data / Room table names and shared ids
const val PROJECT_DETAILS_TABLE = "project_details"
const val PROJECT_TABLE = "project"
const val WORKDAY_TABLE = "workday"
const val PROJECT_NAME_TABLE = "project_name"
const val SETTINGS_TABLE = "settings"
const val WORK_TYPE_TABLE = "work_type"
const val CALCULATED_FLEXTIME_TOTAL_TABLE = "calculated_flextime_total"
const val ID = "id"
