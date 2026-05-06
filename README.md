# AWWorkTime 3.0
## Purpose
+ An app for work time management
## Branches
+ main: default "release" branch, master: "construction" branch
## Features
+ **Intro** for a fancy introduction animation
+ **Calendar** to select a date to manage
+ **Workday** to manage projects for selected date
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
+ Screenshot tests

This is called feature-based packaging.

## Code analysis and testing
1. Run unit tests
2. Run official Compose screenshot validation: ./gradlew validateScreenshotTest (gradlew :{module}:validate{Variant}ScreenshotTest)
3. (If needed, run official Compose screenshot baseline update: ./gradlew updateScreenshotTest) "Compose Preview Screenshot Testing"
4. (If needed, run androidTest (DAO tests))
5. ./gradlew detekt
6. ./gradlew lint
7. Run SonarCube

## Features to be implemented still
+ General
  + create full backup support.
  + Sync
  + Better orientation support
  + Dark theme
  + FormChangeRules outside ui package
  + Check test coverage and add tests
  + Warnings on build
  + isNewDay -> isNewDayOrProject
+ Constants
  + Regroup
+ SingleProjectScreen
  + Project name edit and validation
  + Kilometres validation. 0050 not valid.
  + Show work_type_help string
  + Selecting work type doesn't work in edit mode
  + Rename DialogMainFields -> UpperTextFields
  + Own ViewModel. Decouple from WorkdayViewModel.
+ WorkdayScreen 
  + Ask to save when leaving without saving (check other screens also)
  + Don't show project_names projects, when there are already recorded projects
+ ProjectDetailsScreen
  + "Lunch time estimate" → Ask to save globally to "Daily lunch time estimate" in SettingsScreen
+ CalendarScreen
  + "Note! Automatic calculations for most fields in this app."
+ SettingsScreen
  + Translate fed "Work type"
  + App localization selection
  + Ask to save does not work
  + SettingsViewModel.refreshProjectsByMonth maybe be called when pushing pdf button, not on VM init
+ PDF
  + Use Intent to ask user where to save pdf and 
  + show the pdf instantly on screen
  + make pdf dynamic?
  + Project time sum, Half-allowance and Full-allowance kilometre sum for each project

## What to test constantly
+ Thorough testing of all features in all screens
    + Intro,
    + Calendar,
    + Projects,
    + Single project,
    + Project Details,
    + Settings
      + PDF creation
