from __future__ import annotations

import argparse
import datetime as dt
import json
import urllib.request
from collections import defaultdict
from pathlib import Path

from rule_tools.rules import domains_from_line, write_domains


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": "WeiG-RootAd-Rules/0.1.0"})
    with urllib.request.urlopen(request, timeout=45) as response:
        if response.status != 200:
            raise RuntimeError(f"{url} returned HTTP {response.status}")
        data = response.read(64 * 1024 * 1024 + 1)
        if len(data) > 64 * 1024 * 1024:
            raise RuntimeError(f"{url} exceeds the 64 MiB source limit")
        return data.decode("utf-8-sig", errors="replace")


def sync(root: Path, date: dt.date, sequence: int | None = None) -> dict[str, object]:
    config = json.loads((root / "rules/sources.json").read_text(encoding="utf-8"))
    combined: set[str] = set()
    provenance: dict[str, set[str]] = defaultdict(set)
    source_stats: list[dict[str, object]] = []
    raw_entries = 0
    invalid_lines = 0

    for source in config["sources"]:
        if not source.get("enabled", True):
            continue
        source_type = source.get("type", "remote")
        if source_type == "local":
            relative = Path(source["path"])
            if relative.is_absolute() or ".." in relative.parts:
                raise RuntimeError(f"Unsafe local source path: {relative}")
            source_path = root / relative
            text = source_path.read_text(encoding="utf-8-sig")
            location = relative.as_posix()
        elif source_type == "remote":
            text = fetch(source["url"])
            location = source["url"]
        else:
            raise RuntimeError(f"Unsupported source type: {source_type}")
        source_domains: set[str] = set()
        source_raw = 0
        for line in text.splitlines():
            parsed = domains_from_line(line)
            if parsed:
                source_raw += len(parsed)
                source_domains.update(parsed)
            elif line.strip() and not line.lstrip().startswith(("#", "!")):
                invalid_lines += 1
        raw_entries += source_raw
        combined.update(source_domains)
        for domain in source_domains:
            provenance[domain].add(source["id"])
        source_stats.append({
            "id": source["id"], "location": location, "license": source.get("license", "unknown"),
            "parsed_entries": source_raw, "unique_domains": len(source_domains),
        })

    if len(combined) < 1000:
        raise RuntimeError(f"Refusing suspicious upstream result with only {len(combined)} domains")

    target = root / "rules/vendor/wei.G/260723.txt"
    previous = set()
    if target.exists():
        for line in target.read_text(encoding="utf-8-sig").splitlines():
            previous.update(domains_from_line(line))
    change = len(combined) - len(previous)
    change_ratio = abs(change) / max(len(previous), 1)
    if previous and change_ratio > 0.35:
        raise RuntimeError(f"Refusing {change_ratio:.1%} upstream size change without review")

    if sequence is None:
        sequence = 1
        version_file = root / "rules/version.txt"
        if version_file.exists():
            current = version_file.read_text(encoding="utf-8").strip()
            prefix = f"{date:%Y%m%d}"
            if len(current) == 10 and current.startswith(prefix) and current.isdigit():
                sequence = int(current[-2:]) + 1
    if sequence < 1 or sequence > 99:
        raise RuntimeError("Rule release sequence must be between 1 and 99")
    version = int(f"{date:%Y%m%d}{sequence:02d}")
    write_domains(target, combined, header=(
        "Generated from configured upstreams by sync_upstreams.py.",
        f"rule_version={version}",
        "Exact-domain deduplication only; parent domains do not cover subdomains in hosts files.",
    ))
    (root / "rules/version.txt").write_text(f"{version}\n", encoding="utf-8", newline="\n")

    report = {
        "schema": 1, "version": version, "generated_at": f"{date.isoformat()}T03:00:00+08:00",
        "raw_entries": raw_entries, "unique_domains": len(combined),
        "duplicates_removed": max(raw_entries - len(combined), 0), "invalid_lines_ignored": invalid_lines,
        "previous_domains": len(previous), "added_net": max(change, 0), "removed_net": max(-change, 0),
        "sources": source_stats,
    }
    reports = root / "rules/reports"
    reports.mkdir(parents=True, exist_ok=True)
    (reports / f"{version}.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n",
                                               encoding="utf-8", newline="\n")
    return report


def main() -> None:
    parser = argparse.ArgumentParser(description="Synchronize and exactly deduplicate RootAd upstream rules")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--date", type=dt.date.fromisoformat, default=dt.date.today())
    parser.add_argument("--sequence", type=int)
    args = parser.parse_args()
    report = sync(args.root.resolve(), args.date, args.sequence)
    print(json.dumps(report, ensure_ascii=False))


if __name__ == "__main__":
    main()
