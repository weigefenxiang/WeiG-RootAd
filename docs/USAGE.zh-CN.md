# 命令行用法

管理器调用同一套 Root 控制接口，也可手动执行：

```bash
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl status
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl cn-profile lean
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl cn-profile balanced
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl cn-profile strict
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl global-profile off
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl global-profile lean
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl reward 10
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl reward-stop
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl pack-disable reward.tencent
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl pack-enable reward.tencent
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl protection-off
su -mm -c /data/adb/modules/weig_rootad/bin/rulectl protection-on
```

自定义精确域名：

```bash
rulectl block-add ads.example.com
rulectl allow-add safe.example.com
rulectl domain-disable sdk.example.com
rulectl domain-enable sdk.example.com
```

用户规则保存在 `/data/adb/weig_rootad/user/`，更新核心或官方规则不会覆盖。完整卸载会一并删除用户规则、官方规则、模块和全部状态。

默认使用境内精简、境外关闭，并启用全部四个奖励广告包。奖励广告域名不进入六个普通档位；`reward 10` 只会临时放行当前勾选的奖励广告包。
