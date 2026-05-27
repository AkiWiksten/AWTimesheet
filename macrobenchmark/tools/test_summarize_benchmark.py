from __future__ import annotations

import io
import unittest
from contextlib import redirect_stdout
from pathlib import Path
import sys

TOOLS_DIR = Path(__file__).resolve().parent
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

import summarize_benchmark as sb


class SummarizeBenchmarkTest(unittest.TestCase):
    def test_recomposition_benchmark_summary_includes_both_metrics(self) -> None:
        record = {
            "name": "calRecomp",
            "metrics": {},
            "sampledMetrics": {
                "frameDurationCpuMs": {
                    "runs": [[1.0, 2.0, 3.0, 17.5]],
                    "P50": 2.0,
                    "P90": 17.5,
                    "P95": 17.5,
                    "max": 17.5,
                },
                "recompositionCount": {
                    "runs": [100, 120, 90],
                    "P50": 100,
                    "P90": 120,
                    "max": 120,
                },
            },
        }

        status, details = sb.build_summary_details(record, "Recomposition")

        self.assertEqual("❌", status)
        self.assertIn("frameDurationCpuMs: longFrames>16ms%=25.00;count=1", details)
        self.assertIn("recompositionCount: avg=103.3;max=120", details)

    def test_print_summary_shows_recomposition_section(self) -> None:
        records = [
            {
                "name": "calRecomp",
                "metrics": {},
                "sampledMetrics": {
                    "frameDurationCpuMs": {
                        "runs": [[1.0, 2.0, 3.0, 17.5]],
                        "P50": 2.0,
                        "P90": 17.5,
                        "P95": 17.5,
                        "max": 17.5,
                    },
                    "recompositionCount": {
                        "runs": [100, 120, 90],
                        "P50": 100,
                        "P90": 120,
                        "max": 120,
                    },
                },
            }
        ]

        buffer = io.StringIO()
        with redirect_stdout(buffer):
            derived_values = sb.print_summary(records)

        output = buffer.getvalue()
        self.assertIn("RECOMPOSITION", output)
        self.assertIn("calRecomp", output)
        self.assertIn("recompositionCount: avg=103.3;max=120", output)
        self.assertIn("frameDurationCpuMs: longFrames>16ms%=25.00;count=1", output)
        self.assertEqual([("calRecomp", 25.0)], derived_values["long_frames"])
        self.assertEqual([("calRecomp", 120.0)], derived_values["recompositions"])


if __name__ == "__main__":
    unittest.main()

