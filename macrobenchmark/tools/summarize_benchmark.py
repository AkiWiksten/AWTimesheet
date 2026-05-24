#!/usr/bin/env python3
"""Summarize AndroidX Benchmark JSON output for startup and frame timing metrics."""

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
)
PERCENTILE_KEYS = ("P50", "p50", "median", "P90", "p90", "P95", "p95", "max")


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
        values = [as_float(v) for v in metric["runs"]]
        return [v for v in values if v is not None]
    if isinstance(metric, list):
        values = [as_float(v) for v in metric]
        return [v for v in values if v is not None]
    return []


def iter_known_metrics(record: dict[str, Any]) -> Iterable[tuple[str, Any]]:
    metrics = record.get("metrics")
    if not isinstance(metrics, dict):
        return []
    return [(metric_name, metrics.get(metric_name)) for metric_name in KNOWN_METRICS]


def derive_metric(metric_name: str, runs: list[float]) -> tuple[str, float | None]:
    if metric_name == "frameOverrunMs" and runs:
        jank = (sum(1 for v in runs if v > 0.0) / len(runs)) * 100.0
        missed = sum(1 for v in runs if v > 0.0)
        return f"jank%={jank:.2f};missedFrames={missed}", jank
    if metric_name == "frameDurationCpuMs" and runs:
        long_frames = sum(1 for v in runs if v > 16.0)
        pct = (long_frames / len(runs)) * 100.0
        return f"longFrames>16ms%={pct:.2f};count={long_frames}", pct
    return "", None


def print_summary(records: list[dict[str, Any]]) -> dict[str, list[tuple[str, float]]]:
    if not records:
        print("No benchmark JSON records found.")
        return {"jank": [], "long_frames": []}

    print("benchmark,metric,p50,p90,p95,max,derived")
    derived_values: dict[str, list[tuple[str, float]]] = {"jank": [], "long_frames": []}

    for record in records:
        name = str(record.get("name", "<unknown>"))
        for metric_name, metric_value in iter_known_metrics(record):
            if metric_value is None:
                continue

            p = find_percentiles(metric_value)
            runs = find_runs(metric_value)

            derived, derived_percent = derive_metric(metric_name=metric_name, runs=runs)
            store_derived_value(
                derived_values=derived_values,
                benchmark_name=name,
                metric_name=metric_name,
                value=derived_percent,
            )

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
                    ]
                )
            )

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

    if failures:
        print("\nTHRESHOLD CHECK: FAILED")
        for failure in failures:
            print(f"- {failure}")
        return 1

    if max_jank_percent is not None or max_long_frames_percent is not None:
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
        fail_on_missing_runs=args.fail_on_missing_runs,
    )
    sys.exit(exit_code)


if __name__ == "__main__":
    main()



