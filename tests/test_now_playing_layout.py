"""The media panel must reserve its own row, never overlay the app grid."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / "launcher/app/src/main/res"
ANDROID = "{http://schemas.android.com/apk/res/android}"


class NowPlayingLayoutTest(unittest.TestCase):
    def test_panel_reserves_a_row_before_grid_in_both_layouts(self):
        for variant in ("layout", "layout-iw"):
            with self.subTest(variant=variant):
                root = ET.parse(ROOT / variant / "activity_main.xml").getroot()
                parents = {child: parent for parent in root.iter() for child in parent}
                panel = next(node for node in root.iter()
                             if node.get(ANDROID + "id") == "@+id/nowPlayingPanel")
                grid = next(node for node in root.iter()
                            if node.get(ANDROID + "id") == "@+id/grid")
                container = parents[panel]
                self.assertEqual("LinearLayout", container.tag, "Panel must not overlay grid in FrameLayout")
                self.assertEqual("vertical", container.get(ANDROID + "orientation"))
                self.assertIs(container, parents[parents[grid]])
                self.assertLess(list(container).index(panel), list(container).index(parents[grid]))
                self.assertEqual("match_parent", panel.get(ANDROID + "layout_width"),
                                 "Panel must adapt to narrow displays")
