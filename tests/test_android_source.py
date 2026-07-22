from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]


class AndroidStartupSourceTests(unittest.TestCase):
    def test_content_view_exists_before_window_insets_controller(self) -> None:
        source = (ROOT / "app/src/main/java/com/weig/rootad/MainActivity.java").read_text(
            encoding="utf-8"
        )
        content_view = source.index("setContentView(buildScreen())")
        insets_controller = source.index("getWindow().getInsetsController()")
        self.assertLess(content_view, insets_controller)


if __name__ == "__main__":
    unittest.main()
