# Wei.G RootAd

[简体中文](README.zh-CN.md)

Wei.G RootAd is a lightweight, open-source root ad blocker for Android 12–16. A small native Android manager controls one systemless-hosts core shared by Magisk, KernelSU, and APatch.

## Features

- 17,041 base rules from the Wei.G rule set after 74 known reward-ad endpoints are forcibly excluded and exactly deduplicated.
- Optional Tencent/QQ, WeChat, short-video, and other reward-ad blocking packs, disabled by default, with a ten-minute temporary allowance.
- Live running-rule count, protection switch, profiles, custom allow/block rules, and individual rule disable.
- Independent GitHub updates for rules, manager APK, and root core.
- Weekly upstream sync with exact deduplication, validation, change limits, deterministic output, and rollback.
- System light/dark theme with a compact black/white and blue interface.
- Prefilled GitHub Issue report with safe device and rule diagnostics.
- Complete removal of the hosts mount, core, official rules, custom rules, and state.

Rule-only releases are data-only: scripts, APKs, native binaries, links, traversal paths, unknown files, and oversized archives are rejected.

## Install and use

**Most users:** download `WeiG-RootAd-v0.1.0.zip`, install it with MMRL, Magisk, KernelSU, or APatch, and reboot. This recommended package installs both the Root core and manager app. Open the manager, grant Root once, and choose Strict or Balanced.

The manager shows **Core installed; reboot required** before the first reboot. Rule changes and rule-only updates apply immediately; only core installation or a core update requires a reboot.

Advanced users who do not want the manager can install `WeiG-RootAd-v0.1.0-core-only.zip`. The standalone `WeiG-RootAd-Manager-v0.1.0.apk` remains available for manual installation and app-only updates. Both ZIP variants use the same module ID and core; do not install them as separate modules.

Use **Updates → Update rules** for weekly rule releases; the APK does not need to change. If automatic APK installation fails on a restrictive ROM, the core remains installed and the standalone APK can be installed normally.

Every push to `main` automatically refreshes the `test-latest` GitHub pre-release. Its fixed `*-test` assets are testing builds; stable releases remain versioned and immutable.

Publish without opening the Actions page:

```bash
git add .
git commit -m "Update Wei.G RootAd"
git push origin main                 # refreshes the rolling test release
git tag v0.1.0
git push origin v0.1.0               # creates the stable v0.1.0 release
```

The initial manager version is `0.1.0`. Later manager versions use the requested `YY.DD.MM` display format while Android `versionCode` always increases. Rule versions use `YYYYMMDDNN` independently.

## Build

```bash
python -m rule_tools.build_rules
python -m unittest discover -s tests -v
python -m rule_tools.build_rule_release
gradle :app:assembleDebug
python -m rule_tools.build_module --output dist/WeiG-RootAd-v0.1.0-core-only.zip
python -m rule_tools.build_module --output dist/WeiG-RootAd-v0.1.0.zip \
  --manager-apk app/build/outputs/apk/debug/app-debug.apk
```

Android builds use JDK 17, Gradle 9.6, AGP 9.2, `compileSdk 36`, `targetSdk 36`, and `minSdk 31`. The manager uses only Android platform APIs—no AndroidX UI or analytics SDK.

Before publishing, confirm `GITHUB_OWNER` in `app/build.gradle` and configure the four signing secrets documented in [Rule maintenance](docs/RULE_MAINTENANCE.md). If the `release` environment requires reviewers, GitHub will still wait for approval; leave required reviewers disabled for fully automatic publishing.

Local all-in-one builds contain the debug-signed manager and are for testing only. Uninstall the debug app before installing the first official release-signed APK. The protected Release workflow verifies and embeds the signed release APK automatically. GitHub update buttons become live after the configured repository publishes the matching assets.

## Limits

Hosts filtering cannot handle ads that share a domain with first-party content, use hard-coded IP addresses, or bypass system DNS through an embedded encrypted-DNS client. To remain lightweight, stable, reversible, and compatible across Root implementations, the core does not modify private app data or disable app components.

GPL-3.0. Original Wei.G rules and metadata are maintained in `rules/vendor/wei.G/`.

## Sources / References

[StevenBlack](https://github.com/StevenBlack/hosts) · [大圣净化](https://github.com/jdlingyu/ad-wars) · [AD-hosts](https://github.com/E7KMbb/AD-hosts) · [yhosts](https://github.com/VeleSila/yhosts) · [OISD](https://oisd.nl/howto) · [1024 Hosts](https://github.com/Goooler/1024_hosts)
