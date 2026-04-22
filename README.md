# AWWorkTime 3.0
## Purpose
+ An app for work time management
## Branches
+ main: default "working" branch, master: "construction" branch
## Features
+ **Intro** for a fancy introduction animation
+ **Calendar** to select a date to manage
+ **Projects** to manage projects for selected date
  + **Single project** to manage a single project
    + **Project Details** to feed and calculate start, end, work time, FlexTime, breaks and lunch.
+ **Settings** to manage settings, like name, employer work type choices (Coding, Installation, Design etc.)
  + **Pdf monthly report** shows your daily work time per projects in a table. Calculates also monthly statistics.
## Main Technologies
+ Android, Jetpack Compose, Kotlin, SQLite
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
+ Unit tests
+ Instrumented Integration Tests (DAO tests, AndroidJUnit4)
+ Screenshot tests (AndroidJUnit4)

This is called feature-based packaging.

## Code analysis and testing
+ ./gradlew detekt
+ ./gradlew lint
+ Run SonarCube
+ Run androidTest (Screenshot tests, DAO tests)
+ Run test (Unit tests)

## Features to be implemented still
+ create full backup support.
+ Project name edit and validation in SingleProjectScreen
+ Project name validation in ProjectsScreen. Kilometres validation. 0050 not valid.
+ Workday: “Open End Time picker to select time and confirm to show the total work time of the day”
+ Ask to save when coming back from or switching to a screen.
+ Show Toast when saving data or confirming.
+ Validation
+ "Note! Automatic calculations for fields below."
+ Thorough testing of all features in all screens
  + Intro,
  + Calendar,
  + Projects,
  + Single project,
  + Project Details,
  + Settings
+ Orientation support
+ detekt, lint, sonarcube run regularly, preferably before pushing code
+ Translate fed "Work type"
+ Move calculateWorkTimeToday from SingleProjectScreen to SingleProjectViewModel
+ Modify "New day" text
+ Edit project broken
+ When is Flex time total updated?
