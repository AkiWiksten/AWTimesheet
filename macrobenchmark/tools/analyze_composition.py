#!/usr/bin/env python3
"""Analyze composition efficiency from macrobenchmark results.

This script provides detailed analysis of recomposition patterns, frame timing,
and composition efficiency metrics to identify optimization opportunities.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any
from dataclasses import dataclass
from collections import defaultdict


@dataclass
class CompositionAnalysis:
    """Analysis of composition metrics for a benchmark."""
    benchmark_name: str
    long_frame_percent: float
    jank_percent: float
    estimated_pressure: str  # HIGH, MEDIUM, LOW
    recommendations: list[str]

    def __str__(self) -> str:
        return f"{self.benchmark_name}: {self.estimated_pressure} pressure\n" \
               f"  Long frames >16ms: {self.long_frame_percent:.1f}%\n" \
               f"  Jank: {self.jank_percent:.1f}%\n" \
               f"  Recommendations:\n" + \
               "\n".join(f"    - {r}" for r in self.recommendations)


def iter_benchmark_records(root: Path) -> list[dict[str, Any]]:
    """Load all benchmark records from JSON files."""
    records = []
    for json_file in root.rglob("*.json"):
        try:
            payload = json.loads(json_file.read_text(encoding="utf-8"))
            if isinstance(payload, dict) and isinstance(payload.get("benchmarks"), list):
                for benchmark in payload["benchmarks"]:
                    if isinstance(benchmark, dict):
                        benchmark["_source"] = str(json_file)
                        records.append(benchmark)
        except (OSError, json.JSONDecodeError):
            continue
    return records


def analyze_composition(record: dict[str, Any]) -> CompositionAnalysis | None:
    """Analyze a single benchmark's composition efficiency."""
    benchmark_name = str(record.get("name", "<unknown>"))
    metrics = record.get("metrics", {})

    if not isinstance(metrics, dict):
        return None

    # Extract frame metrics
    long_frame_data = metrics.get("frameDurationCpuMs")
    jank_data = metrics.get("frameOverrunMs")

    long_frame_percent = 0.0
    jank_percent = 0.0

    if isinstance(long_frame_data, dict):
        runs = long_frame_data.get("runs", [])
        if runs:
            long_frame_percent = (sum(1 for v in runs if v > 16.0) / len(runs)) * 100.0

    if isinstance(jank_data, dict):
        runs = jank_data.get("runs", [])
        if runs:
            jank_percent = (sum(1 for v in runs if v > 0.0) / len(runs)) * 100.0

    # Determine pressure level
    if long_frame_percent > 30.0 or jank_percent > 20.0:
        pressure = "HIGH"
    elif long_frame_percent > 15.0 or jank_percent > 10.0:
        pressure = "MEDIUM"
    else:
        pressure = "LOW"

    # Generate recommendations
    recommendations = generate_recommendations(
        benchmark_name=benchmark_name,
        long_frame_percent=long_frame_percent,
        jank_percent=jank_percent,
        pressure=pressure,
    )

    return CompositionAnalysis(
        benchmark_name=benchmark_name,
        long_frame_percent=long_frame_percent,
        jank_percent=jank_percent,
        estimated_pressure=pressure,
        recommendations=recommendations,
    )


def generate_recommendations(
    benchmark_name: str,
    long_frame_percent: float,
    jank_percent: float,
    pressure: str,
) -> list[str]:
    """Generate optimization recommendations based on composition metrics."""
    recommendations = []

    if pressure == "HIGH":
        recommendations.append("Profile with Logcat recomposition logging to identify hot recomposables")
        recommendations.append("Use derivedStateOf() for expensive computations")
        recommendations.append("Consider memoization with remember() for expensive operations")

    if long_frame_percent > 20.0:
        recommendations.append("Frames exceed 16ms threshold - optimize heavy composition/drawing")
        recommendations.append("Check for LazyColumn/LazyRow performance - use fixed item keys")
        recommendations.append("Profile with Frame Profiler to identify bottlenecks")

    if jank_percent > 15.0:
        recommendations.append("High jank detected - review state management patterns")
        recommendations.append("Consider moving heavy computation off main thread")
        recommendations.append("Use produceState() or rememberCoroutineScope() for async work")

    if "Scroll" in benchmark_name or "scroll" in benchmark_name:
        recommendations.append("For scrolling: ensure LazyColumn items have stable keys")
        recommendations.append("Avoid recomposition during scroll - use skipToLookaheadSize")
        recommendations.append("Profile scrolling performance with FrameMetrics")

    if not recommendations:
        recommendations.append("Performance is acceptable - continue monitoring")

    return recommendations


def print_analysis(analyses: list[CompositionAnalysis]) -> None:
    """Print comprehensive composition analysis."""
    print("=" * 70)
    print("COMPOSITION EFFICIENCY ANALYSIS")
    print("=" * 70)
    print()

    if not analyses:
        print("No benchmarks analyzed.")
        return

    # Group by pressure level
    by_pressure: dict[str, list[CompositionAnalysis]] = defaultdict(list)
    for analysis in analyses:
        by_pressure[analysis.estimated_pressure].append(analysis)

    # Print HIGH pressure first
    for pressure in ["HIGH", "MEDIUM", "LOW"]:
        if pressure not in by_pressure:
            continue

        print(f"\n{pressure} PRESSURE BENCHMARKS ({len(by_pressure[pressure])})")
        print("-" * 70)
        for analysis in by_pressure[pressure]:
            print(analysis)
            print()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "path",
        nargs="?",
        default="macrobenchmark/build",
        help="Directory to scan for benchmark results.",
    )
    args = parser.parse_args()

    root = Path(args.path)
    if not root.exists():
        print(f"Path not found: {root}", file=sys.stderr)
        sys.exit(1)

    records = iter_benchmark_records(root)
    analyses = [
        analysis for analysis in
        [analyze_composition(r) for r in records]
        if analysis is not None
    ]

    print_analysis(analyses)

    # Exit with error code if any HIGH pressure benchmarks
    high_pressure = any(a.estimated_pressure == "HIGH" for a in analyses)
    sys.exit(1 if high_pressure else 0)


if __name__ == "__main__":
    main()

