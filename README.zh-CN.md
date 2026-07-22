# Wei.G RootAd

[English](README.md)

Wei.G RootAd 是面向 Android 12–16 的轻量开源 Root 去广告工具。原生管理器通过同一套 systemless hosts 核心兼容 Magisk、KernelSU 和 APatch。

## 主要功能

- Wei.G 基础规则精确去重并强制排除 74 条奖励广告端点后，基础模式为 17,041 条。
- 奖励广告默认不拦截，可单独勾选腾讯/QQ、微信、短视频和其他奖励广告包，并支持临时放行十分钟。
- 首页显示实际运行规则数，可开关保护、切换模式、管理自定义规则。
- 支持自定义拦截、放行和手动关闭单条规则，规则更新不会覆盖用户内容。
- 规则、管理器 APK、Root 核心分别从 GitHub 更新，并支持规则回滚。
- 每周同步上游，执行精确去重、格式检查、异常变化限制和可重复构建。
- 自动跟随系统深浅色，黑白底配蓝色主色。
- 一键打开预填环境信息的 GitHub Issue。
- 完整卸载会清除 hosts 挂载、模块、官方规则、自定义规则和全部状态。

规则更新包只允许纯数据；脚本、APK、SO、符号链接、路径穿越、未知文件和异常大小都会被拒绝。

## 安装与使用

**普通用户：**只需下载 `WeiG-RootAd-v0.1.0.zip`，使用 MMRL、Magisk、KernelSU 或 APatch 安装并重启。这个推荐安装包会同时安装 Root 核心和管理器。打开管理器、授予一次 Root 权限，然后选择严格或平衡模式。

首次重启前，管理器会显示“核心已安装，等待重启”。规则修改和规则更新立即生效；只有首次安装核心或更新核心需要重启。

不需要管理器的高级用户可以安装 `WeiG-RootAd-v0.1.0-core-only.zip`。同时保留独立的 `WeiG-RootAd-Manager-v0.1.0.apk`，用于手动安装或只更新管理器。两个 ZIP 使用相同的模块 ID 和核心，请勿当作两个模块同时安装。

以后进入“更新 → 一键更新规则”即可，不需要重新安装 APK。如果部分系统限制自动安装 APK，核心仍会正常安装，此时再单独安装 Release 中的管理器 APK 即可。

每次推送到 `main` 都会自动刷新 GitHub 上的 `test-latest` 预发布版本。固定名称的 `*-test` 文件仅用于测试，正式版本继续使用独立版本号且不覆盖。

不需要打开 Actions 页面，直接执行：

```bash
git add .
git commit -m "Update Wei.G RootAd"
git push origin main                 # 自动刷新滚动测试版
git tag v0.1.0
git push origin v0.1.0               # 自动发布正式 v0.1.0
```

管理器首版为 `0.1.0`。后续显示版本按要求采用 `年.日.月`，例如 `26.21.07`；Android 内部 `versionCode` 始终递增。规则独立采用 `YYYYMMDDNN`。

## 构建

```bash
python -m rule_tools.build_rules
python -m unittest discover -s tests -v
python -m rule_tools.build_rule_release
gradle :app:assembleDebug
python -m rule_tools.build_module --output dist/WeiG-RootAd-v0.1.0-core-only.zip
python -m rule_tools.build_module --output dist/WeiG-RootAd-v0.1.0.zip \
  --manager-apk app/build/outputs/apk/debug/app-debug.apk
```

Android 工程使用 JDK 17、Gradle 9.6、AGP 9.2、`compileSdk 36`、`targetSdk 36`、`minSdk 31`，不引入 AndroidX UI 或统计 SDK。

公开前请确认 `app/build.gradle` 中的 `GITHUB_OWNER`，并按[规则维护说明](docs/RULE_MAINTENANCE.md)配置签名密钥。如果 GitHub 的 `release` Environment 设置了 Required reviewers，发布仍会等待人工批准；想全自动就不要开启该选项。

本地生成的一体包内含调试签名管理器，仅用于测试；安装首个正式签名版前需要先卸载调试版。受保护的 Release 工作流会自动验证并嵌入正式签名 APK。仓库发布对应资源后，应用内 GitHub 更新按钮才会生效。

## 限制

hosts 无法处理广告与正文共用同一域名、硬编码 IP，或绕过系统 DNS 的应用内置加密 DNS。为保持轻量、稳定、可回滚并兼容不同 Root 方案，核心不会修改应用私有数据或禁用应用组件。

项目采用 GPL-3.0；Wei.G 原创规则与元数据维护在 `rules/vendor/wei.G/`。

## 引用

[StevenBlack](https://github.com/StevenBlack/hosts) · [大圣净化](https://github.com/jdlingyu/ad-wars) · [AD-hosts](https://github.com/E7KMbb/AD-hosts) · [yhosts](https://github.com/VeleSila/yhosts) · [OISD](https://oisd.nl/howto) · [1024 Hosts](https://github.com/Goooler/1024_hosts)
