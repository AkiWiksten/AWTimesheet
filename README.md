# AWWorkTime 3.0
## Purpose
+ An app for work time management
## Branches
+ main: default "release" branch, master: "construction" branch
## Features
+ **Intro** for a fancy introduction animation
+ **Calendar** to select a date to manage
+ **Projects** to manage projects for selected date
  + **Single project** to manage a single project
    + **Project Details** to feed and calculate start, end, work time, FlexTime, breaks and lunch.
+ **Settings** to manage settings, like name, employer work type choices (Coding, Installation, Design etc.)
  + **Pdf monthly report** shows your daily work time per projects in a table. Calculates also monthly statistics.
## Main Technologies
+ Android, Jetpack Compose, Kotlin, Room
## Modern App Architecture
### Navigation3
### Hilt
### Structure (Compose + MVVM), based on Clean Architecture
    
A clean, scalable example of this is:
+ app/
    + core/                 # shared utilities, theme, base classes
    + data/                 # repositories, API, database
    + domain/               # use cases, business logic
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
+ Run androidTest (DAO tests)
+ Run test (Unit tests)
+ Run official Compose screenshot baseline update: ./gradlew updateScreenshotTest
+ Run official Compose screenshot validation: ./gradlew validateScreenshotTest (gradlew :{module}:validate{Variant}ScreenshotTest)

## Features to be implemented still
+ General
  + create full backup support.
  + Sync
  + Better orientation support
  + Dark theme
  + FormChangeRules outside ui package
  + Simplify SaveSettingsUseCase
  + Check test coverage and add tests
+ Constants
  + Regroup
+ SingleProjectScreen
  + Project name edit and validation
  + Kilometres validation. 0050 not valid.
  + Show work_type_help string
  + Handle duplicate project name, when pressing "Details" or saving
  + Selecting work type doesn't work in edit mode
  + Rename DialogMainFields -> UpperTextFields
  + Own ViewModel. Decouple from WorkdayViewModel.
+ WorkdayScreen 
  + Calculate FlexTimeTotal from WorkdayEntity? Done already?
  + Ask to save when leaving without saving
  + Don't show project_names projects, when there are already recorded projects
  + 
+ ProjectDetailsScreen
  + "Clear day" → "Clear details"
  + Remove ProjectDetailsState.flexTimeToday ProjectDetailsEntity.flexTimeToday
  + "Lunch time estimate" → Ask to save globally to "Daily lunch time estimate" in SettingsScreen
  + First "Estimated end time" then "End time"
+ CalendarScreen
  + "Note! Automatic calculations for most fields in this app."
  + Show color for modified days
+ SettingsScreen
  + Translate fed "Work type"
  + App localization selection
  + Use "Design" and "Other" as Default work types when list is empty
  + Ask to save does not work
  + Remove SettingsState.projectsByMonth
+ PDF
  + Use Intent to ask user where to save pdf and 
  + show the pdf instantly on screen
  + make pdf dynamic?
  + Project time sum, Half-allowance and Full-allowance kilometre sum for each project
+ Screenshot tests
  + Expand official Compose screenshot coverage and maintain baselines with validateScreenshotTest

## What to test constantly
+ Thorough testing of all features in all screens
    + Intro,
    + Calendar,
    + Projects,
    + Single project,
    + Project Details,
    + Settings
      + PDF creation
