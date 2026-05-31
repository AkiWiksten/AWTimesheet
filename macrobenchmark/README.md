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
| **Scroll** | `calScroll` | Jank during calendar list scroll | `frameOverrunMs` jank % | < 5% |
| | `workdayScroll` | Jank during workday list scroll | `frameOverrunMs` jank % | < 5% |
| | `settingsScroll` | Jank during settings list scroll | `frameOverrunMs` jank % | < 5% |
| **Recomposition** | `calRecomp` | Compose recompositions on calendar | `frameDurationCpuMs` >16ms % | < 10% |
| | `workdayRecomp` | Compose recompositions on workday | `frameDurationCpuMs` >16ms % | < 10% |
| | `settingsRecomp` | Compose recompositions on settings | `frameDurationCpuMs` >16ms % | < 10% |
| | `projDetailsRecomp` | Compose recompositions when editing details | `frameDurationCpuMs` >16ms % | < 10% |
| | `singleProjRecomp` | Compose recompositions when editing single project | `frameDurationCpuMs` >16ms % | < 10% |

---

## Included benchmarks

### Startup Benchmarks
- `StartupBenchmark#startupCold` - Startup timing with process kill (most realistic real-world scenario)
- `StartupBenchmark#startupWarm` - Startup timing with app in memory (faster, common after backgrounding)

### Scroll & Frame Timing Benchmarks
- `ScrollBenchmark#calScroll` - Jank detection during calendar list scrolling
- `ScrollBenchmark#workdayScroll` - Jank detection during workday list scrolling  
- `ScrollBenchmark#settingsScroll` - Jank detection during settings list scrolling

### Recomposition Efficiency Benchmarks
- `RecompBm#calRecomp` - Composition performance on calendar screen interactions
- `RecompBm#workdayRecomp` - Composition performance on workday screen interactions
- `RecompBm#settingsRecomp` - Composition performance on settings screen interactions
- `RecompBm#projDetailsRecomp` - Composition performance when editing project details
- `RecompBm#singleProjRecomp` - Composition performance when editing single project entry

Calendar/workday-facing scroll and recomposition benchmarks seed a realistic dataset
once during setup so the target UI has visible content before measurement begins.
For Workday editor flows, navigation to `SingleProjectScreen` first tries the Add
action and can fall back to selecting an existing project and using Edit when the
action row is off-screen or long lists make direct Add navigation less reliable.

## Common Commands

### Run All Benchmarks (10 tests, ~15–20 minutes on real device)
```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest
.\gradlew.bat sequentialBenchmarks <- USE THIS
.\gradlew.bat sequentialBenchmarksContinue
.\gradlew.bat sequentialBenchmarksExisting
.\gradlew.bat sequentialBenchmarksClean
.\gradlew.bat sequentialBenchmarksEmpty
```

`sequentialBenchmarks` and `sequentialBenchmarksContinue` run startup tests with
`startupDataset=empty` by default (faster local feedback) and use the safer
`startupProfile=ci` startup setting by default to reduce device stress in long
sequential sessions. Use `sequentialBenchmarksExisting` for realistic returning-
user startup data (slower). Scroll and recomposition benchmarks seed realistic
data in their own setup blocks; the `startupDataset` argument only affects
`StartupBenchmark`.

### Run All Benchmarks Sequentially (one benchmark per invocation)

Use this when running all 10 in one invocation causes device/ADB instability.
The runner archives each benchmark's JSON output into a session folder and the
final summary is generated from that full session (not just the latest test).
The script also waits for the device to become healthy before each benchmark,
adds a cooldown between runs, and attempts recovery when the device disconnects
after a failed benchmark.

```powershell
powershell -ExecutionPolicy Bypass -File .\macrobenchmark\run_benchmarks_sequential.ps1
```

Optional flags:

```powershell
# Continue with remaining benchmarks even if one fails
powershell -ExecutionPolicy Bypass -File .\macrobenchmark\run_benchmarks_sequential.ps1 -ContinueOnFailure

# Print commands without executing them
powershell -ExecutionPolicy Bypass -File .\macrobenchmark\run_benchmarks_sequential.ps1 -DryRun

# Show full Gradle task logs (disables quiet mode)
powershell -ExecutionPolicy Bypass -File .\macrobenchmark\run_benchmarks_sequential.ps1 -VerboseGradle

# Run sequential suite with empty startup dataset profile
powershell -ExecutionPolicy Bypass -File .\macrobenchmark\run_benchmarks_sequential.ps1 -StartupDataset empty

# Use local startup profile instead of the safer default CI profile
powershell -ExecutionPolicy Bypass -File .\macrobenchmark\run_benchmarks_sequential.ps1 -StartupProfile local

# Shorten or disable cooldown between benchmarks (0 disables)
powershell -ExecutionPolicy Bypass -File .\macrobenchmark\run_benchmarks_sequential.ps1 -CooldownSeconds 10

# Wait longer for the device to come back after a crash/reboot
powershell -ExecutionPolicy Bypass -File .\macrobenchmark\run_benchmarks_sequential.ps1 -DeviceRecoveryTimeoutSeconds 300

# Fail a single benchmark run if it exceeds 6 minutes (default is 420 seconds)
powershell -ExecutionPolicy Bypass -File .\macrobenchmark\run_benchmarks_sequential.ps1 -BenchmarkTimeoutSeconds 360
```

### Run by Category

**Startup only** (2 tests, profile-dependent runtime):
```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark#startupCold"
```

**Startup profiles and overrides**

- `local` profile (default): `STARTUP_ITERATIONS_LOCAL` in `BenchmarkConfig.kt` (now 1 iteration for faster local development)
- `ci` profile: `STARTUP_ITERATIONS_CI` in `BenchmarkConfig.kt` (1 iteration)
- Explicit `startupIterations` override always wins
- `startupIncludeFrameTiming` (optional): `true`/`false`, default `false`
- `startupDataset` (optional): `empty` (default for faster local runs) or `existing` (seeds realistic data for more accurate measurements)
- `startupDataset` only applies to `StartupBenchmark`; other benchmarks seed realistic workday/calendar data during setup

```powershell
# Startup with explicit local profile
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark" "-Pandroid.testInstrumentationRunnerArguments.startupProfile=local"

# Startup with CI profile
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark" "-Pandroid.testInstrumentationRunnerArguments.startupProfile=ci"

# Startup with explicit iteration override (highest priority)
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark" "-Pandroid.testInstrumentationRunnerArguments.startupIterations=10"

# Startup with frame timing diagnostics enabled (off by default)
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark#startupCold" "-Pandroid.testInstrumentationRunnerArguments.startupIncludeFrameTiming=true"

# Startup with realistic returning-user dataset (better for accurate measurements, slower)
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark#startupCold" "-Pandroid.testInstrumentationRunnerArguments.startupDataset=existing"

# Startup cold app with 3 iterations for accurate baseline (slower, ~3-4 minutes on a device)
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark#startupCold" "-Pandroid.testInstrumentationRunnerArguments.startupIterations=3"

# Startup with empty DB first-run dataset
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark#startupCold" "-Pandroid.testInstrumentationRunnerArguments.startupDataset=empty"
```

**Scroll/Jank only** (3 tests, ~7 minutes):
```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.ScrollBenchmark"
```

**Recomposition only** (5 tests, ~10 minutes):
```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.RecompBm"
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
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.ScrollBenchmark#calScroll"

# Recomposition
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.RecompBm#singleProjRecomp"
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
 calScroll,frameOverrunMs,jank%=2.50;missedFrames=5
```
- **Jank < 5%**: ✓ Smooth scrolling
- **Jank 5–10%**: ⚠ Occasional stutter
- **Jank > 10%**: ❌ Visible jank, optimize scrollable lists

### Long Frame Metrics (Recomposition Benchmarks)
```
 calRecomp,frameDurationCpuMs,longFrames>16ms%=8.0;count=16
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
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.RecompBm#calRecomp"
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

If you are using the sequential runner, it now waits for the device before each
benchmark and attempts recovery after disconnects. If the phone fully reboots,
rerun the sequential command once the device is visible again.

### Stale Object Exceptions
```
androidx.test.uiautomator.StaleObjectException
```
**Solution:** This usually occurs when UI changes during benchmark interactions. The retry logic in recomposition benchmarks should handle this automatically. If persistent:
1. Increase `CLICK_RETRY_COUNT` in `RecompositionBenchmark.kt`
2. Prefer the sequential runner so each benchmark starts from a cleaner device state
3. Run on a less busy device/emulator

### Workday editor navigation is flaky
```
Could not open SingleProject screen from Workday Add action
```
**Solution:** The benchmark already retries Add, scrolls toward the action row, and
can fall back to selecting an existing project and using Edit. If this still
persists, verify the seeded dataset is present and rerun the single benchmark first:
```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.RecompBm#singleProjRecomp"
```

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
- recomposition details during interactions when available (`recompositionCount`, plus inline frame-timing detail for recomposition benchmarks)




