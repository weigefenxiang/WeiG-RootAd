# Rule maintenance / 规则维护

## Weekly automation

`.github/workflows/rules-sync.yml` runs every Sunday at 03:00 Asia/Singapore and can also be started manually. Weekly execution already satisfies the monthly minimum update requirement.

The job reads enabled local or remote entries from `rules/sources.json`, converts supported hosts/adblock/exact-domain formats, lowercases ASCII domains, rejects IP literals and invalid syntax, removes exact duplicates, forcibly excludes the reward set from both base profiles, runs tests, commits the auditable snapshot, and publishes a data-only release.

Hosts entries are exact. `example.com` does **not** block `ads.example.com`, so the synchronizer deliberately does not remove a subdomain merely because its parent is present.

The job refuses a source result below 1,000 domains or a one-run size change above 35%. Failed validation produces no release, so installed devices keep the previous rules.

每周日新加坡时间 03:00 自动同步，也能在 Actions 中手动运行。流程会读取本地或远程来源、统一格式、精确去重、排除无效 IP/域名、从严格和平衡中强制移除奖励集合、构建规则与可选包、测试并发布纯数据 ZIP。单次变化超过 35% 或结果少于 1,000 条时拒绝发布。

## Versions

- Initial manager/core: `0.1.0`, Android `versionCode=1`.
- Later display version: `YY.DD.MM`, for example `26.21.07`; always increment `versionCode`.
- Rule release: `rules-YYYYMMDDNN`; its manifest stores numeric `YYYYMMDDNN`.

## GitHub setup

Change these values in `app/build.gradle` before the first public build:

```groovy
GITHUB_OWNER = "your-account"
CODE_REPOSITORY = "WeiG-RootAd"
RULES_REPOSITORY = "WeiG-RootAd"
```

The first release keeps code and rules in one repository; the manager searches recent Releases for the correct asset type, so weekly rule releases do not hide APK/core releases. If you later split the rules into `WeiG-RootAd-Rules`, move `rules/`, `rule_tools/`, the rule tests, and `rules-sync.yml`, then change only `RULES_REPOSITORY`; keep the release contract unchanged.

Create a protected GitHub environment named `release` and add:

- `ROOTAD_KEYSTORE_BASE64`
- `ROOTAD_KEYSTORE_PASSWORD`
- `ROOTAD_KEY_ALIAS`
- `ROOTAD_KEY_PASSWORD`

Do not enable required reviewers on that environment if every push/tag must publish without an approval click.

Keep the original keystore offline. Losing it prevents future APK updates; changing it will be rejected by Android and by the manager's certificate check.

## Adding a source

Add a stable source ID, `type` (`local` or `remote`), path/URL, and compatible license to `rules/sources.json`, then run:

```bash
python -m rule_tools.sync_upstreams --date 2026-07-21
python -m rule_tools.build_rules
python -m unittest discover -s tests -v
python -m rule_tools.build_rule_release
```

Review the JSON report under `rules/reports/`, profile-count differences, additions/removals, and reward compatibility before merging.
