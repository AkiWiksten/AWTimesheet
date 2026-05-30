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
9. Run MacroBenchmark occasionally .\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest

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

## My manual backlog
+ General
  + Test implemented backup & sync (android:allowBackup="true")
  + Dark theme
  + Check test coverage and add tests
  + Warnings on build
  + Configure Python interpreter for benchmark summarization script
+ Repositories
  + CalendarRepository: Add interface also
+ SingleProjectScreen
  + Project name edit and validation
  + Kilometres validation. 0050 not valid.
  + Select distance from map also
  + Show work_type_help string
  + Selecting work type doesn't work in edit mode
  + Add full flex time day (Zero time project, add work type "Absence->Flex time")
+ WorkdayScreen 
  + Don't show project_names projects, when there are already recorded projects
  + Unselect project from list
  + Select many in list
  + Select all in list
  + Delete all in list
  + Delete selected in list
  + For Absence->Flex time, add "Flex time by date" by -"Work time by date estimate"
+ ProjectDetailsScreen
  + "Lunch time estimate" Ask to save globally to "Daily lunchtime estimate" in SettingsScreen
+ CalendarScreen
+ SettingsScreen
  + Absence.
    + Add/update/delete vacation range into database. Start and end date by picker
    + Paid/unpaid/Sick leave/Flex day
    + Show next in SettingsScreen, others in a list
  + Translate fed "Work type"
  + App localization selection
+ Excel
  + Show start date in a month first, e.g. 15th.
  + Handle by selecting start date 
    + monthly reports 
    + weekly reports
## What to test constantly
+ Thorough regression testing of all features in all screens
    + Intro, 
    + Calendar, 
    + Workday, R
    + Single project, R
    + Project Details, 
    + Settings R
      + Excel creation R

## AI provided quality improvement plan
+ Fix scroll jank regressions on workday first
  + Latest benchmark showed workdayScroll over the <5% jank target (marginal fail).
  + Start with features/workday list item composables: reduce recomposition scope, stabilize item keys, avoid expensive modifiers/draw ops in scrolling rows.
  + Re-run only this benchmark before broader runs.
+ Turn performance checks into stricter release gates
  + You already have :macrobenchmark:verifyPerf; make it mandatory in CI for protected branches.
  + Keep thresholds in gradle.properties but enforce fail-on-missing-runs and consistent device profile.
  + This prevents “slow creep” over time.
+ Increase test coverage where behavior risk is highest
  + Prioritize feature ViewModels and cross-screen flows (calendar → workday → projectdetails).
  + Add more integration-level tests around data + domain seams, not only isolated unit tests.
  + Your structure is testable; now push coverage depth where user-facing bugs happen most.
+ Strengthen architectural boundaries further
  + You already enforce some module rules in build.gradle.kts (verifyModuleBoundaries); extend rules to prevent accidental feature-to-feature coupling where not intended.
  + From your own backlog, define missing contracts (e.g., CalendarRepository interface) to reduce concrete dependency spread.
+ Complete unresolved reliability TODOs from your backlog
  + Highest-value backlog items: save-confirmation consistency, validation correctness, and state persistence behavior.
  + These directly affect trust and perceived quality more than cosmetic polish.
+ Standardize quality gates into one “definition of done”
  + Keep one required pipeline: unit tests + lint/detekt + boundary check + screenshot validation + perf gate.
  + This improves consistency and developer productivity by removing ambiguity.
+ Refine recomposition hot paths on heavy screens
  + Even with passing long-frame metrics, reduce noisy frame-overrun areas (calendar/projectdetails) proactively.
  + Focus on state hoisting, memoization (remember, derivedStateOf where appropriate), and avoiding broad state invalidations.

