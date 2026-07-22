from __future__ import annotations

import argparse
from pathlib import Path

from rule_tools.rules import compile_profiles


def main() -> None:
    parser = argparse.ArgumentParser(description="Build deterministic Wei.G RootAd rule profiles")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--version", type=int)
    args = parser.parse_args()

    root = args.root.resolve()
    version = args.version
    if version is None:
        version = int((root / "rules/version.txt").read_text(encoding="utf-8").strip())

    profiles = compile_profiles(root, version)
    for name, domains in profiles.items():
        print(f"{name}: {len(domains)} rules")


if __name__ == "__main__":
    main()
