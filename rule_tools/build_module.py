from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import tempfile
import zipfile
from pathlib import Path


TEXT_SUFFIXES = {".sh", ".prop", ".txt", ".hosts", ".domains", ".json"}
EXECUTABLE_NAMES = {"action.sh", "customize.sh", "service.sh", "uninstall.sh", "update-binary", "rulectl"}
ZIP_TIMESTAMP = (2026, 1, 1, 0, 0, 0)


def file_sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def normalize_text_files(root: Path) -> None:
    for path in root.rglob("*"):
        if path.is_file() and (path.suffix in TEXT_SUFFIXES or path.name == "update-binary"):
            data = path.read_bytes().replace(b"\r\n", b"\n")
            path.write_bytes(data)


def build(root: Path, output: Path, manager_apk: Path | None = None) -> None:
    module = root / "module"
    generated = root / "rules/generated"
    required = [generated / name for name in (
        "strict.domains", "balanced.domains", "reward.domains", "manifest.json", "packs.json",
        "reward-tencent.domains", "reward-wechat.domains", "reward-short-video.domains",
        "reward-other.domains",
    )]
    missing = [str(path) for path in required if not path.exists()]
    if missing:
        raise SystemExit(f"Run build_rules first; missing: {', '.join(missing)}")

    with tempfile.TemporaryDirectory(prefix="weig-rootad-module-") as temp_dir:
        staging = Path(temp_dir) / "module"
        shutil.copytree(module, staging)
        (staging / "rules").mkdir(parents=True, exist_ok=True)
        for path in required:
            shutil.copy2(path, staging / "rules" / path.name)
        shutil.copy2(generated / "strict.hosts", staging / "system/etc/hosts")
        if manager_apk is not None:
            if not manager_apk.is_file():
                raise SystemExit(f"Manager APK not found: {manager_apk}")
            manager_dir = staging / "manager"
            manager_dir.mkdir(parents=True, exist_ok=True)
            shutil.copy2(manager_apk, manager_dir / "WeiG-RootAd-Manager.apk")
        normalize_text_files(staging)

        output.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
            for path in sorted(staging.rglob("*")):
                if path.is_file() and path.name != ".gitkeep":
                    relative = path.relative_to(staging).as_posix()
                    mode = 0o755 if path.name in EXECUTABLE_NAMES else 0o644
                    info = zipfile.ZipInfo(relative, ZIP_TIMESTAMP)
                    info.create_system = 3
                    info.external_attr = mode << 16
                    info.compress_type = zipfile.ZIP_DEFLATED
                    archive.writestr(info, path.read_bytes(), compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)

    checksums = output.parent / "SHA256SUMS"
    checksums.write_text(
        f"{file_sha256(output)}  {output.name}\n", encoding="utf-8", newline="\n"
    )


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Build a Wei.G RootAd core-only or manager-inclusive module ZIP"
    )
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument(
        "--output", type=Path, default=Path("dist/WeiG-RootAd-v0.1.0-core-only.zip")
    )
    parser.add_argument(
        "--manager-apk", type=Path,
        help="Embed this signed manager APK and let customize.sh install/update it",
    )
    args = parser.parse_args()
    root = args.root.resolve()
    output = args.output if args.output.is_absolute() else root / args.output
    manager_apk = args.manager_apk
    if manager_apk is not None and not manager_apk.is_absolute():
        manager_apk = root / manager_apk
    build(root, output, manager_apk)
    print(json.dumps({
        "module": str(output),
        "variant": "all-in-one" if manager_apk else "core-only",
        "sha256": file_sha256(output),
    }))


if __name__ == "__main__":
    main()
