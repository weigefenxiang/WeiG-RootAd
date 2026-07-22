from __future__ import annotations

import json
import datetime as dt
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

from rule_tools.build_module import build, file_sha256
from rule_tools.build_rule_release import FILES, build as build_rule_release
from rule_tools.rules import compile_profiles, domains_from_line, normalize_domain
from rule_tools.sync_upstreams import sync


ROOT = Path(__file__).resolve().parents[1]


class RuleParsingTests(unittest.TestCase):
    def test_normalizes_exact_domains(self) -> None:
        self.assertEqual(normalize_domain("SDK.E.QQ.COM."), "sdk.e.qq.com")
        self.assertIsNone(normalize_domain("*.example.com"))
        self.assertIsNone(normalize_domain("120.232.202.5"))
        self.assertIsNone(normalize_domain("not a domain"))

    def test_reads_supported_formats(self) -> None:
        self.assertEqual(domains_from_line("0.0.0.0 ads.example.com"), {"ads.example.com"})
        self.assertEqual(domains_from_line("||ads.example.com^"), {"ads.example.com"})
        self.assertEqual(domains_from_line("@@||safe.example.com^$important"), {"safe.example.com"})
        self.assertEqual(domains_from_line("# comment"), set())


class GeneratedRuleTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        version = int((ROOT / "rules/version.txt").read_text(encoding="utf-8").strip())
        cls.profiles = compile_profiles(ROOT, version)

    def test_profiles_are_monotonic(self) -> None:
        self.assertTrue(self.profiles["balanced"] <= self.profiles["strict"])
        self.assertFalse(self.profiles["reward"] & self.profiles["strict"])
        self.assertFalse(self.profiles["reward"] & self.profiles["balanced"])

    def test_source_is_nontrivial(self) -> None:
        self.assertEqual(len(self.profiles["strict"]), 17_041)
        self.assertEqual(len(self.profiles["reward"]), 74)

    def test_reward_packs_are_disjoint_and_complete(self) -> None:
        manifest = json.loads((ROOT / "rules/generated/manifest.json").read_text(encoding="utf-8"))
        combined: set[str] = set()
        for pack in manifest["packs"]:
            domains = set()
            for line in (ROOT / "rules/generated" / pack["file"]).read_text(encoding="utf-8").splitlines():
                domains.update(domains_from_line(line))
            self.assertFalse(combined & domains)
            combined.update(domains)
        self.assertEqual(combined, self.profiles["reward"])

    def test_manifest_exposes_running_rule_count_contract(self) -> None:
        manifest = json.loads((ROOT / "rules/generated/manifest.json").read_text(encoding="utf-8"))
        self.assertIn("running_rules", manifest["status_fields"])
        self.assertIn("pending_reboot", manifest["status_fields"])
        self.assertIn("reward_temporarily_allowed", manifest["status_fields"])
        self.assertTrue(manifest["manual_rule_control"])


class ModuleBuildTests(unittest.TestCase):
    def test_module_zip_is_reproducible_and_executable(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            first = Path(temp_dir) / "first.zip"
            second = Path(temp_dir) / "second.zip"
            build(ROOT, first)
            build(ROOT, second)
            self.assertEqual(file_sha256(first), file_sha256(second))

            with zipfile.ZipFile(first) as archive:
                names = archive.namelist()
                self.assertTrue(all(not name.startswith("/") and ".." not in Path(name).parts for name in names))
                self.assertNotIn("manager/WeiG-RootAd-Manager.apk", names)
                self.assertEqual((archive.getinfo("bin/rulectl").external_attr >> 16) & 0o777, 0o755)
                self.assertEqual((archive.getinfo("module.prop").external_attr >> 16) & 0o777, 0o644)

    def test_all_in_one_zip_embeds_manager_without_changing_core_layout(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            manager = Path(temp_dir) / "manager.apk"
            manager.write_bytes(b"signed-apk-placeholder")
            output = Path(temp_dir) / "all-in-one.zip"
            build(ROOT, output, manager)

            with zipfile.ZipFile(output) as archive:
                self.assertEqual(
                    archive.read("manager/WeiG-RootAd-Manager.apk"),
                    b"signed-apk-placeholder",
                )
                self.assertIn("module.prop", archive.namelist())
                self.assertIn("system/etc/hosts", archive.namelist())


class RuleReleaseTests(unittest.TestCase):
    def test_data_release_contains_only_allowed_files(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            output = Path(temp_dir) / "rules.zip"
            build_rule_release(ROOT, output)
            with zipfile.ZipFile(output) as archive:
                self.assertEqual(set(archive.namelist()), set(FILES))

    def test_weekly_sync_exactly_deduplicates(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            (root / "rules").mkdir()
            (root / "rules/sources.json").write_text(json.dumps({
                "schema": 1,
                "sources": [{"id": "test", "url": "https://example.invalid/rules", "license": "test"}],
            }), encoding="utf-8")
            lines = [f"0.0.0.0 ad{i}.example.com" for i in range(1_200)]
            lines.extend(["0.0.0.0 ad1.example.com", "not a domain", "# comment"])
            with patch("rule_tools.sync_upstreams.fetch", return_value="\n".join(lines)):
                report = sync(root, dt.date(2026, 7, 21))
            self.assertEqual(report["unique_domains"], 1_200)
            self.assertEqual(report["duplicates_removed"], 1)
            self.assertEqual(report["version"], 2026072101)
            saved = (root / "rules/vendor/wei.G/260723.txt").read_text(encoding="utf-8")
            self.assertEqual(saved.count("ad1.example.com\n"), 1)


if __name__ == "__main__":
    unittest.main()
