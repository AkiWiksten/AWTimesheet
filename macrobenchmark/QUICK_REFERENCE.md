# Quick Reference: Recomposition Benchmarks

## Essential Commands

### Run Benchmarks
```powershell
# All benchmarks (startup + scroll + recomposition)
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest

# Recomposition only
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.RecompositionBenchmark

# Single recomposition test (e.g., projectDetailsScreenRecompositions)
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest `
  -Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.RecompositionBenchmark#projectDetailsScreenRecompositions
```

### Analyze Results
```powershell
# Basic summary (CSV format)
python -u .\macrobenchmark\tools\summarize_benchmark.py

# Detailed composition analysis with recommendations
python -u .\macrobenchmark\tools\analyze_composition.py

# Summary with thresholds
python -u .\macrobenchmark\tools\summarize_benchmark.py `
  --max-jank-percent 5 `
  --max-long-frames-percent 10 `
  --max-recompositions 500
```

### CI/CD Gates
```powershell
# Run + verify against thresholds (exits code 1 on failure)
.\gradlew.bat verifyPerf

# Or with custom thresholds
.\gradlew.bat verifyPerf -Pperf.maxJankPercent=4 -Pperf.maxLongFramesPercent=8
```

## Metric Quick Reference

| Metric | Good | Warning | Bad |
|--------|------|---------|-----|
| Jank % | <5% | 5-10% | >10% |
| Long Frames (>16ms) % | <10% | 10-15% | >15% |
| Startup Cold | <400ms | 400-600ms | >600ms |
| Startup Warm | <100ms | 100-200ms | >200ms |

## Interpreting Results

### CSV Output Example
```
benchmark,metric,p50,p90,p95,max,derived
calendarScreenRecompositions,frameDurationCpuMs,14.2,18.5,22.1,45.3,longFrames>16ms%=28.50;count=17
calendarScreenRecompositions,frameOverrunMs,-1.8,2.5,6.2,29.7,jank%=15.00;missedFrames=9
```

- **p50**: 50th percentile (median)
- **p90/p95**: 90th/95th percentile (high performers)
- **max**: Maximum observed value
- **longFrames%**: Percentage of frames >16ms
- **jank%**: Percentage of janky frames

### Composition Efficiency Analysis Example
```
HIGH PRESSURE BENCHMARKS (1)
calendarScreenRecompositions: HIGH pressure
  Long frames >16ms: 25.3%
  Jank: 18.5%
  Recommendations:
    - Profile with Logcat recomposition logging
    - Use derivedStateOf() for expensive computations
```

## Pressure Levels

**HIGH Pressure** → Action Required
- Long frames >30% OR Jank >20%
- Investigate root causes in CalendarScreen or other components
- Apply optimization patterns from `RECOMPOSITION_GUIDE.md`

**MEDIUM Pressure** → Monitor
- Long frames 15-30% OR Jank 10-20%
- Good candidates for optimization
- Watch for performance regressions

**LOW Pressure** → Good Status
- Long frames <15% AND Jank <10%
- No immediate action needed
- Continue regular monitoring

## Common Issues & Fixes

### Issue: No benchmark results found
**Cause**: Haven't run benchmarks yet
**Fix**: 
```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest
```

### Issue: Device connection timeout
**Cause**: ADB connection unstable
**Fix**: 
```powershell
adb kill-server
adb start-server
adb devices
```

### Issue: Inconsistent results
**Cause**: Device background load, thermal throttling
**Fix**: 
- Close all apps on device
- Run benchmark multiple times
- Check device temperature

### Issue: HIGH pressure but don't see excessive recompositions
**Cause**: Frame time includes drawing/layout, not just recomposition
**Fix**: 
- Use Frame Profiler in Android Studio
- Enable Compose recomposition logging: `adb shell setprop debug.compose.recomposition.count true`

## Optimization Quick Patterns

### Pattern 1: Memoize Expensive Operations
```kotlin
// Before (slow)
val summary = workEntries.groupBy { it.date }.values.sumOf { it.sumOf(Entry::hours) }

// After (fast)
val summary = remember(workEntries) { 
    workEntries.groupBy { it.date }.values.sumOf { it.sumOf(Entry::hours) }
}
```

### Pattern 2: Use derivedStateOf for Dependent State
```kotlin
// Before (recalculates often)
val height = if (isExpanded.value) 500 else 100

// After (memoized)
val height = remember { derivedStateOf { 
    if (isExpanded.value) 500 else 100 
}}
```

### Pattern 3: Add Keys to LazyLists
```kotlin
// Before (items recompose on scroll)
LazyColumn {
    items(workEntries) { entry ->
        WorkEntryItem(entry)
    }
}

// After (only changed items recompose)
LazyColumn {
    items(workEntries, key = { it.id }) { entry ->
        WorkEntryItem(entry)
    }
}
```

### Pattern 4: Use Immutable Data Classes
```kotlin
// Before (mutable, triggers recompositions)
data class UiState(var selectedDate: LocalDate? = null)

// After (immutable)
data class UiState(val selectedDate: LocalDate? = null)
```

## Gradle Properties Setup

**`gradle.properties`** (optional):
```properties
perf.maxJankPercent=5
perf.maxLongFramesPercent=10
perf.maxRecompositions=500
perf.pythonExecutable=python
```

Then use defaults:
```powershell
.\gradlew.bat verifyPerf
```

## Testing Workflow

1. **Baseline**: Run benchmarks and save results
   ```powershell
   .\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest
   python -u .\macrobenchmark\tools\analyze_composition.py > baseline.txt
   ```

2. **Make changes** to your Compose code

3. **Retest**: Run benchmarks again
   ```powershell
   .\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest
   python -u .\macrobenchmark\tools\analyze_composition.py > after_changes.txt
   ```

4. **Compare**: Check if metrics improved or regressed

5. **Choose threshold**: Set realistic targets
   ```powershell
   .\gradlew.bat verifyPerf -Pperf.maxJankPercent=5 -Pperf.maxLongFramesPercent=10
   ```

## Resources

- **Detailed Guide**: `macrobenchmark/RECOMPOSITION_GUIDE.md`
- **Getting Started**: `macrobenchmark/GETTING_STARTED.md`
- **Benchmarks README**: `macrobenchmark/README.md`
- **Compose Performance**: https://developer.android.com/develop/ui/compose/performance
- **Stable API**: https://developer.android.com/develop/ui/compose/performance/stability

---

## All Benchmarks

| Benchmark | Screen | Focus |
|-----------|--------|-------|
| calendarScreenRecompositions | Calendar | Date selection, scrolling |
| workdayScreenRecompositions | Workday | Entry management |
| settingsScreenRecompositions | Settings | Configuration |
| **projectDetailsScreenRecompositions** | **Project Details** | **Form interactions** |
| **singleProjectScreenRecompositions** | **Single Project** | **Entry editing** |

**Bold items** = newly added

---

**Quick Tip**: Bookmark this file for quick reference during daily development!



