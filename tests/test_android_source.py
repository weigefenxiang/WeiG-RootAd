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

    def test_manager_targets_separate_zeroad_rules_repository(self) -> None:
        gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
        self.assertIn('"WeiG-ZeroAd"', gradle)
        self.assertIn('"WeiG-ZeroAd-Rules"', gradle)

    def test_rule_updater_accepts_six_profiles(self) -> None:
        source = (ROOT / "app/src/main/java/com/weig/rootad/RuleUpdater.java").read_text(
            encoding="utf-8"
        )
        for profile in (
            "cn-lean.domains", "cn-balanced.domains", "cn-strict.domains",
            "global-lean.domains", "global-balanced.domains", "global-strict.domains",
        ):
            self.assertIn(profile, source)


if __name__ == "__main__":
    unittest.main()
