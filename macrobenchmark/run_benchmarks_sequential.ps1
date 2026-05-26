param(
    [switch]$ContinueOnFailure,
    [switch]$DryRun,
    [switch]$SummarizeAfterEach,
    [switch]$SummarizeAtEnd = $true,
    [switch]$VerboseGradle
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$gradlew = Join-Path $projectRoot "gradlew.bat"
$summaryScript = Join-Path $PSScriptRoot "tools\summarize_benchmark.py"
$additionalOutputDir = Join-Path $PSScriptRoot "build\outputs\connected_android_test_additional_output"
$sessionRoot = Join-Path $PSScriptRoot ("build\sequential_sessions\" + (Get-Date -Format "yyyyMMdd_HHmmss"))

if (-not (Test-Path $gradlew)) {
    throw "Could not find gradle wrapper at '$gradlew'."
}

New-Item -ItemType Directory -Path $sessionRoot -Force | Out-Null

function Save-BenchmarkJsonSnapshot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Benchmark
    )

    if (-not (Test-Path $additionalOutputDir)) {
        return
    }

    $safeBenchmark = ($Benchmark -replace "[^A-Za-z0-9._-]", "_")
    $destDir = Join-Path $sessionRoot $safeBenchmark
    New-Item -ItemType Directory -Path $destDir -Force | Out-Null

    $jsonFiles = Get-ChildItem -Path $additionalOutputDir -Recurse -File -Filter "*.json" -ErrorAction SilentlyContinue
    foreach ($jsonFile in $jsonFiles) {
        # Prefix with source folder to avoid collisions across benchmark runs.
        $sourceFolder = Split-Path -Leaf (Split-Path -Parent $jsonFile.FullName)
        $destName = "$sourceFolder`_$($jsonFile.Name)"
        Copy-Item -Path $jsonFile.FullName -Destination (Join-Path $destDir $destName) -Force
    }
}

$benchmarks = @(
    "com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark#startupCold",
    "com.akiwiksten.awtimesheet.macrobenchmark.StartupBenchmark#startupWarm",
    "com.akiwiksten.awtimesheet.macrobenchmark.ScrollBenchmark#calScroll",
    "com.akiwiksten.awtimesheet.macrobenchmark.ScrollBenchmark#workdayScroll",
    "com.akiwiksten.awtimesheet.macrobenchmark.ScrollBenchmark#settingsScroll",
    "com.akiwiksten.awtimesheet.macrobenchmark.RecompBm#calRecomp",
    "com.akiwiksten.awtimesheet.macrobenchmark.RecompBm#workdayRecomp",
    "com.akiwiksten.awtimesheet.macrobenchmark.RecompBm#settingsRecomp",
    "com.akiwiksten.awtimesheet.macrobenchmark.RecompBm#projDetailsRecomp",
    "com.akiwiksten.awtimesheet.macrobenchmark.RecompBm#singleProjRecomp"
)

$failed = New-Object System.Collections.Generic.List[string]

Push-Location $projectRoot
try {
    foreach ($benchmark in $benchmarks) {
        $cmdArgs = @(
            ":macrobenchmark:connectedBenchmarkAndroidTest",
            "-Pandroid.testInstrumentationRunnerArguments.class=$benchmark"
        )

        if (-not $VerboseGradle) {
            $cmdArgs = @("-q", "--console=plain", "--no-daemon") + $cmdArgs
        } else {
            $cmdArgs = @("--no-daemon") + $cmdArgs
        }

        Write-Host "================================================================================"
        Write-Host "Running benchmark: $benchmark"
        Write-Host "================================================================================"

        if ($DryRun) {
            Write-Host "$gradlew $($cmdArgs -join ' ')"
            continue
        }

        & $gradlew @cmdArgs
        $exitCode = $LASTEXITCODE

        Save-BenchmarkJsonSnapshot -Benchmark $benchmark

        if ($exitCode -ne 0) {
            $failed.Add($benchmark)
            Write-Warning "Benchmark failed: $benchmark (exit code $exitCode)"
            if (-not $ContinueOnFailure) {
                break
            }
        }

        if ($SummarizeAfterEach -and (Test-Path $summaryScript)) {
            python -u $summaryScript $sessionRoot
        }
    }

    if (-not $DryRun -and $SummarizeAtEnd -and (Test-Path $summaryScript)) {
        Write-Host ""
        Write-Host "Final benchmark summary"
        python -u $summaryScript $sessionRoot
        Write-Host "Session summary path: $sessionRoot"
    }
}
finally {
    Pop-Location
}

if ($failed.Count -gt 0) {
    Write-Host ""
    Write-Host "Failed benchmarks:" -ForegroundColor Yellow
    $failed | ForEach-Object { Write-Host " - $_" }
    exit 1
}

Write-Host ""
Write-Host "All sequential benchmark runs completed." -ForegroundColor Green

