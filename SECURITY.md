# Security policy

Rule and executable updates use separate repositories and trust paths. A schema-3 rule ZIP may contain only the six declared profiles and hosts exports, the reward set and four packs, `manifest.json`, `packs.json`, and `health-summary.json`. The manager verifies the release asset digest when GitHub provides one, exact file set, SHA-256 values, counts, syntax, nesting, regional separation, reward separation, archive size, and pack union. The Root core validates runtime files again before atomic activation and keeps one rollback version.

Wei.G ZeroAd does not delete other apps' data, modify other modules, flush global firewall tables, or execute code from rule updates. Runtime writes are limited to its module directory, `/data/adb/weig_rootad`, and the fixed ZeroAd staging directory.

Keep the Android signing keystore outside the public repository. Never put GitHub personal access tokens in the APK.

Public issue reports must not contain cookies, authorization headers, account identifiers, request bodies, full private URLs, device serial numbers, or unrelated captures.
