# AWTimesheet
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
    
A clean, scalable example of this project is:
+ app/                      # android application module (wiring, nav host, DI entry points)
+ core/                     # shared utilities, common UI/theme, helpers
+ data/                     # database/entities/dao and data sources
+ domain/                   # use cases and domain models/repository contracts
+ build-logic/              # Gradle convention plugins shared by all modules
+ features/
    + intro/
    + calendar/
    + workday/
    + singleproject/
    + projectdetails/
    + settings/
+ Unit tests
+ Instrumented Integration Tests (DAO tests, AndroidJUnit4)
+ Screenshot tests

This is called feature-based packaging.

### Build conventions

Project build configuration is centralized with convention plugins in `build-logic/`.

- `awtimesheet.android.base`
- `awtimesheet.android.compose.app`
- `awtimesheet.android.compose.feature`
- `awtimesheet.feature.dependencies`

See `build-logic/README.md` for details and usage examples.

## Code analysis and testing
1. Run unit tests (`.\gradlew.bat testDebugUnitTest` or `.\gradlew.bat :{module}:test{Variant}UnitTest`)
2. Run official Compose screenshot validation: `.\gradlew.bat validateScreenshotTest` (`.\gradlew.bat :{module}:validate{Variant}ScreenshotTest`)
3. (If needed, run official Compose screenshot baseline update: `.\gradlew.bat updateScreenshotTest`) "Compose Preview Screenshot Testing"
4. (If needed, run androidTest (DAO tests))
5. `.\gradlew.bat detekt`
6. `.\gradlew.bat lint`
7. `.\gradlew.bat --no-configuration-cache verifyModuleBoundaries`
8. SonarCube runs automatically on new code
9. Run MacroBenchmark .\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest

## Macrobenchmark

**10 comprehensive performance benchmarks** covering startup timing, scroll jank, and Compose recomposition efficiency:

- **Startup** (2 tests): cold/warm app launch timing
- **Scroll** (3 tests): jank detection on calendar, workday, settings lists
- **Recomposition** (5 tests): composition efficiency on calendar, workday, settings, project details, single project screens

Run benchmarks on a connected device (recommended) or emulator:

`.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest`

Quick reference:

`macrobenchmark/BENCHMARK_QUICK_START.md` ← **Start here** for 30-second overview and common commands

`macrobenchmark/README.md` ← Full benchmark matrix, workflows, troubleshooting

Summarize benchmark output:

`python -u .\macrobenchmark\tools\summarize_benchmark.py`

Example CI gate (fails on jank > 5%, long frames > 10%):

`python -u .\macrobenchmark\tools\summarize_benchmark.py .\macrobenchmark\build --max-jank-percent 5 --max-long-frames-percent 10 --fail-on-missing-runs`

Or use Gradle wrapper:

`.\gradlew.bat :macrobenchmark:verifyPerf`

Root-level aliases:

`.\gradlew.bat verifyPerf` or `.\gradlew.bat summarizePerf`

Performance gate defaults (in `gradle.properties`):

- `perf.maxJankPercent=5`
- `perf.maxLongFramesPercent=10`

Override per run:

`.\gradlew.bat verifyPerf -Pperf.maxJankPercent=4 -Pperf.maxLongFramesPercent=8`

## Features to be implemented still
+ General
  + Test implemented backup & sync (android:allowBackup="true")
  + Better orientation support
  + Dark theme
  + Check test coverage and add tests
  + Warnings on build
  + more modules
  + button press animations
  + Refactor tests
  + Configure Python interpreter
+ Constants
  + Regroup
+ Repositories
  + CalendarRepository: Add interface also
+ SingleProjectScreen
  + Project name edit and validation
  + Kilometres validation. 0050 not valid.
  + Select distance from map also
  + Show work_type_help string
  + Selecting work type doesn't work in edit mode
  + Rename DialogMainFields -> UpperTextFields
  + DialogDropdownFields -> DropdownFields
+ WorkdayScreen 
  + Ask to save when leaving without saving (check other screens also)
  + Don't show project_names projects, when there are already recorded projects
  + Add flexTimeTotal into SettingsEntity
    + Update it, when projects by date update
+ ProjectDetailsScreen
  + "Lunch time estimate" â†’ Ask to save globally to "Daily lunchtime estimate" in SettingsScreen
+ CalendarScreen
  + "Remember to click a date if you want to select the visible month!"
  + Better scalability
    + Fetch projects of the current month once in the app lifecycle.
      + Calculate workdaysByMonth (month, year, workdaytime) and weekSum + monthSum from that
    + Update workdaysByMonth when returning from WorkdayScreen
      + Update weekSum + monthSum
    + When changing month, fetch projects and add to workdaysByMonth and so onâ€¦
+ SettingsScreen
  + Translate fed "Work type"
  + App localization selection
  + Ask to save does not work
  + Create Excel report also, or export to Excel, CSV or similar
  + Split into sections
+ Excel
  + Handle monthly, weekly and biweekly reports, by selecting start date
  + Work type section: Add rows if >= 7 work types
## What to test constantly
+ Thorough testing of all features in all screens
    + Intro, 
    + Calendar, 
    + Workday, R
    + Single project, R
    + Project Details, 
    + Settings R
      + PDF creation R

