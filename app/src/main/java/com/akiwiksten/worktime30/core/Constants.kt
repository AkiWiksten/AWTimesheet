package com.akiwiksten.worktime30.core

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val DATE = "date"
const val ZERO_TIME = "00:00"
const val DEFAULT_DAILY_WORK_TIME = "07:30"
const val MINUTES_IN_HOUR = 60
const val TIME_FORMAT = "HH:mm"
const val DATE_FORMAT = "yyyy-MM-dd"
const val LOADING_INDICATOR_DELAY_MS = 1_000L
const val APP_NAME_QUALIFIER = "app_name"
const val LABEL_FONT_SIZE_SCALE = 1.08f
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

// Project Details
const val START_TIME = "start_time"
const val END_TIME = "end_time"
const val WORK_TIME_TODAY = "work_time_today"
const val WORK_TIME_TODAY_ESTIMATE = "work_time_today_estimate"
const val INITIAL_FLEX_TIME_TOTAL = "initial_flex_time_total"
const val DAILY_WORK_TIME_ESTIMATE = "daily_work_time"
const val LUNCH_START = "lunch_start"
const val LUNCH_END = "lunch_end"
const val DAILY_LUNCH_TIME_ESTIMATE = "lunch_time"
const val BREAK_START = "break_start"
const val BREAK_END = "break_end"
const val FLEX_TIME_TODAY = "flex_time_today"

// Screens
const val INTRO_SCREEN = "intro_screen"
const val CALENDAR_SCREEN = "calendar_screen"
const val PROJECT_DETAILS_SCREEN = "project_details_screen"
const val PROJECTS_SCREEN = "projects_screen"
const val SINGLE_PROJECT_SCREEN = "single_project_screen"
const val SETTINGS_SCREEN = "settings_screen"

// Project
const val PROJECT_NAME = "project_name"
const val PROJECT_TIME = "project_time"
const val KILOMETRES = "kilometres"
const val ALLOWANCE = "allowance"
const val WORK_TYPE = "workType"

// Settings
const val NAME = "name"
const val EMPLOYER = "employer"

// Database Tables
const val PROJECT_DETAILS_TABLE = "project_details"
const val WORK_STATS_TABLE = "work_stats"
const val PROJECT_TABLE = "project"
const val WORKDAY_TABLE = "workday"
const val PROJECT_NAME_TABLE = "project_name"
const val SETTINGS_TABLE = "settings"
const val WORK_TYPE_TABLE = "work_type"
const val ID = "id"
