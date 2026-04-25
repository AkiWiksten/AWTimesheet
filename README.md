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
+ Validation
+ "Note! Automatic calculations for fields below."
+ Orientation support
+ Translate fed "Work type"
+ Move calculateWorkTimeToday from SingleProjectScreen to SingleProjectViewModel
+ When is Flex time total updated?
+ App localization selection
+ "Clear day" → "Clear details"
+ Use "Design" and "Other" as Default work types when list is empty
+ work_type_help
+ add_project_item_failed_title add_project_item_failed_title_text (Duplicate)
+ Everything with bigger font
+ Selecting work type doesn't work in edit mode
+ DialogMainFields
+ Flex time total doesn't update
+ Use Intent to ask user where to save pdf
+ PDF: Project time sum, Half-allowance and Full-allowance kilometre sum for each project
+ ProjectsScreen: "No projects" state should be shown on a empty list
+ Calculate FlexTimeTotal from WorkdayEntity
+ Ask to save when leaving workday
+ Clean ProjectDetailsState
+ "Lunch time estimate" → Ask to save globally to "Daily lunch time estimate" in SettingsScreen
+ ProjectDetailsRepository → Extract WorkStats stuff into own repository


Constantly:
+ Thorough testing of all features in all screens
    + Intro,
        + Overall functionality
    + Calendar,
    + Projects,
    + Single project,
    + Project Details,
    + Settings
+ detekt, lint, SonarQube run regularly, preferably before pushing code
