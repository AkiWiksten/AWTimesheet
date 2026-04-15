# WorkTime 3.0
## Purpose
+ An app for work time management
### Features
+ **Intro** for a fancy introduction animation
+ **Calendar** to select a date to manage
+ **Projects** to manage projects for selected date
  + **Single project** to manage a single project
    + **Workday** to feed and calculate start, end, work time, balance, breaks and lunch.
+ **Settings** to manage settings, like name, employer work type choices (Coding, Installation, Design etc.)
  + **Pdf monthly report** shows your daily work time per projects in a table. Calculates also monthly statistics.
## Technologies
+ Android, Jetpack Compose, Kotlin, SQLite are the main technologies used
## Modern App Architecture
### Navigation3
### Hilt
### Structure (Compose + MVVM)
    
A clean, scalable example of this is:
+ app/
    + core/                 # shared utilities, theme, base classes
    + data/                 # repositories, API, database
    + domain/               # use cases, business logic (optional but good)
    + feature/
        + home/
            + HomeScreen.kt
            + HomeViewModel.kt
            + HomeUiState.kt
            + components/
        + profile/
            + ProfileScreen.kt
            + ProfileViewModel.kt
            + components/
        + login/
    + navigation/
        + NavGraph.kt

This is called feature-based packaging.
## Features to be implemented yet
+ create full backup support.
+ unify Headers for Screens
+ unify icon names
+ Project name edit and validation in SingleProjectScreen
+ Project name validation in ProjectsScreen. Kilometres validation. 0050 not valid.
+ Workday: “Open End Time picker to select time and confirm to show the total work time of the day”
+ Ask to save when coming back from or switching to a screen.
+ Show Toast when saving data or confirming.
+ Validation
+ Design patterns to implement. Already implemented? Where?
+ GetCalendarDataUseCase: only calculate from projects, not at all from workdays.
+ Test all features in all screens
  + Intro,
  + Calendar,
  + Projects,
  + Single project,
  + Workday,
  + Settings
+ Orientation support
  + detekt, lint, sonarcube run regularly, preferrably before pushing code
  + In “WorkdayScreen”
  + move “Daily work time” and “Lunch time” to SettingsScreen. Add estimate to the strings.
  + move “Balance today” and “Balance total” to ProjectsScreen. Show them in a same way as “Work time today”.
  + Edit “Balance total” in SettingsScreen.
  + Remove “Work time total”
