# Security policy

Rule and executable updates are separate trust channels. A rule ZIP may contain only `manifest.json`, `strict.domains`, `balanced.domains`, and `reward.domains`. The manager verifies the GitHub asset digest when provided, every profile SHA-256 and count, exact-domain syntax, archive size, and entry set. The root core validates the files again before atomic activation and keeps one rollback version.

Wei.G RootAd does not delete other apps' data, modify other modules, flush global firewall tables, or execute code from a rule update. Runtime writes are limited to its module directory, `/data/adb/weig_rootad`, and a fixed temporary staging directory.

Keep the Android signing keystore outside the public repository. Use a protected GitHub `release` environment with manual approval. Never put GitHub personal access tokens in the APK.

Public rule reports must not contain cookies, authorization headers, account identifiers, request bodies, full private URLs, device serial numbers, or unrelated captures.
