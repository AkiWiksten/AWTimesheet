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
val FIELD_CORNER_RADIUS = 12.dp
val PADDING_SPACING_SMALL = 12.dp
val PADDING_SPACING = 16.dp
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
const val ABSENCE_SCREEN = "absence_screen"
const val CREATE_ABSENCE_SCREEN = "create_absence_screen"
const val PROJECT_DETAILS_SCREEN = "project_details_screen"
const val PROJECTS_SCREEN = "projects_screen"
const val SINGLE_PROJECT_SCREEN = "single_project_screen"
const val SETTINGS_SCREEN = "settings_screen"
const val LOCATION_SCREEN = "location_screen"
const val LOCATION_PICKER_SCREEN = "location_picker_screen"

// Domain / project entity keys
const val PROJECT_NAME = "project_name"
const val PROJECT_TIME = "project_time"
const val KILOMETRES = "kilometres"
const val ALLOWANCE = "allowance"
const val WORK_TYPE = "workType"
const val COMMENT = "comment"

// Domain / settings keys
const val NAME = "name"
const val EMPLOYER = "employer"
const val LANGUAGE = "language"
const val ENABLE_TEST_FEATURES = "enable_test_features"

// Domain / absence entity keys
const val START_DATE = "start_date"
const val END_DATE = "end_date"
const val ABSENCE_TYPE = "absence_type"
const val INCLUDE_WEEKENDS = "include_weekends"
const val IS_FLEX_DAY = "is_flex_day"

// Domain / location entity keys
const val START_POINT = "start_point"
const val DESTINATION_POINT = "destination_point"
const val START_LATITUDE = "start_latitude"
const val START_LONGITUDE = "start_longitude"
const val DESTINATION_LATITUDE = "destination_latitude"
const val DESTINATION_LONGITUDE = "destination_longitude"
const val DISTANCE = "distance"
const val TIMESTAMP = "timestamp"

// Data / Room table names and shared ids
const val PROJECT_DETAILS_TABLE = "project_details"
const val PROJECT_TABLE = "project"
const val WORKDAY_TABLE = "workday"
const val PROJECT_NAME_TABLE = "project_name"
const val SETTINGS_TABLE = "settings"
const val WORK_TYPE_TABLE = "work_type"
const val CALCULATED_FLEXTIME_TOTAL_TABLE = "calculated_flextime_total"
const val ABSENCE_TABLE = "absence"
const val ROUTE_TABLE = "route"
const val ID = "id"
