# Rule maintenance / 规则维护

Live rules are built in the separate `WeiG-ZeroAd-Rules` repository. The app repository keeps only a Wei.G offline fallback so a newly flashed core can start before its first rule update.

在线规则由独立的 `WeiG-ZeroAd-Rules` 仓库构建。应用主仓库只保留 Wei.G 离线兜底，确保刚刷入核心、尚未更新规则时也能启动。

## Release contract

The rule repository publishes `WeiG-ZeroAd-Rules.zip` with schema 3. It contains six exact-domain profiles, their hosts exports, the independent reward set and four reward packs, `manifest.json`, `packs.json`, and `health-summary.json`. No executable content is accepted.

Publish at least one rules Release before pushing/tagging the manager repository. The manager Release workflow fails closed if no verified rules asset is available, rather than shipping three misleading fallback choices.

The manager repository is configured in `app/build.gradle`:

```groovy
GITHUB_OWNER = "weigefenxiang"
CODE_REPOSITORY = "WeiG-ZeroAd"
RULES_REPOSITORY = "WeiG-ZeroAd-Rules"
```

## Manager signing

Create a GitHub environment named `release` and add:

- `ROOTAD_KEYSTORE_BASE64`
- `ROOTAD_KEYSTORE_PASSWORD`
- `ROOTAD_KEY_ALIAS`
- `ROOTAD_KEY_PASSWORD`

Do not enable required reviewers if every push/tag should publish automatically. Keep the original keystore offline; changing it prevents Android from installing later APK updates over the existing app.

## Versions

- Initial manager/core: `0.1.0`, Android `versionCode=1`.
- Later manager display versions may use `YY.DD.MM`; always increase `versionCode`.
- Rule versions are independent numeric `YYYYMMDDNN` values.

## Weekly rule job

The rules repository runs every Sunday at 03:00 Asia/Singapore. Four upstream downloads and sixteen DNS shards run in parallel. A domain is removed from Lean only after three consecutive weekly NXDOMAIN observations; timeouts and SERVFAIL are retained as unknown. Generated changes above the configured safety limit stop publication for review.
