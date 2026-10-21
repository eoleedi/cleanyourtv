"""Keep the troubleshooting requirement in sync with this local compatibility build."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]


class CompatibilityDocumentationTest(unittest.TestCase):
    def test_local_sdk_requirement_matches_build(self):
        build = (ROOT / "launcher/app/build.gradle").read_text()
        minimum = re.search(r"\bminSdk\s+(\d+)", build).group(1)
        target = re.search(r"\btargetSdk\s+(\d+)", build).group(1)
        guide = (ROOT / "docs/TROUBLESHOOTING.md").read_text()
        expected = f"**Local compatibility build:** minSdk {minimum}, targetSdk {target}."
        self.assertTrue(expected in guide, f"Troubleshooting must declare: {expected}")
