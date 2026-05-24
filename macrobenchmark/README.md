# Macrobenchmark module

This module measures app startup and frame timing for key Compose paths.

## Included benchmarks

- `StartupBenchmark.startupCold`
- `StartupBenchmark.startupWarm`
- `ScrollBenchmark.calendarScrollFrameTiming`
- `ScrollBenchmark.workdayScrollFrameTiming`
- `ScrollBenchmark.settingsScrollFrameTiming`

## Run benchmarks

```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest
```

Run a single class if needed:

```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark
```

## Summarize output

After a run, summarize benchmark JSON files with:

```powershell
python -u .\macrobenchmark\tools\summarize_benchmark.py
```

Optional custom scan path:

```powershell
python -u .\macrobenchmark\tools\summarize_benchmark.py .\macrobenchmark\build
```

Example threshold gate (CI-friendly, exits with code 1 on failure):

```powershell
python -u .\macrobenchmark\tools\summarize_benchmark.py .\macrobenchmark\build --max-jank-percent 5 --max-long-frames-percent 10 --fail-on-missing-runs
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
.\gradlew.bat :macrobenchmark:verifyPerf -Pperf.maxJankPercent=4 -Pperf.maxLongFramesPercent=8 -Pperf.pythonExecutable=python
```

Default values are centralized in `gradle.properties` (`perf.*`).

The summary highlights:
- startup timing / first frame metrics
- frame overrun metrics (jank and missed frames estimate)
- long frames over 16 ms (`frameDurationCpuMs` based estimate)





