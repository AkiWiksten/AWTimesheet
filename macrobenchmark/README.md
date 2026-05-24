# Macrobenchmark module

Comprehensive performance testing for **app startup**, **frame rendering**, and **composition efficiency**.

**10 benchmarks** across 3 test classes, targeting key user-facing screens:
- 2 startup tests (cold/warm)
- 3 scroll & jank tests
- 5 recomposition efficiency tests

---

## Benchmark Matrix

| Class | Test | Measures | Key Metric | Target |
|-------|------|----------|-----------|--------|
| **Startup** | `startupCold` | Time from process kill to first draw | `timeToInitialDisplayMs` (p50) | < 500 ms |
| | `startupWarm` | Time from backgrounded to first draw | `timeToInitialDisplayMs` (p50) | < 150 ms |
| **Scroll** | `calendarScrollFrameTiming` | Jank during calendar list scroll | `frameOverrunMs` jank % | < 5% |
| | `workdayScrollFrameTiming` | Jank during workday list scroll | `frameOverrunMs` jank % | < 5% |
| | `settingsScrollFrameTiming` | Jank during settings list scroll | `frameOverrunMs` jank % | < 5% |
| **Recomposition** | `calendarScreenRecompositions` | Compose recompositions on calendar | `frameDurationCpuMs` >16ms % | < 10% |
| | `workdayScreenRecompositions` | Compose recompositions on workday | `frameDurationCpuMs` >16ms % | < 10% |
| | `settingsScreenRecompositions` | Compose recompositions on settings | `frameDurationCpuMs` >16ms % | < 10% |
| | `projectDetailsScreenRecompositions` | Compose recompositions when editing details | `frameDurationCpuMs` >16ms % | < 10% |
| | `singleProjectScreenRecompositions` | Compose recompositions when editing single project | `frameDurationCpuMs` >16ms % | < 10% |

---

## Included benchmarks

### Startup Benchmarks
- `StartupBenchmark.startupCold` - Startup timing with process kill (most realistic real-world scenario)
- `StartupBenchmark.startupWarm` - Startup timing with app in memory (faster, common after backgrounding)

### Scroll & Frame Timing Benchmarks
- `ScrollBenchmark.calendarScrollFrameTiming` - Jank detection during calendar list scrolling
- `ScrollBenchmark.workdayScrollFrameTiming` - Jank detection during workday list scrolling  
- `ScrollBenchmark.settingsScrollFrameTiming` - Jank detection during settings list scrolling

### Recomposition Efficiency Benchmarks
- `RecompositionBenchmark.calendarScreenRecompositions` - Composition performance on calendar screen interactions
- `RecompositionBenchmark.workdayScreenRecompositions` - Composition performance on workday screen interactions
- `RecompositionBenchmark.settingsScreenRecompositions` - Composition performance on settings screen interactions
- `RecompositionBenchmark.projectDetailsScreenRecompositions` - Composition performance when editing project details
- `RecompositionBenchmark.singleProjectScreenRecompositions` - Composition performance when editing single project entry

## Common Commands

### Run All Benchmarks (10 tests, ~15–20 minutes on real device)
```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest
```

### Run by Category

**Startup only** (2 tests, profile-dependent runtime):
```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark"
```

**Startup profiles and overrides**

- `local` profile (default): `STARTUP_ITERATIONS_LOCAL` in `BenchmarkConfig.kt`
- `ci` profile: `STARTUP_ITERATIONS_CI` in `BenchmarkConfig.kt`
- Explicit `startupIterations` override always wins

```powershell
# Startup with explicit local profile
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark" "-Pandroid.testInstrumentationRunnerArguments.startupProfile=local"

# Startup with CI profile
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark" "-Pandroid.testInstrumentationRunnerArguments.startupProfile=ci"

# Startup with explicit iteration override (highest priority)
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark" "-Pandroid.testInstrumentationRunnerArguments.startupIterations=10"
```

**Scroll/Jank only** (3 tests, ~7 minutes):
```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.ScrollBenchmark"
```

**Recomposition only** (5 tests, ~10 minutes):
```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.RecompositionBenchmark"
```

### Run Single Test (PowerShell)

**Note:** Keep `#` inside quotes to avoid comment interpretation:
```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark#startupCold"
```

**Examples:**
```powershell
# Startup
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark#startupWarm"

# Scroll
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.ScrollBenchmark#calendarScrollFrameTiming"

# Recomposition
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.RecompositionBenchmark#singleProjectScreenRecompositions"
```

## Interpret Results

### Startup Metrics
```
startupCold,timeToInitialDisplayMs,310.0
startupWarm,timeToInitialDisplayMs,92.0
```
- **Cold < 400 ms**: ✓ Excellent for mobile
- **Cold 400–600 ms**: ⚠ Acceptable, monitor
- **Cold > 600 ms**: ❌ Investigate DB init, startup code

### Jank Metrics (Scroll Benchmarks)
```
calendarScrollFrameTiming,frameOverrunMs,jank%=2.50;missedFrames=5
```
- **Jank < 5%**: ✓ Smooth scrolling
- **Jank 5–10%**: ⚠ Occasional stutter
- **Jank > 10%**: ❌ Visible jank, optimize scrollable lists

### Long Frame Metrics (Recomposition Benchmarks)
```
calendarScreenRecompositions,frameDurationCpuMs,longFrames>16ms%=8.0;count=16
```
- **Long frames < 10%**: ✓ Composition efficient
- **Long frames 10–20%**: ⚠ Recompositions causing drops
- **Long frames > 20%**: ❌ Use `derivedStateOf`, `remember`, or `LazyColumn`

---

## Workflow: Baseline → Change → Retest

### 1. Establish Baseline
```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest
python -u .\macrobenchmark\tools\summarize_benchmark.py | Tee-Object baseline.txt
```

### 2. Make Performance Improvement

Example: Optimize calendar recomposition by adding `derivedStateOf()`:
```kotlin
val selectedDateState = derivedStateOf { selectedDate.value }
```

### 3. Retest Same Benchmark
```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.RecompositionBenchmark#calendarScreenRecompositions"
python -u .\macrobenchmark\tools\summarize_benchmark.py
```

### 4. Compare Metrics
```
Before: longFrames>16ms%=12.0
After:  longFrames>16ms%=7.5
✓ 37% improvement!
```

---

## Troubleshooting

### Device Disconnection
```
Error: No connected devices!
```
**Solution:** Reconnect device and verify USB debugging is enabled:
```powershell
adb devices
```
You should see `device_id  device` (not `offline`).

### Stale Object Exceptions
```
androidx.test.uiautomator.StaleObjectException
```
**Solution:** This usually occurs when UI changes during benchmark interactions. The retry logic in recomposition benchmarks should handle this automatically. If persistent:
1. Increase `CLICK_RETRY_COUNT` in `RecompositionBenchmark.kt`
2. Run on a less busy device/emulator

### No Benchmark Output Found
```
No benchmark JSON records found.
```
**Solution:** Benchmarks may still be running or failed silently. Check:
```powershell
ls .\macrobenchmark\build\outputs\connected_android_test_additional_output\
```

### Test Compilation Errors
```
Unresolved reference 'TARGET_PACKAGE'
```
**Solution:** Recompile the benchmark module:
```powershell
.\gradlew.bat :macrobenchmark:compileBenchmarkKotlin
```

After a run, summarize benchmark JSON files with:

```powershell
python -u .\macrobenchmark\tools\summarize_benchmark.py
```

Optional custom scan path:

```powershell
python -u .\macrobenchmark\tools\summarize_benchmark.py .\macrobenchmark\build
```

Example threshold gate with recomposition limits (CI-friendly, exits with code 1 on failure):

```powershell
python -u .\macrobenchmark\tools\summarize_benchmark.py .\macrobenchmark\build --max-jank-percent 5 --max-long-frames-percent 10 --max-recompositions 500 --fail-on-missing-runs
```

Gradle wrapper tasks are also available:

```powershell
.\gradlew.bat :macrobenchmark:summarizePerf
```

```powershell
.\gradlew.bat :macrobenchmark:verifyPerf
```

From project root, aliases are available too:

```powershell
.\gradlew.bat verifyPerf
.\gradlew.bat summarizePerf
```

Customize gate thresholds or Python executable:

```powershell
.\gradlew.bat :macrobenchmark:verifyPerf -Pperf.maxJankPercent=4 -Pperf.maxLongFramesPercent=8 -Pperf.maxRecompositions=500 -Pperf.pythonExecutable=python
```

Default values are centralized in `gradle.properties` (`perf.*`).

The summary highlights:
- startup timing / first frame metrics
- frame overrun metrics (jank and missed frames estimate)
- long frames over 16 ms (`frameDurationCpuMs` based estimate)
- recomposition events during interactions (when available)


