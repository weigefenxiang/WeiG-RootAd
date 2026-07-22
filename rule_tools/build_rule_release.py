from __future__ import annotations

import argparse
import hashlib
import json
import zipfile
from pathlib import Path


FILES = (
    "manifest.json", "packs.json", "strict.domains", "balanced.domains", "reward.domains",
    "reward-tencent.domains", "reward-wechat.domains", "reward-short-video.domains",
    "reward-other.domains",
)
TIMESTAMP = (2026, 1, 1, 0, 0, 0)


def build(root: Path, output: Path) -> Path:
    generated = root / "rules/generated"
    missing = [name for name in FILES if not (generated / name).is_file()]
    if missing:
        raise SystemExit("Missing generated rule files: " + ", ".join(missing))
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name in FILES:
            info = zipfile.ZipInfo(name, TIMESTAMP)
            info.create_system = 3
            info.external_attr = 0o644 << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(info, (generated / name).read_bytes(), compress_type=zipfile.ZIP_DEFLATED,
                             compresslevel=9)
    digest = hashlib.sha256(output.read_bytes()).hexdigest()
    (output.parent / "RULES_SHA256SUMS").write_text(f"{digest}  {output.name}\n", encoding="utf-8")
    return output


def main() -> None:
    parser = argparse.ArgumentParser(description="Build a data-only Wei.G RootAd rules release")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--output", type=Path, default=Path("dist/weig-rootad-rules.zip"))
    args = parser.parse_args()
    root = args.root.resolve()
    output = args.output if args.output.is_absolute() else root / args.output
    print(json.dumps({"rules": str(build(root, output))}))


if __name__ == "__main__":
    main()
