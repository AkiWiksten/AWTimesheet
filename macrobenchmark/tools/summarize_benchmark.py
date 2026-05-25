#!/usr/bin/env python3
"""
Summarize AndroidX Benchmark JSON output for startup, scroll jank, and recomposition metrics.

Organizes results by benchmark class (Startup, Scroll, Recomposition) with visual indicators.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any, Iterable

KNOWN_METRICS = (
    "timeToInitialDisplayMs",
    "startupMs",
    "frameDurationCpuMs",
    "frameOverrunMs",
    "recompositionCount",
)
PERCENTILE_KEYS = ("P50", "p50", "median", "P90", "p90", "P95", "p95", "max")

# Benchmark metadata for organization and interpretation
LEGACY_AND_CURRENT_BENCHMARKS = {
    "startupCold": {"class": "Startup", "metric": "timeToInitialDisplayMs", "target": "< 500 ms", "description": "App launch from cold start (process killed)"},
    "startupWarm": {"class": "Startup", "metric": "timeToInitialDisplayMs", "target": "< 150 ms", "description": "App resume from warm start (backgrounded)"},
    "calendarScrollFrameTiming": {"class": "Scroll", "metric": "frameOverrunMs", "target": "jank < 5%", "description": "Calendar list scroll jank"},
    "calScroll": {"class": "Scroll", "metric": "frameOverrunMs", "target": "jank < 5%", "description": "Calendar list scroll jank"},
    "workdayScrollFrameTiming": {"class": "Scroll", "metric": "frameOverrunMs", "target": "jank < 5%", "description": "Workday list scroll jank"},
    "workdayScroll": {"class": "Scroll", "metric": "frameOverrunMs", "target": "jank < 5%", "description": "Workday list scroll jank"},
    "settingsScrollFrameTiming": {"class": "Scroll", "metric": "frameOverrunMs", "target": "jank < 5%", "description": "Settings list scroll jank"},
    "settingsScroll": {"class": "Scroll", "metric": "frameOverrunMs", "target": "jank < 5%", "description": "Settings list scroll jank"},
    "calendarScreenRecompositions": {"class": "Recomposition", "metric": "frameDurationCpuMs", "target": "longFrames < 10%", "description": "Calendar screen composition efficiency"},
    "calRecomp": {"class": "Recomposition", "metric": "frameDurationCpuMs", "target": "longFrames < 10%", "description": "Calendar screen composition efficiency"},
    "workdayScreenRecompositions": {"class": "Recomposition", "metric": "frameDurationCpuMs", "target": "longFrames < 10%", "description": "Workday screen composition efficiency"},
    "workdayRecomp": {"class": "Recomposition", "metric": "frameDurationCpuMs", "target": "longFrames < 10%", "description": "Workday screen composition efficiency"},
    "settingsScreenRecompositions": {"class": "Recomposition", "metric": "frameDurationCpuMs", "target": "longFrames < 10%", "description": "Settings screen composition efficiency"},
    "settingsRecomp": {"class": "Recomposition", "metric": "frameDurationCpuMs", "target": "longFrames < 10%", "description": "Settings screen composition efficiency"},
    "projectDetailsScreenRecompositions": {"class": "Recomposition", "metric": "frameDurationCpuMs", "target": "longFrames < 10%", "description": "Project details editing composition efficiency"},
    "projDetailsRecomp": {"class": "Recomposition", "metric": "frameDurationCpuMs", "target": "longFrames < 10%", "description": "Project details editing composition efficiency"},
    "singleProjectScreenRecompositions": {"class": "Recomposition", "metric": "frameDurationCpuMs", "target": "longFrames < 10%", "description": "Single project editing composition efficiency"},
    "singleProjRecomp": {"class": "Recomposition", "metric": "frameDurationCpuMs", "target": "longFrames < 10%", "description": "Single project editing composition efficiency"},
}

BENCHMARK_METADATA = LEGACY_AND_CURRENT_BENCHMARKS


def iter_benchmark_records(root: Path) -> Iterable[dict[str, Any]]:
    for json_file in root.rglob("*.json"):
        try:
            payload = json.loads(json_file.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue

        if isinstance(payload, dict) and isinstance(payload.get("benchmarks"), list):
            for benchmark in payload["benchmarks"]:
                if isinstance(benchmark, dict):
                    benchmark["_source"] = str(json_file)
                    yield benchmark


def as_float(value: Any) -> float | None:
    if isinstance(value, (int, float)):
        return float(value)
    return None


def find_percentiles(metric: Any) -> dict[str, float]:
    result: dict[str, float] = {}
    if isinstance(metric, dict):
        for key in PERCENTILE_KEYS:
            parsed = as_float(metric.get(key))
            if parsed is not None:
                result[key] = parsed
    return result


def find_runs(metric: Any) -> list[float]:
    if isinstance(metric, dict) and isinstance(metric.get("runs"), list):
        values = metric["runs"]
        # Handle nested lists (sampledMetrics case where runs is [[...], [...]])
        flat_values = []
        for v in values:
            if isinstance(v, list):
                flat_values.extend([as_float(item) for item in v])
            else:
                flat_values.append(as_float(v))
        return [v for v in flat_values if v is not None]
    if isinstance(metric, list):
        values = [as_float(v) for v in metric]
        return [v for v in values if v is not None]
    return []


def iter_known_metrics(record: dict[str, Any]) -> Iterable[tuple[str, Any]]:
    result = []
    metrics = record.get("metrics")
    if isinstance(metrics, dict):
        for metric_name in KNOWN_METRICS:
            value = metrics.get(metric_name)
            if value is not None:
                result.append((metric_name, value))

    # Also check sampledMetrics for metrics not found in metrics
    sampled_metrics = record.get("sampledMetrics")
    if isinstance(sampled_metrics, dict):
        for metric_name in KNOWN_METRICS:
            # Only add if not already found in metrics
            if not any(m[0] == metric_name for m in result):
                value = sampled_metrics.get(metric_name)
                if value is not None:
                    result.append((metric_name, value))

    return result


def derive_metric(metric_name: str, runs: list[float]) -> tuple[str, float | None]:
    if metric_name == "timeToInitialDisplayMs" and runs:
        avg = sum(runs) / len(runs)
        return f"avg={avg:.1f}ms", avg
    if metric_name == "startupMs" and runs:
        avg = sum(runs) / len(runs)
        return f"avg={avg:.1f}ms", avg
    if metric_name == "frameOverrunMs" and runs:
        jank = (sum(1 for v in runs if v > 0.0) / len(runs)) * 100.0
        missed = sum(1 for v in runs if v > 0.0)
        return f"jank%={jank:.2f};missedFrames={missed}", jank
    if metric_name == "frameDurationCpuMs" and runs:
        long_frames = sum(1 for v in runs if v > 16.0)
        pct = (long_frames / len(runs)) * 100.0
        return f"longFrames>16ms%={pct:.2f};count={long_frames}", pct
    if metric_name == "recompositionCount" and runs:
        avg = sum(runs) / len(runs) if runs else 0.0
        max_val = max(runs) if runs else 0.0
        return f"avg={avg:.1f};max={max_val:.0f}", max_val
    return "", None


def supports_unicode_output() -> bool:
    encoding = (sys.stdout.encoding or "").lower()
    if not encoding:
        return False
    try:
        "✓⚠❌".encode(encoding)
        return True
    except UnicodeEncodeError:
        return False


def status_symbols() -> dict[str, str]:
    if supports_unicode_output():
        return {"ok": "✓", "warn": "⚠", "fail": "❌", "unknown": " "}
    return {"ok": "OK", "warn": "!", "fail": "X", "unknown": " "}


STATUS_SYMBOLS = status_symbols()


def get_status_indicator(metric_name: str, value: float) -> str:
    """Return a status indicator based on metric value and thresholds."""
    if metric_name == "frameOverrunMs":  # jank percentage
        if value < 5.0:
            return STATUS_SYMBOLS["ok"]
        elif value < 10.0:
            return STATUS_SYMBOLS["warn"]
        else:
            return STATUS_SYMBOLS["fail"]
    elif metric_name == "frameDurationCpuMs":  # long frames percentage
        if value < 10.0:
            return STATUS_SYMBOLS["ok"]
        elif value < 20.0:
            return STATUS_SYMBOLS["warn"]
        else:
            return STATUS_SYMBOLS["fail"]
    elif metric_name == "timeToInitialDisplayMs":  # startup milliseconds
        if value < 300.0:
            return STATUS_SYMBOLS["ok"]
        elif value < 500.0:
            return STATUS_SYMBOLS["warn"]
        else:
            return STATUS_SYMBOLS["fail"]
    return STATUS_SYMBOLS["unknown"]


def print_summary(records: list[dict[str, Any]]) -> dict[str, list[tuple[str, float]]]:
    if not records:
        print("No benchmark JSON records found.")
        return {"jank": [], "long_frames": [], "recompositions": []}

    print("=" * 80)
    print("BENCHMARK RESULTS SUMMARY")
    print("=" * 80)
    print()

    # Organize results by benchmark class
    results_by_class: dict[str, list[dict[str, Any]]] = {
        "Startup": [],
        "Scroll": [],
        "Recomposition": [],
    }

    derived_values: dict[str, list[tuple[str, float]]] = {"jank": [], "long_frames": [], "recompositions": []}

    # Collect and group results
    for record in records:
        name = str(record.get("name", "<unknown>"))
        metadata = BENCHMARK_METADATA.get(name, {"class": "Unknown", "description": ""})
        benchmark_class = metadata.get("class", "Unknown")

        if benchmark_class in results_by_class:
            record["_class"] = benchmark_class
            record["_metadata"] = metadata
            results_by_class[benchmark_class].append(record)

            # Extract metrics
            for metric_name, metric_value in iter_known_metrics(record):
                if metric_value is None:
                    continue

                runs = find_runs(metric_value)
                derived, derived_percent = derive_metric(metric_name=metric_name, runs=runs)
                store_derived_value(
                    derived_values=derived_values,
                    benchmark_name=name,
                    metric_name=metric_name,
                    value=derived_percent,
                )

    # Print organized by class
    print("benchmark,metric,p50,p90,p95,max,derived,description")
    for benchmark_class in ["Startup", "Scroll", "Recomposition"]:
        for record in results_by_class[benchmark_class]:
            name = str(record.get("name", "<unknown>"))
            metadata = record.get("_metadata", {})
            description = metadata.get("description", "")

            for metric_name, metric_value in iter_known_metrics(record):
                if metric_value is None:
                    continue

                p = find_percentiles(metric_value)
                runs = find_runs(metric_value)
                derived, derived_percent = derive_metric(metric_name=metric_name, runs=runs)

                print(
                    ",".join(
                        [
                            name,
                            metric_name,
                            str(p.get("P50") or p.get("p50") or p.get("median") or ""),
                            str(p.get("P90") or p.get("p90") or ""),
                            str(p.get("P95") or p.get("p95") or ""),
                            str(p.get("max") or ""),
                            derived,
                            description,
                        ]
                    )
                )

    # Print summary by class with status
    print()
    print("=" * 80)
    print("SUMMARY BY CLASS")
    print("=" * 80)
    for benchmark_class in ["Startup", "Scroll", "Recomposition"]:
        print(f"\n{benchmark_class.upper()}")
        if results_by_class[benchmark_class]:
            for record in results_by_class[benchmark_class]:
                name = str(record.get("name", "<unknown>"))
                metadata = record.get("_metadata", {})
                target = metadata.get("target", "N/A")
                
                # Extract the most relevant value for status
                status = " "
                for metric_name, metric_value in iter_known_metrics(record):
                    if metric_value is None:
                        continue
                    runs = find_runs(metric_value)
                    _, derived_percent = derive_metric(metric_name=metric_name, runs=runs)
                    if derived_percent is not None:
                        status = get_status_indicator(metric_name, derived_percent)
                        break
                
                print(f"  {status} {name:40} Target: {target}")
        else:
            print(f"  (no tests)")

    return derived_values


def store_derived_value(
    derived_values: dict[str, list[tuple[str, float]]],
    benchmark_name: str,
    metric_name: str,
    value: float | None,
) -> None:
    if value is None:
        return
    if metric_name == "frameOverrunMs":
        derived_values["jank"].append((benchmark_name, value))
    elif metric_name == "frameDurationCpuMs":
        derived_values["long_frames"].append((benchmark_name, value))
    elif metric_name == "recompositionCount":
        derived_values["recompositions"].append((benchmark_name, value))


def evaluate_single_threshold(
    values: list[tuple[str, float]],
    limit: float | None,
    metric_label: str,
    missing_message: str,
    fail_on_missing_runs: bool,
) -> list[str]:
    failures: list[str] = []
    if limit is None:
        return failures
    if not values and fail_on_missing_runs:
        failures.append(missing_message)
        return failures
    for benchmark_name, value in values:
        if value > limit:
            failures.append(f"{benchmark_name}: {metric_label} {value:.2f} exceeded max {limit:.2f}")
    return failures


def evaluate_thresholds(
    derived_values: dict[str, list[tuple[str, float]]],
    max_jank_percent: float | None,
    max_long_frames_percent: float | None,
    max_recompositions: float | None,
    fail_on_missing_runs: bool,
) -> int:
    failures = evaluate_single_threshold(
        values=derived_values.get("jank", []),
        limit=max_jank_percent,
        metric_label="jank%",
        missing_message="No frameOverrunMs runs found while --fail-on-missing-runs is enabled.",
        fail_on_missing_runs=fail_on_missing_runs,
    )
    failures.extend(
        evaluate_single_threshold(
            values=derived_values.get("long_frames", []),
            limit=max_long_frames_percent,
            metric_label="longFrames>16ms%",
            missing_message="No frameDurationCpuMs runs found while --fail-on-missing-runs is enabled.",
            fail_on_missing_runs=fail_on_missing_runs,
        )
    )
    failures.extend(
        evaluate_single_threshold(
            values=derived_values.get("recompositions", []),
            limit=max_recompositions,
            metric_label="recompositions",
            missing_message="No recompositionCount runs found while --fail-on-missing-runs is enabled.",
            fail_on_missing_runs=fail_on_missing_runs,
        )
    )

    if failures:
        print("\nTHRESHOLD CHECK: FAILED")
        for failure in failures:
            print(f"- {failure}")
        return 1

    if max_jank_percent is not None or max_long_frames_percent is not None or max_recompositions is not None:
        print("\nTHRESHOLD CHECK: PASSED")
    return 0


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "path",
        nargs="?",
        default="macrobenchmark/build",
        help="Directory to scan recursively for benchmark JSON output.",
    )
    parser.add_argument(
        "--max-jank-percent",
        type=float,
        default=None,
        help="Fail if any benchmark jank percentage exceeds this value.",
    )
    parser.add_argument(
        "--max-long-frames-percent",
        type=float,
        default=None,
        help="Fail if any benchmark long-frames (>16ms) percentage exceeds this value.",
    )
    parser.add_argument(
        "--max-recompositions",
        type=float,
        default=None,
        help="Fail if any benchmark recomposition count exceeds this value.",
    )
    parser.add_argument(
        "--fail-on-missing-runs",
        action="store_true",
        help="Fail when threshold checks are requested but run arrays are missing.",
    )
    args = parser.parse_args()

    root = Path(args.path)
    if not root.exists():
        print(f"Path not found: {root}")
        sys.exit(1)

    records = list(iter_benchmark_records(root))
    derived_values = print_summary(records)
    exit_code = evaluate_thresholds(
        derived_values=derived_values,
        max_jank_percent=args.max_jank_percent,
        max_long_frames_percent=args.max_long_frames_percent,
        max_recompositions=args.max_recompositions,
        fail_on_missing_runs=args.fail_on_missing_runs,
    )
    sys.exit(exit_code)


if __name__ == "__main__":
    main()
