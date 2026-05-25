import json
from pathlib import Path

# Read the latest benchmark JSON
json_path = Path(r"C:\AndroidDev\AWTimesheet\macrobenchmark\build\outputs\connected_android_test_additional_output\benchmark\connected\SM-G781B - 13\com.akiwiksten.awtimesheet.macrobenchmark-benchmarkData.json")

with open(json_path) as f:
    data = json.load(f)

startup_bench = data['benchmarks'][0]
print("=" * 80)
print("BENCHMARK RESULTS AFTER FIX v2")
print("=" * 80)
print()

# Time to Initial Display
ttid = startup_bench['metrics']['timeToInitialDisplayMs']
print(f"timeToInitialDisplayMs (first draw):")
print(f"  Min: {ttid['minimum']:.1f}ms")
print(f"  Median (p50): {ttid['median']:.1f}ms")
print(f"  Max: {ttid['maximum']:.1f}ms")
print(f"  Target: < 500ms")
print()

# Frame Count
fc = startup_bench['metrics']['frameCount']
print(f"frameCount (total frames during startup):")
print(f"  Min: {int(fc['minimum'])} frames")
print(f"  Median: {int(fc['median'])} frames")
print(f"  Max: {int(fc['maximum'])} frames")
print(f"  All runs: {[int(x) for x in fc['runs']]}")
print()

# Frame Duration
fdc = startup_bench['sampledMetrics']['frameDurationCpuMs']
print(f"frameDurationCpuMs:")
print(f"  P50: {fdc['P50']:.2f}ms")
print(f"  P90: {fdc['P90']:.2f}ms")
print(f"  P95: {fdc['P95']:.2f}ms")
print()

# Frame Overrun
fo = startup_bench['sampledMetrics']['frameOverrunMs']
print(f"frameOverrunMs:")
print(f"  P50: {fo['P50']:.2f}ms")
print(f"  P90: {fo['P90']:.2f}ms")
print(f"  P95: {fo['P95']:.2f}ms")
print()

# Calculate simple stats from raw data
print("=" * 80)
print("COMPARISON: BEFORE vs AFTER FIX v2")
print("=" * 80)
print()

print("timeToInitialDisplayMs (first draw speed):")
print(f"  BEFORE:  378.3 ms ✓")
print(f"  AFTER:   {ttid['median']:.1f} ms {'⚠️ SLOWER' if ttid['median'] > 380 else '✓'}")
print(f"  Change:  {ttid['median'] - 378.3:+.1f} ms")
print()

print("Frame Count (total frames rendered during startup)")
print(f"  BEFORE:  ~26-39 frames ❌ (jank!)")
print(f"  AFTER:   {int(fc['median'])} frames ✅ (much fewer!)")
print(f"  Improvement: {((26-int(fc['median']))/26)*100:.0f}% fewer frames")
print()

print("Frame Duration P50 (typical frame time):")
print(f"  BEFORE:  60.6 ms ❌")
print(f"  AFTER:   {fdc['P50']:.2f} ms ✅")
print()

print("=" * 80)
print("ANALYSIS")
print("=" * 80)

if ttid['median'] > 400:
    print("⚠️  First draw is slower than ideal (>400ms)")
    print("   Possible cause: 100ms delay in LaunchedEffect")
    print("   Recommendation: Try 50ms or 33ms delay instead")
else:
    print("✅ First draw is excellent (<400ms)")

if int(fc['median']) <= 3:
    print("✅ Very few frames rendered during startup (2-3 vs 26-39 before)")
    print("   This is a MASSIVE improvement in frame efficiency")
else:
    print(f"⚠️  More frames than expected: {int(fc['median'])}")

if fdc['P50'] < 20:
    print("✅ Typical frame time is under 20ms (60fps capable)")
else:
    print(f"⚠️  Some frames taking {fdc['P50']:.1f}ms (>16.67ms 60fps target)")

print()

