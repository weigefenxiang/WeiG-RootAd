# Android manager UI contract / 管理器界面约定

The native manager targets Android 12–16, follows system light/dark mode, and uses a restrained black/white and blue palette. It has no WebView, analytics, ads, VPN, accessibility service, or LSPosed dependency.

## Status and actions

The top card shows the live `running_rules` value returned by `rulectl status`. Every action immediately shows a local progress message and disables conflicting controls; the command response then updates the UI without making a second Root status request.

Before the first reboot, or after a core update, the card displays **Core installed; reboot required** and offers **Reboot now**. Rule edits, profile changes, reward-pack changes, rule-only updates, and manager APK updates do not require a reboot.

System-bar and display-cutout insets are applied to the scrollable content so the Wei.G title is not hidden by the clock, camera cutout, or gesture area.

## Profiles and reward packs

Strict and Balanced are base profiles. Both forcibly exclude all 74 known reward-ad domains. In the current Wei.G snapshot they both contain 17,041 rules because every entry in the compatibility allowlist is already absent from the base set; the two-profile structure remains so future compatibility exceptions can make Balanced smaller without changing the command or update format.

Reward blocking is a separate, opt-in section:

- Tencent / QQ: 38
- WeChat: 8
- Short video: 15
- Other: 13

Checking a pack adds only that pack to the active hosts list. **Allow selected reward ads for 10 minutes** temporarily removes all selected reward packs and shows a live countdown; ending the allowance or reaching zero restores them automatically.

## Custom rules and issue flow

Users can add an exact domain to block/allow lists or disable/restore an existing rule. These files live outside downloaded rule releases and survive rule-only updates.

**Create GitHub issue** opens a prefilled browser page with app, Android, device, Root implementation, rule version, profile, and running count. It never includes cookies, request bodies, account tokens, or raw HTTPS traffic. Hosts filtering cannot discover URL paths or traffic hidden behind hard-coded IPs or embedded encrypted DNS.

## 中文摘要

管理器顶部显示真实运行规则数；所有按钮点击后立即出现“处理中”反馈。严格和平衡都不含奖励广告，四个奖励包默认关闭，可分别勾选并临时放行十分钟。首次安装或核心更新后需要重启，规则、规则包和 APK 更新不需要。界面自动避开状态栏、刘海和底部手势区。
